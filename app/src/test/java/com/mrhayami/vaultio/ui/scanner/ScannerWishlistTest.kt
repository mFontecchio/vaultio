package com.mrhayami.vaultio.ui.scanner

import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerWishlistTest {

    private val repository = mockk<VaultioRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ScannerViewModel

    private val sampleCard = TcgDexCard(
        id = "sv3-1",
        localId = "1",
        name = "Pikachu",
        image = null,
        rarity = "Common",
        category = "Pokemon"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { repository.userPreferencesRepository } returns userPreferencesRepository
        every { repository.allFolders } returns MutableStateFlow(emptyList<FolderEntity>())
        every { repository.observeCatalogCardCount() } returns MutableStateFlow(10)
        every { userPreferencesRepository.bulkScanCondition } returns
            MutableStateFlow(PricingUtils.CONDITION_NM)
        every { userPreferencesRepository.bulkScanPrinting } returns
            MutableStateFlow(PricingUtils.PRINTING_UNLIMITED)
        every { userPreferencesRepository.bulkScanFinish } returns
            MutableStateFlow(PricingUtils.FINISH_NORMAL)
        every { userPreferencesRepository.bulkScanFolderIds } returns MutableStateFlow(emptyList())

        viewModel = ScannerViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addScannedToWishlist_success_setsMessageAndResumes() = runTest(testDispatcher) {
        coEvery { repository.addCardToWishlist(any(), any()) } returns 1L

        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onEvent(ScannerEvent.CardSelected(sampleCard))
        viewModel.onEvent(
            ScannerEvent.AddScannedToWishlist(
                card = sampleCard,
                quantity = 2,
                condition = PricingUtils.CONDITION_NM,
                printing = PricingUtils.PRINTING_UNLIMITED,
                finish = PricingUtils.FINISH_NORMAL
            )
        )
        advanceUntilIdle()

        val latest = viewModel.uiState.value
        assertEquals("Added to wishlist", latest.successMessage)
        assertNull(latest.selectedCard)
        assertNull(latest.errorMessage)
        assertFalse(latest.isSaving)
        coVerify(exactly = 1) { repository.addCardToWishlist(sampleCard, any()) }
    }

    @Test
    fun addScannedToWishlist_failure_setsErrorMessage() = runTest(testDispatcher) {
        coEvery { repository.addCardToWishlist(any(), any()) } throws RuntimeException("db down")

        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onEvent(
            ScannerEvent.AddScannedToWishlist(
                card = sampleCard,
                quantity = 1,
                condition = PricingUtils.CONDITION_NM,
                printing = PricingUtils.PRINTING_UNLIMITED,
                finish = PricingUtils.FINISH_NORMAL
            )
        )
        advanceUntilIdle()

        val latest = viewModel.uiState.value
        assertNull(latest.successMessage)
        assertTrue(latest.errorMessage?.contains("Failed to add to wishlist") == true)
        assertTrue(latest.errorMessage?.contains("db down") == true)
        assertFalse(latest.isSaving)
    }

    @Test
    fun addScannedToWishlist_doubleSubmit_callsRepositoryOnce() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.addCardToWishlist(any(), any()) } coAnswers {
            gate.await()
            1L
        }

        backgroundScope.launch { viewModel.uiState.collect { } }

        val event = ScannerEvent.AddScannedToWishlist(
            card = sampleCard,
            quantity = 1,
            condition = PricingUtils.CONDITION_NM,
            printing = PricingUtils.PRINTING_UNLIMITED,
            finish = PricingUtils.FINISH_NORMAL
        )

        viewModel.onEvent(event)
        viewModel.onEvent(event)

        gate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addCardToWishlist(any(), any()) }
    }
}
