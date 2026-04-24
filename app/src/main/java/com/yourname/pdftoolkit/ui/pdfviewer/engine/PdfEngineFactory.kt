package com.yourname.pdftoolkit.ui.pdfviewer.engine

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentManager
import com.yourname.pdftoolkit.BuildConfig
import com.yourname.pdftoolkit.ui.pdfviewer.PdfViewerCapability

/**
 * Factory for creating the appropriate PDF viewer engine based on build flavor and device capability.
 *
 * Routing logic:
 * - F-Droid/FOSS flavor: Always use MuPDF (no proprietary dependencies)
 * - Play Store flavor, API 31+ with SDK Extension 13: Use AndroidX PdfViewerFragment
 * - Play Store flavor, older devices: Use PdfBoxFallbackEngine
 *
 * CRITICAL: AndroidXPdfViewerEngine is instantiated via reflection to avoid ClassNotFoundException
 * in FOSS flavors that don't include androidx.pdf dependency.
 */
object PdfEngineFactory {

    /**
     * Create a PDF viewer engine based on build configuration.
     * Falls back to PdfBox if primary engine fails to initialize.
     *
     * Note: This is the legacy signature for simple usage. For AndroidX engine,
     * use createWithFragmentManager() instead.
     */
    fun create(context: Context, uri: Uri): PdfViewerEngine {
        return when (BuildConfig.FLAVOR) {
            "fdroid", "opensource" -> createMuPdfEngine(context, uri)
            else -> createPdfBoxEngine(context, uri)
        }
    }

    /**
     * Create with automatic fallback to PdfBox on failure.
     *
     * Legacy signature - always returns an engine that doesn't require FragmentManager.
     * For native AndroidX support with FragmentManager, use createWithFragmentManager().
     */
    fun createWithFallback(context: Context, uri: Uri): PdfViewerEngine {
        val primaryEngine = create(context, uri)

        return if (primaryEngine.isAvailable()) {
            primaryEngine
        } else {
            PdfBoxFallbackEngine(context, uri)
        }
    }

    /**
     * Create an engine with full native support when possible.
     *
     * This method returns the best available engine for the device:
     * - API 31+ with SDK Extension 13: AndroidXPdfViewerEngine (native viewer)
     * - FOSS/F-Droid flavors: MuPdfViewerEngine
     * - All others: PdfBoxFallbackEngine
     *
     * CRITICAL: This method should be called from a background thread (Dispatchers.Default)
     * because it uses reflection which can block for 100-500ms.
     *
     * @param context Application context
     * @param uri PDF document URI
     * @param callbacks Callback interface for engine events (better reflection compatibility than lambdas)
     * @return PdfViewerEngine instance, never null
     */
    fun createWithCallbacks(
        context: Context,
        uri: Uri,
        callbacks: PdfEngineCallbacks? = null
    ): PdfViewerEngine {
        // FOSS flavors always use MuPDF (no proprietary Google libraries)
        if (BuildConfig.FLAVOR == "fdroid" || BuildConfig.FLAVOR == "opensource") {
            return createMuPdfEngine(context, uri)
        }

        // Play Store flavor: try native first if supported
        return if (PdfViewerCapability.isNativeViewerSupported()) {
            try {
                createAndroidXEngineViaReflection(context, callbacks)
            } catch (e: Throwable) {
                // Catch Throwable (not just Exception) to handle OOM, LinkageError, etc.
                Log.e("PdfEngineFactory", "AndroidX engine creation failed: ${e.javaClass.simpleName}", e)
                callbacks?.onError(e)
                callbacks?.onFallbackRequired()
                createPdfBoxEngine(context, uri)
            }
        } else {
            createPdfBoxEngine(context, uri)
        }
    }

    /**
     * Check if native AndroidX PDF viewer is available on this device.
     * Convenience method wrapping PdfViewerCapability.
     */
    fun isNativeViewerAvailable(): Boolean {
        return PdfViewerCapability.isNativeViewerSupported()
    }

    /**
     * Create AndroidX engine via reflection to avoid ClassNotFoundException in FOSS flavors.
     *
     * CRITICAL: This method uses reflection to instantiate AndroidXPdfViewerEngine.
     * This prevents the JVM classloader from trying to resolve AndroidXPdfViewerEngine
     * when PdfEngineFactory is loaded in FOSS builds that don't include androidx.pdf.
     *
     * Uses PdfEngineCallbacks interface instead of lambdas for better reflection compatibility.
     *
     * @throws ClassNotFoundException if AndroidXPdfViewerEngine class is not available
     * @throws NoSuchMethodException if constructor signature doesn't match
     * @throws Throwable for any instantiation error (OOM, LinkageError, etc.)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @Suppress("UNCHECKED_CAST")
    private fun createAndroidXEngineViaReflection(
        context: Context,
        callbacks: PdfEngineCallbacks?
    ): PdfViewerEngine {
        // Load the AndroidXPdfViewerEngine class via reflection
        val engineClass = Class.forName(
            "com.yourname.pdftoolkit.ui.pdfviewer.engine.AndroidXPdfViewerEngine"
        )

        // Find the constructor: (Context, PdfEngineCallbacks?)
        // Using interface instead of lambdas for reliable reflection
        val constructor = engineClass.getConstructor(
            Context::class.java,                // context
            PdfEngineCallbacks::class.java      // callbacks interface
        )

        // Instantiate the engine
        return constructor.newInstance(
            context,
            callbacks
        ) as PdfViewerEngine
    }

    private fun createPdfBoxEngine(context: Context, uri: Uri): PdfViewerEngine {
        return PdfBoxFallbackEngine(context, uri)
    }

    private fun createMuPdfEngine(context: Context, uri: Uri): PdfViewerEngine {
        return try {
            // Try MuPDF first for F-Droid
            MuPdfViewerEngine(context, uri)
        } catch (e: Throwable) {
            // Catch Throwable (not just Exception) for OOM, etc.
            Log.e("PdfEngineFactory", "MuPDF engine failed, falling back to PdfBox: ${e.message}")
            PdfBoxFallbackEngine(context, uri)
        }
    }
}
