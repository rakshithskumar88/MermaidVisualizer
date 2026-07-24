package com.alextdev.mermaidvisualizer

import com.alextdev.mermaidvisualizer.lang.MermaidFileType
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.concurrency.ThreadingAssertions
import java.util.Base64

private val LOG = Logger.getInstance("MermaidDiagramOpener")

private val DIAGRAM_TYPE_WORD = Regex("[A-Za-z][A-Za-z0-9-]*")

private const val FALLBACK_TAB_NAME = "diagram"

/**
 * Derives the tab file name from the diagram type keyword ("flowchart TD ..." -> "flowchart.mmd"),
 * skipping blank lines, `%%` comments/directives and a leading YAML frontmatter block.
 * Falls back to "diagram.mmd" when no identifier-like first word is found.
 */
internal fun diagramTabFileName(source: String): String {
    val lines = source.lines().map { it.trim() }
    var i = 0
    while (i < lines.size && (lines[i].isEmpty() || lines[i].startsWith("%%"))) i++
    if (i < lines.size && lines[i] == "---") {
        i++
        while (i < lines.size && lines[i] != "---") i++
        i++
        while (i < lines.size && (lines[i].isEmpty() || lines[i].startsWith("%%"))) i++
    }
    val word = lines.getOrNull(i)?.split(' ', '\t', ':', ';', '{')?.firstOrNull()
    val name = word?.let { DIAGRAM_TYPE_WORD.matchEntire(it)?.value } ?: FALLBACK_TAB_NAME
    return "$name.mmd"
}

/**
 * Opens the given Mermaid source in a separate preview-only editor tab.
 * Must be called on the EDT (both bridge entry points dispatch via invokeLater).
 */
internal fun openDiagramInNewTab(source: String, project: Project?) {
    if (project == null || project.isDisposed) {
        LOG.warn("Cannot open diagram in a new tab: no project available")
        notifyMermaid(project, MyMessageBundle.message("open.in.tab.failed"), NotificationType.ERROR)
        return
    }
    if (source.isBlank()) {
        LOG.warn("Open in new tab requested with blank source")
        notifyMermaid(project, MyMessageBundle.message("open.in.tab.failed"), NotificationType.ERROR)
        return
    }
    project.service<MermaidDiagramTabService>().open(source)
}

/** Markdown-preview entry point: the source travels through the BrowserPipe as base64 UTF-8. */
internal fun openDiagramInNewTabFromBase64(b64: String, project: Project?) {
    val source = try {
        String(Base64.getDecoder().decode(b64), Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        LOG.warn("Failed to open diagram in a new tab: invalid base64 (length=${b64.length})", e)
        notifyMermaid(project, MyMessageBundle.message("open.in.tab.failed"), NotificationType.ERROR)
        return
    }
    openDiagramInNewTab(source, project)
}

/**
 * Opens diagram snapshots as [LightVirtualFile] tabs, deduplicated by source content so that
 * clicking the button twice focuses the existing tab instead of stacking copies.
 * [TextEditorWithPreview.openPreviewForFile] puts the platform DEFAULT_LAYOUT_FOR_FILE key on the
 * file, so the MermaidSplitEditor opens in preview-only layout. EDT-only, no synchronization needed.
 */
@Service(Service.Level.PROJECT)
internal class MermaidDiagramTabService(private val project: Project) {

    private val openTabs = HashMap<String, LightVirtualFile>()

    fun open(source: String) {
        ThreadingAssertions.assertEventDispatchThread()
        val fileEditorManager = FileEditorManager.getInstance(project)
        openTabs.values.removeIf { !fileEditorManager.isFileOpen(it) }
        val trimmed = source.trim()
        val file = openTabs.getOrPut(trimmed) {
            LightVirtualFile(diagramTabFileName(trimmed), MermaidFileType, trimmed)
        }
        TextEditorWithPreview.openPreviewForFile(project, file)
    }
}
