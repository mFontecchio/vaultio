package com.mrhayami.vaultio.ui.collection

import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderCardCrossRef
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.local.WishlistCardWithDetails
import com.mrhayami.vaultio.data.repository.VaultioRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val repository = mockk<VaultioRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CollectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Use MutableStateFlow instead of flowOf to ensure flows don't complete and always have a value
        every { userPreferencesRepository.viewMode } returns MutableStateFlow(ViewMode.GRID)
        every { userPreferencesRepository.sortMode } returns MutableStateFlow(SortMode.DATE_ADDED)
        every { userPreferencesRepository.listSettings } returns MutableStateFlow(ListSettings())
        every { userPreferencesRepository.gridSettings } returns MutableStateFlow(GridSettings())
        every { userPreferencesRepository.pokedexSettings } returns MutableStateFlow(PokedexSettings())
        every { userPreferencesRepository.preferSetLogo } returns MutableStateFlow(true)
        every { userPreferencesRepository.justTcgApiKey } returns MutableStateFlow("fake_key")
        every { userPreferencesRepository.lastSetCheck } returns MutableStateFlow(0L)

        every { repository.allSets } returns MutableStateFlow(emptyList<SetEntity>())
        every { repository.allUserCards } returns MutableStateFlow(emptyList<CardWithDetails>())
        every { repository.allFolders } returns MutableStateFlow(emptyList<FolderEntity>())
        every { repository.allFolderCardCrossRefs } returns MutableStateFlow(emptyList<FolderCardCrossRef>())
        every { repository.allPrices } returns MutableStateFlow(emptyList<PriceEntity>())
        every { repository.allVintagePrices } returns MutableStateFlow(emptyList<VintagePriceEntity>())
        every { repository.allWishlistCards } returns MutableStateFlow(emptyList<WishlistCardWithDetails>())

        coEvery { repository.getApiUsageDetails() } returns null

        viewModel = CollectionViewModel(repository, userPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ViewMode.GRID, viewModel.state.value.viewMode)
        assertEquals(false, viewModel.state.value.isSearchBarVisible)
    }

    @Test
    fun `OnToggleSearchBar event updates state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(CollectionEvent.OnToggleSearchBar)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isSearchBarVisible)
    }
}
