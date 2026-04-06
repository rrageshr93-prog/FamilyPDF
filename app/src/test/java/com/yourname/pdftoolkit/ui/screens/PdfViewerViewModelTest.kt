package com.yourname.pdftoolkit.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfViewerViewModelTest {

    private lateinit var viewModel: PdfViewerViewModel

    @Before
    fun setup() {
        viewModel = PdfViewerViewModel()
    }

    @Test
    fun `initial state is correct`() {
        assertEquals(PdfTool.None, viewModel.toolState.value)
        assertEquals("", viewModel.searchState.value.query)
        assertEquals(SaveState.Idle, viewModel.saveState.value)
        assertEquals(AnnotationTool.NONE, viewModel.selectedAnnotationTool.value)
    }

    @Test
    fun `search with short query updates query but does not load`() = runBlocking {
        viewModel.search("a")
        // search checks length synchronously
        assertEquals("a", viewModel.searchState.value.query)
        assertFalse(viewModel.searchState.value.isLoading)
        assertTrue(viewModel.searchState.value.matches.isEmpty())
    }

    @Test
    fun `setTool updates toolState`() {
        viewModel.setTool(PdfTool.Search)
        assertTrue(viewModel.toolState.value is PdfTool.Search)

        viewModel.setTool(PdfTool.None)
        assertTrue(viewModel.toolState.value is PdfTool.None)
    }

    @Test
    fun `setTool Edit does not set default annotation tool`() {
        // Initial state
        assertEquals(AnnotationTool.NONE, viewModel.selectedAnnotationTool.value)

        viewModel.setTool(PdfTool.Edit)

        // Should NOT default to HIGHLIGHTER anymore (stays NONE)
        assertEquals(AnnotationTool.NONE, viewModel.selectedAnnotationTool.value)
        assertTrue(viewModel.toolState.value is PdfTool.Edit)
    }

    @Test
    fun `setAnnotationTool updates toolState to Edit`() {
        viewModel.setAnnotationTool(AnnotationTool.MARKER)

        assertEquals(AnnotationTool.MARKER, viewModel.selectedAnnotationTool.value)
        assertTrue(viewModel.toolState.value is PdfTool.Edit)
    }

    @Test
    fun `clearSearch resets state`() {
        viewModel.search("test")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchState.value.query)
        assertTrue(viewModel.searchState.value.matches.isEmpty())
    }

    @Test
    fun `addAnnotation adds stroke to annotations`() {
        val stroke = AnnotationStroke(
            pageIndex = 0,
            tool = AnnotationTool.HIGHLIGHTER,
            color = androidx.compose.ui.graphics.Color.Yellow,
            points = listOf(androidx.compose.ui.geometry.Offset(0f, 0f)),
            strokeWidth = 20f
        )
        
        viewModel.addAnnotation(stroke)
        
        assertEquals(1, viewModel.annotations.value.size)
        assertEquals(stroke, viewModel.annotations.value[0])
    }

    @Test
    fun `undoAnnotation removes last stroke and adds to redo stack`() {
        val stroke1 = AnnotationStroke(0, AnnotationTool.HIGHLIGHTER, androidx.compose.ui.graphics.Color.Yellow, 
            listOf(androidx.compose.ui.geometry.Offset(0f, 0f)), 20f)
        val stroke2 = AnnotationStroke(0, AnnotationTool.MARKER, androidx.compose.ui.graphics.Color.Red, 
            listOf(androidx.compose.ui.geometry.Offset(10f, 10f)), 8f)
        
        viewModel.addAnnotation(stroke1)
        viewModel.addAnnotation(stroke2)
        viewModel.undoAnnotation()
        
        assertEquals(1, viewModel.annotations.value.size)
        assertEquals(stroke1, viewModel.annotations.value[0])
        assertTrue(viewModel.canRedo())
    }

    @Test
    fun `redoAnnotation restores undone stroke`() {
        val stroke = AnnotationStroke(0, AnnotationTool.HIGHLIGHTER, androidx.compose.ui.graphics.Color.Yellow, 
            listOf(androidx.compose.ui.geometry.Offset(0f, 0f)), 20f)
        
        viewModel.addAnnotation(stroke)
        viewModel.undoAnnotation()
        
        assertTrue(viewModel.canRedo())
        assertEquals(0, viewModel.annotations.value.size)
        
        viewModel.redoAnnotation()
        
        assertEquals(1, viewModel.annotations.value.size)
        assertEquals(stroke, viewModel.annotations.value[0])
        assertFalse(viewModel.canRedo())
    }

    @Test
    fun `addAnnotation clears redo stack`() {
        val stroke1 = AnnotationStroke(0, AnnotationTool.HIGHLIGHTER, androidx.compose.ui.graphics.Color.Yellow, 
            listOf(androidx.compose.ui.geometry.Offset(0f, 0f)), 20f)
        val stroke2 = AnnotationStroke(0, AnnotationTool.MARKER, androidx.compose.ui.graphics.Color.Red, 
            listOf(androidx.compose.ui.geometry.Offset(10f, 10f)), 8f)
        
        viewModel.addAnnotation(stroke1)
        viewModel.undoAnnotation()
        assertTrue(viewModel.canRedo())
        
        viewModel.addAnnotation(stroke2)
        assertFalse(viewModel.canRedo())
    }

    @Test
    fun `undoAnnotation with empty list does nothing`() {
        viewModel.undoAnnotation()
        assertTrue(viewModel.annotations.value.isEmpty())
        assertFalse(viewModel.canRedo())
    }

    @Test
    fun `redoAnnotation with empty redo stack does nothing`() {
        val stroke = AnnotationStroke(0, AnnotationTool.HIGHLIGHTER, androidx.compose.ui.graphics.Color.Yellow, 
            listOf(androidx.compose.ui.geometry.Offset(0f, 0f)), 20f)
        
        viewModel.addAnnotation(stroke)
        viewModel.redoAnnotation() // Should do nothing
        
        assertEquals(1, viewModel.annotations.value.size)
    }

    @Test
    fun `multiple undo and redo cycles work correctly`() {
        val stroke1 = AnnotationStroke(0, AnnotationTool.HIGHLIGHTER, androidx.compose.ui.graphics.Color.Yellow, 
            listOf(androidx.compose.ui.geometry.Offset(0f, 0f)), 20f)
        val stroke2 = AnnotationStroke(0, AnnotationTool.MARKER, androidx.compose.ui.graphics.Color.Red, 
            listOf(androidx.compose.ui.geometry.Offset(10f, 10f)), 8f)
        val stroke3 = AnnotationStroke(0, AnnotationTool.UNDERLINE, androidx.compose.ui.graphics.Color.Blue, 
            listOf(androidx.compose.ui.geometry.Offset(20f, 20f)), 4f)
        
        viewModel.addAnnotation(stroke1)
        viewModel.addAnnotation(stroke2)
        viewModel.addAnnotation(stroke3)
        
        assertEquals(3, viewModel.annotations.value.size)
        
        // Undo twice
        viewModel.undoAnnotation()
        viewModel.undoAnnotation()
        assertEquals(1, viewModel.annotations.value.size)
        assertTrue(viewModel.canRedo())
        
        // Redo once
        viewModel.redoAnnotation()
        assertEquals(2, viewModel.annotations.value.size)
        assertTrue(viewModel.canRedo())
        
        // Add new annotation should clear redo stack
        val stroke4 = AnnotationStroke(1, AnnotationTool.HIGHLIGHTER, androidx.compose.ui.graphics.Color.Green, 
            listOf(androidx.compose.ui.geometry.Offset(30f, 30f)), 20f)
        viewModel.addAnnotation(stroke4)
        assertFalse(viewModel.canRedo())
        assertEquals(3, viewModel.annotations.value.size)
    }
}
