package com.alextdev.mermaidvisualizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MermaidDiagramTabNameTest {

    @Test
    fun `simple flowchart`() {
        assertEquals("flowchart.mmd", diagramTabFileName("flowchart TD\n    A --> B"))
    }

    @Test
    fun `sequence diagram`() {
        assertEquals("sequenceDiagram.mmd", diagramTabFileName("sequenceDiagram\n    Alice->>Bob: Hi"))
    }

    @Test
    fun `diagram type with version suffix`() {
        assertEquals("stateDiagram-v2.mmd", diagramTabFileName("stateDiagram-v2\n    [*] --> Idle"))
    }

    @Test
    fun `pie with colon-glued first word`() {
        assertEquals("pie.mmd", diagramTabFileName("pie: title Pets"))
    }

    @Test
    fun `first word glued to semicolon`() {
        assertEquals("graph.mmd", diagramTabFileName("graph;"))
    }

    @Test
    fun `first word glued to opening brace`() {
        assertEquals("erDiagram.mmd", diagramTabFileName("erDiagram{"))
    }

    @Test
    fun `leading blank lines are skipped`() {
        assertEquals("flowchart.mmd", diagramTabFileName("\n\n   \nflowchart LR\n    A --> B"))
    }

    @Test
    fun `leading comments are skipped`() {
        assertEquals("gantt.mmd", diagramTabFileName("%% a comment\n%% another\ngantt\n    title Plan"))
    }

    @Test
    fun `init directive is skipped`() {
        assertEquals("flowchart.mmd", diagramTabFileName("%%{init: {'theme': 'dark'}}%%\nflowchart TD\n    A --> B"))
    }

    @Test
    fun `yaml frontmatter is skipped`() {
        assertEquals("pie.mmd", diagramTabFileName("---\ntitle: Animals\n---\npie\n    \"Dogs\": 50"))
    }

    @Test
    fun `frontmatter followed by comment`() {
        assertEquals("timeline.mmd", diagramTabFileName("---\ntitle: X\n---\n%% comment\ntimeline\n    2024 : event"))
    }

    @Test
    fun `blank source falls back to diagram`() {
        assertEquals("diagram.mmd", diagramTabFileName(""))
        assertEquals("diagram.mmd", diagramTabFileName("   \n  \n"))
    }

    @Test
    fun `non-identifier first word falls back to diagram`() {
        assertEquals("diagram.mmd", diagramTabFileName("123 --> B"))
    }

    @Test
    fun `comments only falls back to diagram`() {
        assertEquals("diagram.mmd", diagramTabFileName("%% nothing but comments"))
    }
}
