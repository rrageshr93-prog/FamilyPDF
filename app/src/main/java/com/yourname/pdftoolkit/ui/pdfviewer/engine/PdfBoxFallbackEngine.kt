package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.yourname.pdftoolkit.ui.screens.PdfViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Legacy PDFBox-based fallback engine.
 * Wraps existing rendering logic without modification.
 */
class PdfBoxFallbackEngine(
    private val context: Context,
    private val uri: Uri
) : PdfViewerEngine {

    private var document: PDDocument? = null
    private var renderer: PDFRenderer? = null
    private var tempFile: File? = null
    private val documentMutex = Mutex()

    private var pageCount: Int = 0
    private var currentPageIndex: Int = 0

    // Simple LRU cache for rendered pages
    private val pageCache = mutableMapOf<Int, Bitmap>()
    private val maxCacheSize = 3

    override fun load(uri: Uri) {
        // Loading happens in render() with proper coroutine handling
    }

    @Composable
    override fun render() {
        val currentContext = LocalContext.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val isLoading = remember { mutableStateOf(true) }
        val errorMessage = remember { mutableStateOf<String?>(null) }
        val renderedPages = remember { mutableStateListOf<Bitmap?>() }

        DisposableEffect(Unit) {
            scope.launch(Dispatchers.IO) {
                try {
                    loadDocumentInternal(currentContext, uri)
                    pageCount = document?.numberOfPages ?: 0
                    withContext(Dispatchers.Main) {
                        isLoading.value = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage.value = "Failed to load PDF: ${e.message}"
                        isLoading.value = false
                    }
                }
            }
            onDispose {
                cleanup()
            }
        }

        // Track current page
        LaunchedEffect(listState.firstVisibleItemIndex) {
            currentPageIndex = listState.firstVisibleItemIndex
        }

        when {
            isLoading.value -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage.value != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = errorMessage.value ?: "Unknown error",
                        color = Color.Red
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    items(pageCount) { index ->
                        PageItem(
                            pageIndex = index,
                            onRender = { loadPage(index) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PageItem(
        pageIndex: Int,
        onRender: suspend () -> Bitmap?
    ) {
        val scope = rememberCoroutineScope()
        val bitmap = remember(pageIndex) { mutableStateOf<Bitmap?>(pageCache[pageIndex]) }

        LaunchedEffect(pageIndex) {
            if (bitmap.value == null || bitmap.value?.isRecycled == true) {
                bitmap.value = onRender()
            }
        }

        val currentBitmap = bitmap.value
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    private suspend fun loadPage(pageIndex: Int): Bitmap? {
        // Check cache first
        pageCache[pageIndex]?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        return documentMutex.withLock {
            try {
                val pdfRenderer = renderer ?: return@withLock null

                // Render at reduced quality to save memory
                val scale = 1.5f
                val rendered = pdfRenderer.renderImage(pageIndex, scale)

                // Manage cache size
                if (pageCache.size >= maxCacheSize) {
                    pageCache.keys.minOrNull()?.let { oldest ->
                        pageCache.remove(oldest)?.recycle()
                    }
                }

                pageCache[pageIndex] = rendered
                rendered
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun loadDocumentInternal(context: Context, uri: Uri) {
        documentMutex.withLock {
            cleanupInternal()

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input file")

            // Copy to temp file for PDFBox
            val temp = File.createTempFile("pdf_fallback_", ".pdf", context.cacheDir)
            inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile = temp

            document = PDDocument.load(temp)
            renderer = PDFRenderer(document)
        }
    }

    private fun cleanupInternal() {
        pageCache.values.forEach { it.recycle() }
        pageCache.clear()
        renderer = null
        document?.close()
        document = null
        tempFile?.delete()
        tempFile = null
    }

    override fun search(query: String): List<SearchResult> {
        // PDFBox fallback does not support search
        return emptyList()
    }

    override fun navigateToPage(pageIndex: Int) {
        currentPageIndex = pageIndex.coerceIn(0, pageCount - 1)
    }

    override fun getCurrentPage(): Int = currentPageIndex

    override fun getPageCount(): Int = pageCount

    override fun cleanup() {
        cleanupInternal()
    }

    override fun isAvailable(): Boolean {
        return try {
            Class.forName("com.tom_roush.pdfbox.pdmodel.PDDocument")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
