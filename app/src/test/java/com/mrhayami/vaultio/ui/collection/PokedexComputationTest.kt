package com.mrhayami.vaultio.ui.collection

import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokedexComputationTest {

    @Test
    fun `null dex card still marks species collected via name lookup`() {
        val card = cardWithDetails(
            id = "swsh1-25",
            name = "Pikachu",
            pokemonName = "Pikachu",
            dexId = null,
            dexIds = null
        )

        val entries = computePokedexEntries(
            userCards = listOf(card),
            settings = PokedexSettings(showUncollected = false)
        )

        val pikachu = entries.single { it.dexNumber == 25 }
        assertTrue(pikachu.isCollected)
        assertEquals("Pikachu", pikachu.pokemonName)
        assertEquals(1, pikachu.cardCount)
    }

    @Test
    fun `stored dexId is preferred over name fallback`() {
        val card = cardWithDetails(
            id = "swsh1-6",
            name = "Charizard",
            pokemonName = "Charizard",
            dexId = "6",
            dexIds = "[6]"
        )

        val entries = computePokedexEntries(
            userCards = listOf(card),
            settings = PokedexSettings(showUncollected = false)
        )

        assertEquals(listOf(6), entries.filter { it.isCollected }.map { it.dexNumber })
    }

    private fun cardWithDetails(
        id: String,
        name: String,
        pokemonName: String?,
        dexId: String?,
        dexIds: String?
    ) = CardWithDetails(
        userCard = UserCardEntity(id = 1L, cardId = id, quantity = 1),
        card = CardEntity(
            id = id,
            localId = id.substringAfter("-"),
            name = name,
            image = null,
            setId = id.substringBefore("-"),
            rarity = "Common",
            category = "Pokemon",
            types = "Electric",
            dexId = dexId,
            dexIds = dexIds,
            pokemonName = pokemonName
        ),
        set = SetEntity(
            id = id.substringBefore("-"),
            name = "Test Set",
            series = "Test",
            logo = null,
            symbol = null,
            totalCards = 100,
            officialCards = 100,
            releaseDate = null,
            isDownloaded = false
        )
    )
}
