package com.wayfind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wayfind.app.ui.screens.MapNavigationScreen
import com.wayfind.app.ui.screens.ProfileSelectionScreen
import com.wayfind.app.ui.screens.WelcomeScreen
import com.wayfind.app.ui.theme.Slate900
import com.wayfind.app.ui.theme.WayfindTheme
import com.wayfind.app.ui.viewmodel.AppScreen
import com.wayfind.app.ui.viewmodel.WayfindViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WayfindViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WayfindTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate900
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val selectedProfile by viewModel.selectedProfile.collectAsState()

                    when (currentScreen) {
                        AppScreen.WELCOME -> {
                            WelcomeScreen(
                                onStartNavigation = {
                                    viewModel.navigateTo(AppScreen.PROFILE_SELECTION)
                                }
                            )
                        }
                        AppScreen.PROFILE_SELECTION -> {
                            ProfileSelectionScreen(
                                selectedProfile = selectedProfile,
                                onSelectProfile = { profile ->
                                    viewModel.selectProfile(profile)
                                },
                                onConfirm = {
                                    viewModel.navigateTo(AppScreen.MAP_NAVIGATION)
                                }
                            )
                        }
                        AppScreen.MAP_NAVIGATION -> {
                            MapNavigationScreen(
                                viewModel = viewModel,
                                onBackToProfile = {
                                    viewModel.navigateTo(AppScreen.PROFILE_SELECTION)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
