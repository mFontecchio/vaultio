package com.mrhayami.vaultio.data

import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrintedCardNumberTest {

    @Test
    fun `parses a printed number and ignores case and spaces`() {
        val parsed = PrintedCardNumber.parse("  tg05 / tg30 ")
        assertEquals("TG05", parsed?.localId)
        assertEquals("TG30", parsed?.total)
        assertNull(parsed?.totalCount)
    }

    @Test
    fun `rejects names, bare numbers, and a half-typed slash`() {
        assertNull(PrintedCardNumber.parse("pikachu"))
        assertNull(PrintedCardNumber.parse("190"))
        assertNull(PrintedCardNumber.parse("n/a"))
        assertNull(PrintedCardNumber.parse("190/"))
        assertEquals(true, PrintedCardNumber.isIncompleteNumber("190/"))
        assertEquals(false, PrintedCardNumber.isIncompleteNumber("190/250"))
        assertEquals(false, PrintedCardNumber.isIncompleteNumber("pikachu"))
    }

    @Test
    fun `digit keys match the scanner lookup`() {
        assertEquals(listOf("4", "004"), PrintedCardNumber.digitLookupKeys("4"))
        assertEquals(listOf("025", "25"), PrintedCardNumber.digitLookupKeys("025"))
        assertEquals(listOf("190"), PrintedCardNumber.parse("190/250")?.localIdKeys())
        assertEquals(listOf("025", "25"), PrintedCardNumber.parse("025/165")?.localIdKeys())
    }

    @Test
    fun `prefixed keys add the unpadded and two-digit forms only`() {
        assertEquals(listOf("TG05", "TG5"), PrintedCardNumber.parse("TG05/TG30")?.localIdKeys())
        assertEquals(listOf("TG5", "TG05"), PrintedCardNumber.parse("tg5/tg30")?.localIdKeys())
        assertEquals(listOf("SV107"), PrintedCardNumber.parse("SV107/SV122")?.localIdKeys())
    }

    @Test
    fun `collection clause matches local id and either set count`() {
        val clause = collectionSearchClause("190/250")
        assertEquals(
            " AND UPPER(c.localId) IN (?) AND (s.officialCards = ? OR s.totalCards = ?) ",
            clause?.sql
        )
        assertEquals(listOf("190", 250, 250), clause?.args)
    }

    @Test
    fun `collection clause skips set size when the denominator is not an integer`() {
        val clause = collectionSearchClause("TG05/TG30")
        assertEquals(" AND UPPER(c.localId) IN (?, ?) ", clause?.sql)
        assertEquals(listOf("TG05", "TG5"), clause?.args)
    }

    @Test
    fun `collection clause keeps name search and blocks a half-typed slash`() {
        val name = collectionSearchClause("pika")
        assertEquals(
            " AND (c.name LIKE ? OR c.pokemonName LIKE ? OR s.name LIKE ?) ",
            name?.sql
        )
        assertEquals(listOf("%pika%", "%pika%", "%pika%"), name?.args)
        assertEquals(" AND 0 ", collectionSearchClause("190/")?.sql)
        assertNull(collectionSearchClause("  "))
    }

    @Test
    fun `set size filter keeps official or full count and drops an unknown set`() {
        val cards = listOf(card("sv1-190"), card("swsh1-190"), card("missing-190"))
        val sets = mapOf(
            "sv1" to set("sv1", official = 198, total = 250),
            "swsh1" to set("swsh1", official = 202, total = 216)
        )
        assertEquals(listOf("sv1-190"), filterCardsByPrintedTotal(cards, 250, sets).map { it.id })
        assertEquals(cards, filterCardsByPrintedTotal(cards, 250, emptyMap()))
        assertEquals(emptyList<String>(), filterCardsByPrintedTotal(cards, 999, sets).map { it.id })
    }

    private fun card(id: String) = TcgDexCard(
        id = id,
        localId = id.substringAfterLast('-'),
        name = "Card",
        image = null,
        rarity = null,
        category = "Pokemon"
    )

    private fun set(id: String, official: Int, total: Int) =
        SetEntity(
            id = id,
            name = id,
            series = null,
            logo = null,
            symbol = null,
            totalCards = total,
            officialCards = official,
            releaseDate = null
        )
}
