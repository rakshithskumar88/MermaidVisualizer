package com.alextdev.mermaidvisualizer

import com.alextdev.mermaidvisualizer.lang.MermaidFileType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Key
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.Base64

class MermaidDiagramOpenerTest : BasePlatformTestCase() {

    private val fileEditorManager get() = FileEditorManager.getInstance(project)

    override fun tearDown() {
        try {
            fileEditorManager.openFiles.forEach { fileEditorManager.closeFile(it) }
        } finally {
            super.tearDown()
        }
    }

    fun testOpenCreatesTabWithMermaidFileType() {
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        val file = fileEditorManager.openFiles.single()
        assertEquals(MermaidFileType, file.fileType)
        assertEquals("flowchart.mmd", file.name)
    }

    fun testOpenedFileHasPreviewOnlyLayoutUserData() {
        openDiagramInNewTab("sequenceDiagram\n    A->>B: Hi", project)
        val file = fileEditorManager.openFiles.single()
        // The platform key is Kotlin-internal but public in bytecode — reflection is the only test access.
        val layoutKey = TextEditorWithPreview::class.java
            .getField("DEFAULT_LAYOUT_FOR_FILE")
            .get(null) as Key<*>
        assertEquals(TextEditorWithPreview.Layout.SHOW_PREVIEW, file.getUserData(layoutKey))
    }

    fun testOpenTwiceSameSourceFocusesExistingTab() {
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        assertEquals(1, fileEditorManager.openFiles.size)
    }

    fun testWhitespaceVariantsDeduplicateToSameTab() {
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        openDiagramInNewTab("\n  flowchart TD\n    A --> B  \n", project)
        assertEquals(1, fileEditorManager.openFiles.size)
    }

    fun testDifferentSourcesOpenDistinctTabs() {
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        openDiagramInNewTab("sequenceDiagram\n    A->>B: Hi", project)
        assertEquals(2, fileEditorManager.openFiles.size)
    }

    fun testReopenAfterCloseCreatesFreshTab() {
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        fileEditorManager.closeFile(fileEditorManager.openFiles.single())
        openDiagramInNewTab("flowchart TD\n    A --> B", project)
        val file = fileEditorManager.openFiles.single()
        assertTrue(fileEditorManager.isFileOpen(file))
    }

    fun testNullProjectDoesNotThrow() {
        openDiagramInNewTab("flowchart TD\n    A --> B", null)
    }

    fun testBlankSourceDoesNotOpen() {
        openDiagramInNewTab("   \n  ", project)
        assertEquals(0, fileEditorManager.openFiles.size)
    }

    fun testInvalidBase64DoesNotThrow() {
        openDiagramInNewTabFromBase64("not-valid-base64!!", project)
        assertEquals(0, fileEditorManager.openFiles.size)
    }

    fun testBase64VariantDecodesUtf8() {
        val source = "flowchart TD\n    A[Début] --> B[Été ☀]"
        val b64 = Base64.getEncoder().encodeToString(source.toByteArray(Charsets.UTF_8))
        openDiagramInNewTabFromBase64(b64, project)
        val file = fileEditorManager.openFiles.single()
        assertEquals(source, String(file.contentsToByteArray(), Charsets.UTF_8))
    }
}
