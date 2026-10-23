package com.mrhayami.vaultio.data.repository

import android.content.Context
import coil.ImageLoader
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.ApiUsageDao
import com.mrhayami.vaultio.data.local.CardDao
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CollectionSnapshotDao
import com.mrhayami.vaultio.data.local.FolderDao
import com.mrhayami.vaultio.data.local.PriceDao
import com.mrhayami.vaultio.data.local.SetDao
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.TelemetryDao
import com.mrhayami.vaultio.data.local.UserCardDao
import com.mrhayami.vaultio.data.local.WishlistDao
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.JustTcgCard
import com.mrhayami.vaultio.data.remote.JustTcgResponse
import com.mrhayami.vaultio.data.remote.TcgDexApi
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.remote.TcgDexSetDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultioRepositoryTest {

    private val context = mockk<Context>()
    private val setDao = mockk<SetDao>(relaxed = true)
    private val cardDao = mockk<CardDao>(relaxed = true)
    private val userCardDao = mockk<UserCardDao>(relaxed = true)
    private val folderDao = mockk<FolderDao>(relaxed = true)
    private val priceDao = mockk<PriceDao>(relaxed = true)
    private val apiUsageDao = mockk<ApiUsageDao>(relaxed = true)
    private val telemetryDao = mockk<TelemetryDao>(relaxed = true)
    private val collectionSnapshotDao = mockk<CollectionSnapshotDao>(relaxed = true)
    private val wishlistDao = mockk<WishlistDao>(relaxed = true)
    private val tcgDexApi = mockk<TcgDexApi>(relaxed = true)
    private val justTcgApi = mockk<JustTcgApi>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val imageLoader = mockk<ImageLoader>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: VaultioRepository

    @Before
    fun setup() {
        // Relax JustTCG response metadata to avoid NPE in syncApiUsage
        coEvery { justTcgApi.getCardsBatch(any(), any()) } returns JustTcgResponse(
            data = emptyList(),
            metadata = null
        )

        repository = VaultioRepository(
            context, setDao, cardDao, userCardDao, folderDao, priceDao,
            apiUsageDao, telemetryDao, collectionSnapshotDao, wishlistDao,
            tcgDexApi, justTcgApi, userPreferencesRepository, imageLoader,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `updatePricesBatch uses JustTCG batch API for modern cards with tcgPlayerId`() = runTest {
        // Arrange
        val modernCard = CardEntity(
            id = "swsh1-1",
            localId = "1",
            name = "Pikachu",
            image = "url",
            setId = "swsh1",
            rarity = "Common",
            category = "Pokemon",
            types = "Electric",
            dexId = "25",
            tcgPlayerId = "12345"
        )
        val cards = listOf(modernCard)

        every { userPreferencesRepository.justTcgApiKey } returns flowOf("fake_api_key")
        coEvery { apiUsageDao.getUsageForDate(any()) } returns null

        // Mock specific response for this test
        coEvery { justTcgApi.getCardsBatch("fake_api_key", any()) } returns JustTcgResponse(
            data = listOf(
                JustTcgCard(
                    id = "j1",
                    name = "Pikachu",
                    tcgplayerId = "12345",
                    variants = emptyList()
                )
            )
        )

        // Act
        repository.updatePricesBatch(cards)

        // Assert
        coVerify {
            justTcgApi.getCardsBatch("fake_api_key", any())
        }
    }

    @Test
    fun `import enriches existing null-dex catalog stub`() = runTest(testDispatcher) {
        val cardId = "swsh1-25"
        val stub = CardEntity(
            id = cardId,
            localId = "25",
            name = "Pikachu",
            image = "stub.png",
            setId = "swsh1",
            rarity = null,
            category = null,
            types = null,
            dexId = null,
            pHash = 42L
        )
        val remote = TcgDexCard(
            id = cardId,
            localId = "25",
            name = "Pikachu",
            image = "full.png",
            rarity = "Common",
            category = "Pokemon",
            dexId = listOf(25),
            types = listOf("Electric")
        )

        coEvery { setDao.getSetById("swsh1") } returns SetEntity(
            id = "swsh1",
            name = "Sword & Shield",
            series = "SWSH",
            logo = "https://assets.tcgdex.net/en/swsh/swsh1/logo",
            symbol = null,
            totalCards = 202,
            officialCards = 202,
            releaseDate = null,
            isDownloaded = true
        )
        coEvery { cardDao.getCardById(cardId) } returns stub
        coEvery { tcgDexApi.getCardDetail(cardId) } returns remote
        coEvery { userCardDao.insertUserCard(any()) } returns 1L
        coEvery { cardDao.getReferencedStubCardIds(null) } returns emptyList()
        coEvery { cardDao.insertCards(any()) } returns listOf(1L)

        val json = """
            {
              "version": 1,
              "exportDate": 1,
              "folders": [],
              "userCards": [{
                "cardId": "$cardId",
                "quantity": 1,
                "condition": "Near Mint",
                "printing": "unlimited",
                "finish": "normal",
                "manualPrice": null,
                "dateAdded": 1,
                "folderIds": []
              }]
            }
        """.trimIndent()

        val result = repository.importCollectionFromJson(json)
        assertTrue(result.isSuccess)

        val inserted = slot<List<CardEntity>>()
        coVerify { cardDao.insertCards(capture(inserted)) }
        val entity = inserted.captured.single()
        assertEquals("25", entity.dexId)
        assertEquals(42L, entity.pHash)
        assertEquals("Pikachu", entity.pokemonName)
        coVerify { tcgDexApi.getCardDetail(cardId) }
    }

    @Test
    fun `import skips detail fetch when catalog already has dexId`() = runTest(testDispatcher) {
        val cardId = "swsh1-25"
        val existing = CardEntity(
            id = cardId,
            localId = "25",
            name = "Pikachu",
            image = "url",
            setId = "swsh1",
            rarity = "Common",
            category = "Pokemon",
            types = "Electric",
            dexId = "25",
            dexIds = "[25]",
            pokemonName = "Pikachu"
        )

        coEvery { setDao.getSetById("swsh1") } returns SetEntity(
            id = "swsh1",
            name = "Sword & Shield",
            series = "SWSH",
            logo = "https://assets.tcgdex.net/en/swsh/swsh1/logo",
            symbol = null,
            totalCards = 202,
            officialCards = 202,
            releaseDate = null,
            isDownloaded = true
        )
        coEvery { cardDao.getCardById(cardId) } returns existing
        coEvery { userCardDao.insertUserCard(any()) } returns 1L
        coEvery { cardDao.getReferencedStubCardIds(null) } returns emptyList()

        val json = """
            {
              "version": 1,
              "exportDate": 1,
              "folders": [],
              "userCards": [{
                "cardId": "$cardId",
                "quantity": 1,
                "condition": "Near Mint",
                "printing": "unlimited",
                "finish": "normal",
                "manualPrice": null,
                "dateAdded": 1,
                "folderIds": []
              }]
            }
        """.trimIndent()

        val result = repository.importCollectionFromJson(json)
        assertTrue(result.isSuccess)

        coVerify(exactly = 0) { tcgDexApi.getCardDetail(any()) }
        coVerify(exactly = 0) { cardDao.insertCards(any()) }
    }

    @Test
    fun `downloadSet preserves enriched fields and pHash of existing rows`() = runTest(testDispatcher) {
        val cardId = "sv1-25"
        val existing = CardEntity(
            id = cardId,
            localId = "25",
            name = "Pikachu",
            image = "old.png",
            setId = "sv1",
            rarity = "Rare",
            category = "Pokemon",
            types = "Electric",
            dexId = "25",
            dexIds = "[25]",
            pokemonName = "Pikachu",
            tcgPlayerId = "123",
            pHash = 99L
        )
        coEvery { cardDao.getCardsBySetSync("sv1") } returns listOf(existing)
        coEvery { cardDao.getReferencedStubCardIds("sv1") } returns emptyList()
        coEvery { tcgDexApi.getSetDetail("sv1") } returns TcgDexSetDetail(
            id = "sv1",
            name = "Scarlet & Violet",
            cards = listOf(
                TcgDexCard(
                    id = cardId,
                    localId = "25",
                    name = "Pikachu",
                    image = "brief.png",
                    rarity = null,
                    category = null
                )
            )
        )
        coEvery { cardDao.insertCards(any()) } returns listOf(1L)

        repository.downloadSet("sv1")

        val inserted = slot<List<CardEntity>>()
        coVerify { cardDao.insertCards(capture(inserted)) }
        val entity = inserted.captured.single()
        assertEquals("brief.png", entity.image)
        assertEquals("Rare", entity.rarity)
        assertEquals("Pokemon", entity.category)
        assertEquals("Electric", entity.types)
        assertEquals("25", entity.dexId)
        assertEquals("[25]", entity.dexIds)
        assertEquals("Pikachu", entity.pokemonName)
        assertEquals("123", entity.tcgPlayerId)
        assertEquals(99L, entity.pHash)
        coVerify(exactly = 0) { tcgDexApi.getCardDetail(any()) }
    }

    @Test
    fun `downloadSet inserts new cards unchanged`() = runTest(testDispatcher) {
        val cardId = "sv1-1"
        coEvery { cardDao.getCardsBySetSync("sv1") } returns emptyList()
        coEvery { cardDao.getReferencedStubCardIds("sv1") } returns emptyList()
        coEvery { tcgDexApi.getSetDetail("sv1") } returns TcgDexSetDetail(
            id = "sv1",
            name = "Scarlet & Violet",
            cards = listOf(
                TcgDexCard(
                    id = cardId,
                    localId = "1",
                    name = "Sprigatito",
                    image = "brief.png",
                    rarity = null,
                    category = null
                )
            )
        )
        coEvery { cardDao.insertCards(any()) } returns listOf(1L)

        repository.downloadSet("sv1")

        val inserted = slot<List<CardEntity>>()
        coVerify { cardDao.insertCards(capture(inserted)) }
        val entity = inserted.captured.single()
        assertEquals(cardId, entity.id)
        assertEquals("1", entity.localId)
        assertEquals("Sprigatito", entity.name)
        assertEquals("brief.png", entity.image)
        assertEquals("sv1", entity.setId)
        assertEquals(null, entity.rarity)
        assertEquals(null, entity.category)
        assertEquals(null, entity.types)
        assertEquals(null, entity.dexId)
        assertEquals(null, entity.pHash)
    }

    @Test
    fun `repair enriches owned stubs after download`() = runTest(testDispatcher) {
        val stubId = "sv1-1"
        val enrichedId = "sv1-2"
        val stub = CardEntity(
            id = stubId,
            localId = "1",
            name = "Pikachu",
            image = "stub.png",
            setId = "sv1",
            rarity = null,
            category = null,
            types = null,
            dexId = null,
            pHash = 7L
        )
        val enriched = CardEntity(
            id = enrichedId,
            localId = "2",
            name = "Sprigatito",
            image = "full.png",
            setId = "sv1",
            rarity = "Common",
            category = "Pokemon",
            types = "Grass",
            dexId = "906",
            dexIds = "[906]",
            pokemonName = "Sprigatito",
            pHash = 8L
        )
        coEvery { cardDao.getCardsBySetSync("sv1") } returns listOf(stub, enriched)
        coEvery { cardDao.getReferencedStubCardIds("sv1") } returns listOf(stubId)
        coEvery { cardDao.getCardById(stubId) } returns stub
        coEvery { tcgDexApi.getCardDetail(stubId) } returns TcgDexCard(
            id = stubId,
            localId = "1",
            name = "Pikachu",
            image = "full.png",
            rarity = "Common",
            category = "Pokemon",
            dexId = listOf(25),
            types = listOf("Electric")
        )
        coEvery { tcgDexApi.getSetDetail("sv1") } returns TcgDexSetDetail(
            id = "sv1",
            name = "Scarlet & Violet",
            cards = listOf(
                TcgDexCard(
                    id = stubId,
                    localId = "1",
                    name = "Pikachu",
                    image = "stub.png",
                    rarity = null,
                    category = null
                ),
                TcgDexCard(
                    id = enrichedId,
                    localId = "2",
                    name = "Sprigatito",
                    image = "brief.png",
                    rarity = null,
                    category = null
                )
            )
        )
        coEvery { cardDao.insertCards(any()) } returns listOf(1L)

        repository.downloadSet("sv1")

        coVerify(exactly = 1) { tcgDexApi.getCardDetail(stubId) }
        coVerify(exactly = 0) { tcgDexApi.getCardDetail(enrichedId) }

        val batches = mutableListOf<List<CardEntity>>()
        coVerify { cardDao.insertCards(capture(batches)) }
        val repaired = batches.last().single()
        assertEquals(stubId, repaired.id)
        assertEquals("25", repaired.dexId)
        assertEquals("Pokemon", repaired.category)
        assertEquals("Electric", repaired.types)
        assertEquals(7L, repaired.pHash)
    }

    @Test
    fun `manual add search by printed number keeps cards with that set size`() = runTest {
        coEvery { tcgDexApi.searchCardsByLocalId("190") } returns listOf(
            TcgDexCard(id = "sv1-190", localId = "190", name = "Miraidon", image = null, rarity = null, category = "Pokemon"),
            TcgDexCard(id = "swsh1-190", localId = "190", name = "Other", image = null, rarity = null, category = "Pokemon")
        )
        coEvery { setDao.getSetsByIds(any()) } returns listOf(
            SetEntity(id = "sv1", name = "Paldea", series = null, logo = null, symbol = null, totalCards = 250, officialCards = 198, releaseDate = null),
            SetEntity(id = "swsh1", name = "Sword", series = null, logo = null, symbol = null, totalCards = 216, officialCards = 202, releaseDate = null)
        )

        val results = repository.searchCardsForManualAdd("190/250")

        assertEquals(listOf("sv1-190"), results.map { it.id })
        coVerify(exactly = 0) { tcgDexApi.searchCards(any()) }
    }

    @Test
    fun `manual add search returns local id hits when the set catalog is empty`() = runTest {
        coEvery { tcgDexApi.searchCardsByLocalId("190") } returns listOf(
            TcgDexCard(id = "sv1-190", localId = "190", name = "Miraidon", image = null, rarity = null, category = "Pokemon")
        )
        coEvery { setDao.getSetsByIds(any()) } returns emptyList()

        val results = repository.searchCardsForManualAdd("190/250")

        assertEquals(listOf("sv1-190"), results.map { it.id })
    }

    @Test
    fun `manual add search of a gallery number does not filter by set size`() = runTest {
        val gallery = TcgDexCard(id = "swsh9-TG05", localId = "TG05", name = "Friends", image = null, rarity = null, category = "Trainer")
        coEvery { tcgDexApi.searchCardsByLocalId("TG05") } returns listOf(gallery)
        coEvery { tcgDexApi.searchCardsByLocalId("TG5") } returns emptyList()

        val results = repository.searchCardsForManualAdd("TG05/TG30")

        assertEquals(listOf("swsh9-TG05"), results.map { it.id })
        coVerify(exactly = 0) { setDao.getSetsByIds(any()) }
    }

    @Test
    fun `manual add search keeps name lookup and skips a half-typed number`() = runTest {
        val pikachu = TcgDexCard(id = "base1-58", localId = "58", name = "Pikachu", image = null, rarity = null, category = "Pokemon")
        coEvery { tcgDexApi.searchCards("pikachu*") } returns listOf(pikachu)

        assertEquals(listOf("base1-58"), repository.searchCardsForManualAdd("pikachu").map { it.id })
        assertTrue(repository.searchCardsForManualAdd("190/").isEmpty())
        coVerify(exactly = 0) { tcgDexApi.searchCardsByLocalId(any()) }
        coVerify(exactly = 0) { tcgDexApi.searchCards("190/*") }
    }
}
