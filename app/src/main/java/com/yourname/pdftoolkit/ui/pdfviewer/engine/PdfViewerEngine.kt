package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.net.Uri
import androidx.compose.runtime.Composable

/**
 * Search result with page and bounds information.
 */
data class SearchResult(
    val pageIndex: Int,
    val bounds: List<android.graphics.RectF>,
    val snippet: String
)

/**
 * Core interface for PDF viewer engines.
 * All implementations must provide native rendering without custom gesture handling.
 */
interface PdfViewerEngine {
    /**
     * Load a PDF from the given URI.
     */
    fun load(uri: Uri)

    /**
     * Render the PDF viewer UI.
     * Container is provided by the engine implementation.
     */
    @Composable
    fun render()

    /**
     * Search for text in the PDF.
     * @return list of search results with page indices and bounds
     */
    fun search(query: String): List<SearchResult>

    /**
     * Navigate to a specific page.
     */
    fun navigateToPage(pageIndex: Int)

    /**
     * Get current page index.
     */
    fun getCurrentPage(): Int

    /**
     * Get total page count.
     */
    fun getPageCount(): Int

    /**
     * Cleanup resources when viewer is disposed.
     */
    fun cleanup()

    /**
     * Check if this engine is available/functional.
     */
    fun isAvailable(): Boolean = true
}
