package com.mrhayami.vaultio.ui.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PokedexGenerationTest {

    @Test
    fun `kanto contains bulbasaur and mew`() {
        val kanto = PokedexGenerations.first { it.id == 1 }
        assertTrue(kanto.contains(1))
        assertTrue(kanto.contains(151))
        assertFalse(kanto.contains(152))
    }

    @Test
    fun `entriesForGeneration filters to selected range`() {
        val entries = listOf(
            PokedexEntry(1, "Bulbasaur", 1, 1, null, "", true),
            PokedexEntry(25, "Pikachu", 1, 1, null, "", true),
            PokedexEntry(155, "Cyndaquil", 1, 1, null, "", true)
        )
        val kanto = PokedexGenerations.first { it.id == 1 }
        val filtered = entriesForGeneration(entries, kanto)
        assertEquals(listOf(1, 25), filtered.map { it.dexNumber })
    }

    @Test
    fun `generation progress is complete when all collected`() {
        val kanto = PokedexGenerations.first { it.id == 1 }
        val entries = (1..151).map {
            PokedexEntry(it, "P", 1, 1, null, "", isCollected = true)
        }
        val progress = generationProgress(entries, kanto)
        assertTrue(progress.complete)
        assertEquals(151, progress.collected)
    }
}
