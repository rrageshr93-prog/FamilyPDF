package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.pdf.viewer.PdfViewer
import com.yourname.pdftoolkit.ui.pdfviewer.PdfViewerCapability

/**
 * AndroidX PDF Viewer implementation using androidx.pdf:pdf-viewer.
 *
 * This engine wraps PdfViewer (a custom View) and provides native PDF viewing capabilities
 * on API 31+ devices with SDK Extension 13+.
 *
 * Features provided natively by PdfViewer:
 * - Smooth zoom (pinch + double tap)
 * - Text selection (cross-page)
 * - Search/find in document
 * - Password-protected PDF handling (auto dialog)
 * - Link navigation
 * - Hardware-accelerated rendering
 * - Automatic memory management
 *
 * @param context Application context
 * @param callbacks Callback interface for engine events (better reflection compatibility than lambdas)
 */
@RequiresApi(31)
class AndroidXPdfViewerEngine(
    private val context: Context,
    private val callbacks: PdfEngineCallbacks? = null
) : PdfViewerEngine {

    private var pdfView: PdfViewer? = null
    private var documentUri: Uri? = null

    override fun load(uri: Uri) {
        documentUri = uri
        // View will be created in render() via AndroidView
    }

    @Composable
    override fun render() {
        AndroidView(
            factory = { ctx ->
                // Create PdfViewer view directly
                createPdfView(ctx)
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Update view if needed (e.g., URI changed)
                documentUri?.let { uri ->
                    val pdf = pdfView
                    if (pdf != null && pdf.documentUri != uri) {
                        try {
                            pdf.documentUri = uri
                        } catch (e: Throwable) {
                            Log.e("AndroidXEngine", "Error setting document URI", e)
                            callbacks?.onError(e)
                        }
                    }
                }
            }
        )
    }

    /**
     * Create and configure the PdfViewer view.
     */
    private fun createPdfView(context: Context): PdfViewer {
        return try {
            val pdfViewer = PdfViewer(context).also { pdfView = it }

            // Set document URI if available
            documentUri?.let { uri ->
                try {
                    pdfViewer.documentUri = uri
                } catch (e: OutOfMemoryError) {
                    Log.e("AndroidXEngine", "OOM loading PDF document", e)
                    callbacks?.onError(e)
                    callbacks?.onFallbackRequired()
                } catch (e: UnsupportedOperationException) {
                    Log.e("AndroidXEngine", "Unsupported PDF operation", e)
                    callbacks?.onError(e)
                    callbacks?.onFallbackRequired()
                } catch (e: Throwable) {
                    Log.e("AndroidXEngine", "Error setting document URI", e)
                    callbacks?.onError(e)
                    callbacks?.onFallbackRequired()
                }
            }

            // Notify that page data is available (initial load)
            try {
                val current = pdfViewer.currentPageNumber
                val total = pdfViewer.documentPageCount
                callbacks?.onPageChanged(current, total)
            } catch (e: Exception) {
                // Ignore - polling will catch it
            }

            pdfViewer

        } catch (e: OutOfMemoryError) {
            Log.e("AndroidXEngine", "OOM creating PDF engine", e)
            callbacks?.onError(e)
            callbacks?.onFallbackRequired()
            throw e
        } catch (e: UnsupportedOperationException) {
            Log.e("AndroidXEngine", "Unsupported operation creating PDF engine", e)
            callbacks?.onError(e)
            callbacks?.onFallbackRequired()
            throw e
        } catch (e: Throwable) {
            Log.e("AndroidXEngine", "Unexpected error creating PdfViewer", e)
            callbacks?.onError(e)
            throw e
        }
    }

    override fun search(query: String): List<SearchResult> {
        return try {
            val pdf = pdfView
            if (pdf != null) {
                // Activate text search UI in PdfViewer
                pdf.isTextSearchActive = true
                // Note: PdfViewer handles search internally; we return empty
                // as the actual results are consumed by the view's UI
            }
            emptyList()
        } catch (e: Exception) {
            callbacks?.onError(e)
            emptyList()
        }
    }

    override fun navigateToPage(pageIndex: Int) {
        try {
            val pdf = pdfView
            if (pdf != null) {
                // PdfViewer API for programmatic page navigation
                // Note: This API may not be exposed in alpha04
                // pdf.goToPage(pageIndex) // Uncomment when API stable
            }
        } catch (e: Exception) {
            // API not available in current alpha
        }
    }

    override fun getCurrentPage(): Int {
        return try {
            val pdf = pdfView
            pdf?.currentPageNumber ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun getPageCount(): Int {
        return try {
            val pdf = pdfView
            pdf?.documentPageCount ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun cleanup() {
        try {
            // Just clear the reference - the view will be cleaned up by AndroidView
            pdfView = null
            documentUri = null
        } catch (e: Exception) {
            // Ignore cleanup errors - don't crash on dispose
            Log.w("AndroidXEngine", "Cleanup error (ignorable)", e)
        }
    }

    override fun isAvailable(): Boolean {
        // Check capability first
        if (!PdfViewerCapability.isNativeViewerSupported()) {
            return false
        }

        // Verify PdfViewerFragment class is available
        return try {
            Class.forName("androidx.pdf.viewer.PdfViewerFragment")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Deactivate text search if active.
     */
    fun clearSearch() {
        try {
            val pdf = pdfView
            if (pdf != null) {
                pdf.isTextSearchActive = false
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
