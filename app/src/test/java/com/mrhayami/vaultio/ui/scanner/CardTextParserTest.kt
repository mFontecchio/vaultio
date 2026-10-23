package com.mrhayami.vaultio.ui.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardTextParserTest {

    @Test
    fun `base set charizard reads number total and name as vintage`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("STAGE 2", 0.02f),
                top("Evolves from Charmeleon", 0.04f),
                top("Put Charizard on the Stage 1 card", 0.055f),
                top("Charizard 120 HP", 0.08f),
                bottom("100"),
                bottom("Illus. Mitsuhiro Arita ©1995, 96, 98, 99 Nintendo ©1999 Wizards 4/102")
            )
        )
        assertEquals("4", parsed.number)
        assertEquals("102", parsed.total)
        assertEquals("CHARIZARD", parsed.name)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `jungle clefable skips evolution lines`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("STAGE 1", 0.02f),
                top("Evolves from Clefairy", 0.04f),
                top("Put Clefable on the Basic Pokémon", 0.055f),
                top("Clefable 70 HP", 0.08f),
                bottom("©1999 Wizards 1/64")
            )
        )
        assertEquals("1", parsed.number)
        assertEquals("64", parsed.total)
        assertEquals("CLEFABLE", parsed.name)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `gym heroes keeps owner name`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("Evolves from Sabrina's Slowpoke", 0.04f),
                top("Sabrina's Slowbro 70 HP", 0.08f),
                bottom("60/132 ©1999-2000 Wizards")
            )
        )
        assertEquals("60", parsed.number)
        assertEquals("132", parsed.total)
        assertEquals("SABRINAS SLOWBRO", parsed.name)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `neo genesis lugia skips basic pokemon line`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("Basic Pokémon", 0.05f),
                top("Lugia", 0.08f),
                bottom("©1995-2000 Nintendo Wizards 9/111")
            )
        )
        assertEquals("9", parsed.number)
        assertEquals("111", parsed.total)
        assertEquals("LUGIA", parsed.name)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `energy header is not used as the name`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("ENERGY", 0.06f),
                OcrLine("Double Colorless Energy", topFraction = 0.75f, centerYFraction = 0.78f),
                bottom("©1999 Wizards 96/102")
            )
        )
        assertEquals("96", parsed.number)
        assertEquals("102", parsed.total)
        assertNull(parsed.name)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `trainer header alone is not used as the name`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("TRAINER", 0.05f),
                OcrLine("Mail from Bill", topFraction = 0.40f, centerYFraction = 0.43f),
                bottom("©1995-2000 Wizards 105/105")
            )
        )
        assertEquals("105", parsed.number)
        assertEquals("105", parsed.total)
        assertNull(parsed.name)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `trainer name in the top zone is kept`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("TRAINER", 0.04f),
                top("Mail from Bill", 0.12f),
                bottom("105/105 ©1999 Wizards")
            )
        )
        assertEquals("MAIL FROM BILL", parsed.name)
        assertEquals("105", parsed.number)
        assertEquals("105", parsed.total)
    }

    @Test
    fun `team rocket secret uses the printed total`() {
        val parsed = CardTextParser.parseLines(
            listOf(bottom("83/82 ©1999 Wizards"))
        )
        assertEquals("83", parsed.number)
        assertEquals("82", parsed.total)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `a dropped or extra digit on a vintage total is repaired`() {
        val dropped = CardTextParser.parseLines(listOf(bottom("©1999 Wizards 35/02")))
        assertEquals("35", dropped.number)
        assertEquals("102", dropped.total)
        assertEquals("102", CardTextParser.parseLines(listOf(bottom("©1999 Wizards 47/1102"))).total)
        assertEquals("64", CardTextParser.parseLines(listOf(bottom("12/164 ©1999 Wizards"))).total)
    }

    @Test
    fun `vintage keeps the longer collector number when the total agrees`() {
        val parsed = CardTextParser.parseLines(
            listOf(bottom("©1999 Wizards 4/102 41/102"))
        )
        assertEquals("41", parsed.number)
        assertEquals("102", parsed.total)
    }

    @Test
    fun `vintage prefers a numeric collector number over a name fragment`() {
        val parsed = CardTextParser.parseLines(
            listOf(bottom("CHARIZARD/62 11/62 ©1999 Wizards"))
        )
        assertEquals("11", parsed.number)
        assertEquals("62", parsed.total)
    }

    @Test
    fun `confusable digits in the collector number are repaired`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("Charizard", 0.08f),
                bottom("4/1O2")
            )
        )
        assertEquals("4", parsed.number)
        assertEquals("102", parsed.total)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `modern number total ignores an evolution line`() {
        val parsed = CardTextParser.parseLines(
            listOf(
                top("Evolves from Pichu", 0.03f),
                top("Pikachu 60 HP", 0.08f),
                bottom("025/198 ©2023 Pokemon")
            )
        )
        assertEquals("025", parsed.number)
        assertEquals("198", parsed.total)
        assertEquals("PIKACHU", parsed.name)
        assertEquals(CardEra.MODERN, parsed.era)
    }

    @Test
    fun `modern set prefix survives digit normalization`() {
        val parsed = CardTextParser.parseLines(listOf(bottom("SWSH 123")))
        assertEquals("SWSH123", parsed.number)
        assertNull(parsed.total)
        assertEquals(CardEra.MODERN, parsed.era)
    }

    @Test
    fun `modern trainer gallery slash number keeps letters`() {
        val parsed = CardTextParser.parseLines(listOf(bottom("TG05/TG30")))
        assertEquals("TG05", parsed.number)
        assertEquals("TG30", parsed.total)
        assertEquals(CardEra.MODERN, parsed.era)
    }

    @Test
    fun `a copyright year is not a standalone collector number`() {
        val parsed = CardTextParser.parseLines(listOf(bottom("1999")))
        assertNull(parsed.number)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `modern override keeps modern rules on a wizards card`() {
        val parsed = CardTextParser.parseLines(
            listOf(bottom("©1999 Wizards 4/102")),
            ScannerEraOverride.MODERN
        )
        assertEquals("4", parsed.number)
        assertEquals("102", parsed.total)
        assertEquals(CardEra.MODERN, parsed.era)
    }

    @Test
    fun `vintage override ignores a set prefix without a slash`() {
        val parsed = CardTextParser.parseLines(
            listOf(bottom("SWSH 123")),
            ScannerEraOverride.VINTAGE
        )
        assertNull(parsed.number)
        assertEquals(CardEra.VINTAGE, parsed.era)
    }

    @Test
    fun `lookup keys cover padded and bare numbers`() {
        assertEquals(listOf("4", "004"), CardTextParser.localIdLookupKeys("4"))
        assertEquals(listOf("025", "25"), CardTextParser.localIdLookupKeys("025"))
        assertEquals(listOf("TG05"), CardTextParser.localIdLookupKeys("TG05"))
    }

    private fun top(text: String, top: Float) = OcrLine(text, topFraction = top, centerYFraction = top + 0.015f)

    private fun bottom(text: String) = OcrLine(text, topFraction = 0.94f, centerYFraction = 0.96f)
}
