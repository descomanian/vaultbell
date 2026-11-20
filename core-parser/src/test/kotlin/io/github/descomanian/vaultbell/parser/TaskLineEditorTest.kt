package io.github.descomanian.vaultbell.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLineEditorTest {
    @Test
    fun marksOpenTaskDone() {
        assertEquals("- [x] a (@2026-11-14)", TaskLineEditor.markDone("- [ ] a (@2026-11-14)"))
        assertEquals("\t* [x] b", TaskLineEditor.markDone("\t* [ ] b"))
        assertEquals("1. [x] c", TaskLineEditor.markDone("1. [ ] c"))
        assertNull(TaskLineEditor.markDone("- [x] done"))
        assertNull(TaskLineEditor.markDone("plain"))
    }

    @Test
    fun editsOnlyWhenHashMatchesAndKeepsLineEndings() {
        val content = "# T\r\n- [ ] a (@2026-11-14 10:00)\r\n- [ ] b\r\n"
        val hash = lineHashOf("- [ ] a (@2026-11-14 10:00)")
        val result = TaskLineEditor.markDoneInContent(content, 1, hash)
        assertTrue(result is TaskLineEditor.EditResult.Success)
        assertEquals(
            "# T\r\n- [x] a (@2026-11-14 10:00)\r\n- [ ] b\r\n",
            (result as TaskLineEditor.EditResult.Success).content
        )
    }

    @Test
    fun conflictWhenLineChanged() {
        val content = "- [ ] a (@2026-11-14 10:30)\n"
        val oldHash = lineHashOf("- [ ] a (@2026-11-14 10:00)")
        assertEquals(TaskLineEditor.EditResult.Conflict, TaskLineEditor.markDoneInContent(content, 0, oldHash))
        assertEquals(TaskLineEditor.EditResult.Conflict, TaskLineEditor.markDoneInContent(content, 5, oldHash))
    }

    @Test
    fun notATaskWhenAlreadyDone() {
        val line = "- [x] a"
        assertEquals(TaskLineEditor.EditResult.NotATask, TaskLineEditor.markDoneInContent(line, 0, lineHashOf(line)))
    }

    @Test
    fun neverChecksOffRepeatingTasks() {
        val lines =
            listOf(
                "- [ ] Pflanzen gießen (@2026-11-20 09:00) | daily",
                "- [ ] Team-Retro ⏰ 2026-11-20 16:00 🔁 every week",
                "- [ ] Müll rausbringen 📅 2026-11-20 🔁 every 2 weeks",
                "- [ ] Bericht [due:: 2026-11-20] [repeat:: every month on the 1st]"
            )
        for (line in lines) {
            assertNull(line, TaskLineEditor.markDone(line))
            val content = "# Routinen\n$line\n"
            assertEquals(line, TaskLineEditor.EditResult.Repeating, TaskLineEditor.markDoneInContent(content, 1, lineHashOf(line)))
            val bytes = Utf8.encode(content)
            val file = TaskLineEditor.markDoneInFile(bytes, 1, lineHashOf(line))
            assertEquals(TaskLineEditor.FileEditResult.Rejected(TaskLineEditor.EditResult.Repeating), file)
        }
    }
}
