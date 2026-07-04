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
import com.izplay.tv.ui.screens.SetupScreen
import com.izplay.tv.ui.theme.IZPlayTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IZPlayTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                if (state.configured) {
                    HomeScreen(vm)
                } else {
                    SetupScreen(onLogin = vm::loginXtream, onM3u = vm::loginM3u)
                }
            }
        }
    }
}
