package com.yourname.pdftoolkit.ui.pdfviewer.engine

/**
 * Callback interface for PDF engine events.
 *
 * This interface replaces lambda parameters for better reflection compatibility
 * when instantiating AndroidXPdfViewerEngine via reflection in PdfEngineFactory.
 *
 * Kotlin lambdas have complex synthetic class structures that can cause
 * NoSuchMethodException during reflection. A simple interface with explicit
 * method signatures is more reliable for Class.getConstructor() lookups.
 */
interface PdfEngineCallbacks {
    /**
     * Called when the engine encounters an error.
     * @param error The throwable that caused the error
     */
    fun onError(error: Throwable)

    /**
     * Called when the engine requires fallback to a different engine.
     * This typically happens when the native AndroidX PDF viewer is not
     * actually supported despite API level checks passing.
     */
    fun onFallbackRequired()

    /**
     * Called when the current page changes.
     * @param current The current page number (0-indexed)
     * @param total The total number of pages in the document
     */
    fun onPageChanged(current: Int, total: Int)
}
