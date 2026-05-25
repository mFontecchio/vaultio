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
import com.mrhayami.vaultio.data.local.TelemetryDao
import com.mrhayami.vaultio.data.local.UserCardDao
import com.mrhayami.vaultio.data.local.WishlistDao
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.JustTcgCard
import com.mrhayami.vaultio.data.remote.JustTcgResponse
import com.mrhayami.vaultio.data.remote.TcgDexApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

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
            tcgDexApi, justTcgApi, userPreferencesRepository, imageLoader
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
}
