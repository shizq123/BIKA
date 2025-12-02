package com.shizq.bika.ui.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HistoryScreen()
        }
    }

    @Composable
    fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
        HistoryContent()
    }

    @Composable
    fun HistoryContent() {
        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {

            }
        }
    }
}

