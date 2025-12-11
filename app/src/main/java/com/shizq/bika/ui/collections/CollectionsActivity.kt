package com.shizq.bika.ui.collections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.ui.ComicCard
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CollectionsScreen()
        }
    }

    @Composable
    fun CollectionsScreen(viewModel: CollectionsViewModel = hiltViewModel()) {
        val collectionState by viewModel.uiState.collectAsStateWithLifecycle()
        CollectionsContent(collectionState)
    }

    @Composable
    fun CollectionsContent(collectionState: CollectionUiState) {
        Scaffold { innerPadding ->
            when (collectionState) {
                is CollectionUiState.Error -> {}
                CollectionUiState.Loading -> {}
                is CollectionUiState.Success ->
                    LazyColumn(modifier = Modifier.padding(innerPadding)) {
                        items(collectionState.comics) { comic ->
                            ComicCard(comic) {
                                ComicInfoActivity.start(this@CollectionsActivity, comic.id)
                            }
                        }
                    }
            }
        }
    }
}