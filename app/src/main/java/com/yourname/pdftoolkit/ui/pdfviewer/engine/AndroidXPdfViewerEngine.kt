package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable

/**
 * AndroidX PDF Viewer implementation using androidx.pdf:pdf-viewer-fragment.
 *
 * Note: Currently uses PdfBoxFallbackEngine as the AndroidX PDF API is in alpha
 * and the API is unstable. This stub ensures the architecture is ready for
 * migration once the AndroidX PDF library reaches stable.
 */
class AndroidXPdfViewerEngine(
    private val context: Context,
    private val uri: Uri
) : PdfViewerEngine {

    // Delegate to PdfBox for now until AndroidX PDF API stabilizes
    private val fallbackEngine = PdfBoxFallbackEngine(context, uri)

    override fun load(uri: Uri) {
        fallbackEngine.load(uri)
    }

    @Composable
    override fun render() {
        fallbackEngine.render()
    }

    override fun search(query: String): List<SearchResult> {
        return fallbackEngine.search(query)
    }

    override fun navigateToPage(pageIndex: Int) {
        fallbackEngine.navigateToPage(pageIndex)
    }

    override fun getCurrentPage(): Int = fallbackEngine.getCurrentPage()

    override fun getPageCount(): Int = fallbackEngine.getPageCount()

    override fun cleanup() {
        fallbackEngine.cleanup()
    }

    override fun isAvailable(): Boolean {
        // Check if AndroidX PDF is available (currently always false - using fallback)
        return try {
            Class.forName("androidx.pdf.viewer.PdfViewerFragment")
            false // Still use fallback until API stabilizes
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
