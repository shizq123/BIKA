package com.shizq.bika.ui.comicinfo

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
class ComicInfoActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ComicInfoScreen()
        }
    }

    @Composable
    fun ComicInfoScreen(viewModel2: ComicInfoViewModel2 = hiltViewModel()) {
        ComicInfoContent()
    }

    @Composable
    fun ComicInfoContent() {
        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {

            }
        }
    }
}