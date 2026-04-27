package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentManager
import com.yourname.pdftoolkit.ui.pdfviewer.PdfEngineCallbacks

/**
 * Open source flavor PDF engine provider.
 * Uses PdfBoxFallbackEngine as the default (fully open source, no proprietary dependencies).
 */
class OpensourcePdfEngineProvider : PdfEngineProvider {

    override fun createEngine(
        context: Context,
        uri: Uri,
        fragmentManager: FragmentManager?,
        containerId: Int,
        callbacks: PdfEngineCallbacks?
    ): PdfViewerEngine {
        // Always use PdfBox for opensource builds
        return PdfBoxFallbackEngine(context, uri)
    }

    override fun isNativeViewerAvailable(): Boolean {
        // Open source builds never use AndroidX PdfViewerFragment (proprietary)
        return false
    }
}
