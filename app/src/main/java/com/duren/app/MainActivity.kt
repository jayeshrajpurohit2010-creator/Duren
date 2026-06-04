package com.duren.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.feature.nav.DurenNavHost
import com.duren.app.feature.theme.ThemeViewModel
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
                DurenNavHost()
            }
        }
    }
}
