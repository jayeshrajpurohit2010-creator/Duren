package com.duren.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.auth.AuthRepository
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.mood.MoodRepository
import com.duren.app.data.mood.model.Mood
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val emberRepository: EmberRepository,
    private val profileRepository: ProfileRepository,
    private val moodRepository: MoodRepository
) : ViewModel() {

    val profile: StateFlow<Profile?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else profileRepository.observeProfile(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tonight's mood, for the avatar aura (F12). */
    val myMood: StateFlow<Mood?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else moodRepository.observeToday(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Set tonight's mood (1–5). */
    fun setMood(mood: Int) = viewModelScope.launch { moodRepository.setToday(mood) }

    /** Banked Status — leave / clear an away note (F11). */
    fun setBanked(note: String) = viewModelScope.launch { profileRepository.setBanked(note) }
    fun clearBanked() = viewModelScope.launch { profileRepository.clearBanked() }

    /** Ember ids deleted this session — hidden immediately, restored if the delete fails. */
    private val deletedIds = MutableStateFlow<Set<String>>(emptySet())

    val myEmbers: StateFlow<List<Ember>> =
        combine(emberRepository.observeMine(50), deletedIds) { embers, deleted ->
            if (deleted.isEmpty()) embers else embers.filterNot { it.id in deleted }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Delete one of your own embers, with optimistic removal + rollback on failure. */
    fun deleteEmber(emberId: String) {
        deletedIds.update { it + emberId }
        viewModelScope.launch {
            emberRepository.deleteEmber(emberId).onFailure {
                deletedIds.update { ids -> ids - emberId }
            }
        }
    }

    fun signOut() = authRepository.signOut()
}
