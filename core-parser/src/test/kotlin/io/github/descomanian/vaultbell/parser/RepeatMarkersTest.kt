package io.github.descomanian.vaultbell.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatMarkersTest {
    @Test
    fun detectsSupportedAndUnsupportedRules() {
        assertTrue(RepeatMarkers.isRepeating("- [ ] Pflanzen gießen (@2026-11-20 09:00) | daily"))
        assertTrue(RepeatMarkers.isRepeating("- [ ] Team-Retro ⏰ 2026-11-20 16:00 🔁 every week"))
        assertTrue(RepeatMarkers.isRepeating("- [ ] Müll rausbringen 📅 2026-11-20 🔁 every 2 weeks"))
        assertTrue(RepeatMarkers.isRepeating("- [ ] Lauftreff 🔁️ every monday ⏰ 2026-11-23 18:00"))
        assertTrue(RepeatMarkers.isRepeating("- [ ] Bericht [due:: 2026-11-20] [repeat:: every month]"))
        assertTrue(RepeatMarkers.isRepeating("- [ ] Geburtstagskarte (@2026-11-20) | yearly"))
    }

    @Test
    fun oneTimeTasksAreNotRepeating() {
        assertFalse(RepeatMarkers.isRepeating("- [ ] Paket abholen (@2026-11-20 18:30)"))
        assertFalse(RepeatMarkers.isRepeating("- [ ] Tabelle | Spalte (@2026-11-20)"))
        assertFalse(RepeatMarkers.isRepeating("- [ ] Every day matters (@2026-11-20)"))
        assertFalse(RepeatMarkers.isRepeating("- [ ] Abgabe [due:: 2026-11-20]"))
    }

    @Test
    fun ruleTextAsWritten() {
        assertEquals("every 2 weeks", RepeatMarkers.ruleText("- [ ] Müll 📅 2026-11-20 🔁 every 2 weeks"))
        assertEquals("every week", RepeatMarkers.ruleText("- [ ] Retro 🔁 every week ⏰ 2026-11-20 16:00"))
        assertEquals("every month", RepeatMarkers.ruleText("- [ ] Bericht [due:: 2026-11-20] [repeat:: every month]"))
        assertEquals("daily", RepeatMarkers.ruleText("- [ ] Pflanzen (@2026-11-20 09:00) | daily"))
        assertEquals("", RepeatMarkers.ruleText("- [ ] Nur Emoji 🔁"))
        assertNull(RepeatMarkers.ruleText("- [ ] Einmalig (@2026-11-20)"))
    }
}
