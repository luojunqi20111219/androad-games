package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.CodexScreen
import com.example.ui.screens.CombatScreen
import com.example.ui.screens.DebriefScreen
import com.example.ui.screens.MissionSelectScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBg)
                ) { innerPadding ->
                    AstralVanguardApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AstralVanguardApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        AppScreen.MISSION_SELECT -> {
            MissionSelectScreen(viewModel = viewModel, modifier = modifier)
        }
        AppScreen.COMBAT -> {
            CombatScreen(viewModel = viewModel, modifier = modifier)
        }
        AppScreen.ARMORY -> {
            ArmoryScreen(viewModel = viewModel, modifier = modifier)
        }
        AppScreen.CODEX -> {
            CodexScreen(viewModel = viewModel, modifier = modifier)
        }
        AppScreen.DEBRIEF -> {
            DebriefScreen(viewModel = viewModel, modifier = modifier)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = "星界战锋：$name",
        modifier = modifier
    )
}
