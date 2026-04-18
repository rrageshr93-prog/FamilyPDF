package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.content.Context
import android.net.Uri
import com.yourname.pdftoolkit.BuildConfig

/**
 * Factory for creating the appropriate PDF viewer engine based on build flavor.
 */
object PdfEngineFactory {

    /**
     * Create a PDF viewer engine based on build configuration.
     * Falls back to PdfBox if primary engine fails to initialize.
     */
    fun create(context: Context, uri: Uri): PdfViewerEngine {
        return when (BuildConfig.FLAVOR) {
            "fdroid" -> createMuPdfEngine(context, uri)
            else -> createAndroidXEngine(context, uri)
        }
    }

    /**
     * Create with automatic fallback to PdfBox on failure.
     */
    fun createWithFallback(context: Context, uri: Uri): PdfViewerEngine {
        val primaryEngine = create(context, uri)

        return if (primaryEngine.isAvailable()) {
            primaryEngine
        } else {
            PdfBoxFallbackEngine(context, uri)
        }
    }

    private fun createAndroidXEngine(context: Context, uri: Uri): PdfViewerEngine {
        return try {
            AndroidXPdfViewerEngine(context, uri)
        } catch (e: Exception) {
            PdfBoxFallbackEngine(context, uri)
        }
    }

    private fun createMuPdfEngine(context: Context, uri: Uri): PdfViewerEngine {
        return try {
            // Try MuPDF first for F-Droid
            MuPdfViewerEngine(context, uri)
        } catch (e: Exception) {
            // Fall back to PdfBox if MuPDF fails
            PdfBoxFallbackEngine(context, uri)
        }
    }
}
