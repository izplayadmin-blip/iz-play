package com.izplay.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.screens.HomeScreen
import com.izplay.tv.ui.screens.LoadingScreen
import com.izplay.tv.ui.screens.ProfileGateScreen
import com.izplay.tv.ui.screens.SetupScreen
import com.izplay.tv.ui.theme.IZPlayTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IZPlayTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                when {
                    state.configured && state.startupLoading -> LoadingScreen(
                        message = state.startupMessage,
                        progress = state.startupProgress,
                        onRetry = if (state.startupFailed) vm::retryStartup else null,
                        onChangeAccess = if (state.startupFailed) vm::logout else null
                    )
                    state.configured && state.profileGateVisible -> ProfileGateScreen(vm, state)
                    state.configured -> HomeScreen(vm)
                    else -> SetupScreen(onLogin = vm::loginXtream, onM3u = vm::loginM3u)
                }
            }
        }
    }
}
