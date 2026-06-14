package com.duren.app.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.hearth.HearthRepository
import com.duren.app.data.mood.MoodRepository
import com.duren.app.data.mood.model.Mood
import com.duren.app.data.nest.NestRepository
import com.duren.app.data.nest.model.NestRelation
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.NudgeOutcome
import com.duren.app.data.testimonial.TestimonialRepository
import com.duren.app.data.testimonial.model.Testimonial
import com.duren.app.feature.nav.PublicProfileRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only view of another user's profile + their active embers, with the Nest button. */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    profileRepository: ProfileRepository,
    emberRepository: EmberRepository,
    moodRepository: MoodRepository,
    private val nestRepository: NestRepository,
    private val signalRepository: SignalRepository,
    private val hearthRepository: HearthRepository,
    private val testimonialRepository: TestimonialRepository
) : ViewModel() {

    val userId: String = savedStateHandle.toRoute<PublicProfileRoute>().userId

    val profile: StateFlow<Profile?> = profileRepository.observeProfile(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Their mood tonight, for the avatar aura (shown only if they opted in). */
    val theirMood: StateFlow<Mood?> = moodRepository.observeToday(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val embers: StateFlow<List<Ember>> = emberRepository.observeByAuthor(userId, 50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val relation: StateFlow<NestRelation> = nestRepository.observeRelation(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NestRelation.None)

    // One-shot toast text after a nudge; the screen shows it then clears it.
    private val _nudgeMessage = MutableStateFlow<String?>(null)
    val nudgeMessage: StateFlow<String?> = _nudgeMessage.asStateFlow()

    // Mutual Spark (F25): true while there's a live 24h spark between us and them.
    private val _mutualSpark = MutableStateFlow(false)
    val mutualSpark: StateFlow<Boolean> = _mutualSpark.asStateFlow()

    init {
        viewModelScope.launch {
            _mutualSpark.value = emberRepository.hasMutualSparkWith(userId)
        }
    }

    /** What the Nest says about them — 30d testimonials on their presence (F27). */
    val testimonials: StateFlow<List<Testimonial>> = testimonialRepository.observeFor(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Warm their hearth (F26) — a private postcard only they will read. */
    fun sendHearth(text: String) = viewModelScope.launch {
        _nudgeMessage.value = hearthRepository.send(userId, text).fold(
            onSuccess = { "You warmed their hearth 🔥" },
            onFailure = { "The hearth wouldn't catch. Try again." }
        )
    }

    /** Leave a testimonial on their presence (F27). */
    fun writeTestimonial(text: String) = viewModelScope.launch {
        _nudgeMessage.value = testimonialRepository.write(userId, text).fold(
            onSuccess = { "Your words are on their presence ✨" },
            onFailure = { "That didn't land. Try again." }
        )
    }

    fun addToNest() = viewModelScope.launch { nestRepository.sendRequest(userId) }
    fun cancelRequest() = viewModelScope.launch { nestRepository.cancelRequest(userId) }
    fun acceptRequest() = viewModelScope.launch { nestRepository.acceptRequest(userId) }
    fun declineRequest() = viewModelScope.launch { nestRepository.declineRequest(userId) }

    /** Send a silent nudge. Surfaces a one-line result the screen toasts. */
    fun nudge() = viewModelScope.launch {
        val name = profile.value?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "them"
        _nudgeMessage.value = when (signalRepository.nudge(userId)) {
            NudgeOutcome.Sent -> "You nudged $name 👀"
            NudgeOutcome.LimitReached -> "You're out of nudges for tonight."
            NudgeOutcome.Failed -> "Couldn't nudge right now."
        }
    }

    fun clearNudgeMessage() { _nudgeMessage.value = null }
}
