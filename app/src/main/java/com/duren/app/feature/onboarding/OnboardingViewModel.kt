package com.duren.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.auth.AuthRepository
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.settings.SettingsRepository
import com.duren.app.data.tribe.TribeRepository
import com.duren.app.data.tribe.model.Tribe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the first-run "Find your fire" flow. It decides whether the flow is needed
 * (a freshly signed-up soul who hasn't onboarded), serves the curated tribe catalog
 * to pick from, and joins the picks before marking the profile onboarded.
 *
 * The gate is purely reactive: once [SettingsRepository.markOnboarded] writes,
 * [needsOnboarding] flips to false on the next profile snapshot and the host swaps
 * the screen out — no navigation call needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tribeRepository: TribeRepository,
    private val settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    /**
     * Make sure the curated catalog exists — onboarding is the first place a new soul
     * meets the tribes, so we can't assume Discover seeded them yet. Idempotent, and
     * called from the screen (not init) so an already-onboarded user, whose host still
     * builds this ViewModel, doesn't pay a catalog read on every launch.
     */
    fun ensureCatalog() {
        viewModelScope.launch { tribeRepository.seedDefaultTribes() }
    }

    // null = still deciding (profile not loaded yet) → host shows the app, no flash.
    // true = run the flow; false = already onboarded.
    val needsOnboarding: StateFlow<Boolean?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else profileRepository.observeProfile(user.uid)
        }
        .map { profile -> profile?.let { !it.hasOnboarded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tribes: StateFlow<List<Tribe>> = tribeRepository.observeTribes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _finishing = MutableStateFlow(false)
    val finishing: StateFlow<Boolean> = _finishing.asStateFlow()

    fun toggle(tribeId: String) {
        _selected.value = _selected.value.toMutableSet().apply {
            if (!add(tribeId)) remove(tribeId)
        }
    }

    /** Join every picked tribe, then mark onboarding done (which dismisses the flow). */
    fun enter() {
        if (_finishing.value) return
        _finishing.value = true
        viewModelScope.launch {
            val byId = tribes.value.associateBy { it.id }
            _selected.value.forEach { id ->
                val name = byId[id]?.name ?: return@forEach
                tribeRepository.joinTribe(id, name)
            }
            // On success the gate swaps us out; on failure re-enable so they can retry
            // instead of being stuck on a dead button.
            if (settingsRepository.markOnboarded().isFailure) _finishing.value = false
        }
    }

    /** Skip the picks but still mark onboarding done, so it doesn't return. */
    fun skip() {
        if (_finishing.value) return
        _finishing.value = true
        viewModelScope.launch {
            if (settingsRepository.markOnboarded().isFailure) _finishing.value = false
        }
    }
}
