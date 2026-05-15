package com.yourname.pdftoolkit

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yourname.pdftoolkit.review.ReviewIntegration
import com.yourname.pdftoolkit.util.CacheManager
import com.yourname.pdftoolkit.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * FamilyPDF Application Class
 * Made with ❤️ by RR for the family
 */
class PdfToolkitApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("FamilyPDF", "🚀 FamilyPDF started - Made with ❤️ by RR")
        
        // Initialize PdfBox-Android
        PDFBoxResourceLoader.init(applicationContext)
        
        // Initialize In-App Review
        ReviewIntegration.initialize(this)
        
        // Initialize theme
        runBlocking {
            val themeMode = ThemeManager.getThemeMode(applicationContext).first()
            ThemeManager.applyTheme(themeMode)
        }
        
        // Auto-clean cache
        applicationScope.launch {
            try {
                val cleanedBytes = CacheManager.clearOldCache(applicationContext)
                if (cleanedBytes > 0) {
                    Log.d("FamilyPDF", "Cleaned ${cleanedBytes / 1024} KB cache")
                }
                CacheManager.clearPdfOperationsCache(applicationContext)
            } catch (e: Exception) {
                Log.e("FamilyPDF", "Cache cleanup failed", e)
            }
        }
    }
}