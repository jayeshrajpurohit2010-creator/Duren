package com.duren.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.feature.nav.DurenNavHost
import com.duren.app.feature.theme.ThemeViewModel
import com.duren.app.ui.components.CampfireRevealSplash
import com.duren.app.ui.theme.DurenTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val theme by themeViewModel.themeState.collectAsStateWithLifecycle()
            DurenTheme(darkTheme = theme.darkTheme, accent = theme.accent) {
                // Signature Campfire Reveal plays once per cold start, then fades to
                // the app. rememberSaveable keeps a rotation from replaying it.
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    DurenNavHost()
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(animationSpec = tween(600))
                    ) {
                        CampfireRevealSplash(onEnter = { showSplash = false })
                    }
                }
            }
        }
    }
}
