package com.yourname.pdftoolkit.ui.pdfviewer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yourname.pdftoolkit.ui.pdfviewer.engine.PdfEngineFactory
import com.yourname.pdftoolkit.ui.pdfviewer.engine.PdfViewerEngine

/**
 * Simplified PDF Viewer Screen using pluggable engine architecture.
 * No custom gesture handling - delegates everything to the engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUri: Uri?,
    pdfName: String = "PDF Document",
    onNavigateBack: () -> Unit,
    onNavigateToTool: ((String, Uri?, String?) -> Unit)? = null
) {
    val context = LocalContext.current

    if (pdfUri == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No PDF selected", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // Create engine with automatic fallback
    val engine = remember(pdfUri) {
        PdfEngineFactory.createWithFallback(context, pdfUri)
    }

    // Track current page for UI
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    // Cleanup on dispose
    DisposableEffect(engine) {
        onDispose {
            engine.cleanup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (pageCount > 0) {
                            "$pdfName (${currentPage + 1}/$pageCount)"
                        } else {
                            pdfName
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Search button
                    IconButton(onClick = { /* Search handled by engine */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            onNavigateToTool?.invoke("share", pdfUri, null)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    // More options menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    PdfViewerDropdownMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        pdfUri = pdfUri,
                        onNavigateToTool = onNavigateToTool
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Engine handles all rendering, zoom, pan, text selection
            engine.render()
        }
    }
}

@Composable
private fun PdfViewerDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    pdfUri: Uri,
    onNavigateToTool: ((String, Uri?, String?) -> Unit)?
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Compress") },
            onClick = {
                onDismiss()
                onNavigateToTool?.invoke("compress", pdfUri, null)
            }
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Convert to Images") },
            onClick = {
                onDismiss()
                onNavigateToTool?.invoke("convert", pdfUri, null)
            }
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Split PDF") },
            onClick = {
                onDismiss()
                onNavigateToTool?.invoke("split", pdfUri, null)
            }
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Print") },
            onClick = {
                onDismiss()
                onNavigateToTool?.invoke("print", pdfUri, null)
            }
        )
    }
}

/**
 * Loading indicator shown while engine initializes.
 */
@Composable
private fun PdfLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
