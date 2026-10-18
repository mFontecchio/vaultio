package com.mrhayami.vaultio.ui.scanner

import com.mrhayami.vaultio.data.repository.VaultioRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerMatchingTest {

    private val repository: VaultioRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setup() {
        viewModel = ScannerViewModel(repository, testDispatcher)
        assertNotNull(viewModel)
    }

    @Test
    fun `regex prioritized matching - number total`() = testScope.runTest {
        val parsed = CardTextParser.parseLines(
            listOf(OcrLine("123 / 191", topFraction = 0.94f, centerYFraction = 0.96f))
        )
        assertEquals("123", parsed.number)
        assertEquals("191", parsed.total)
        assertEquals(CardEra.MODERN, parsed.era)
    }

    @Test
    fun `regex prioritized matching - set prefix`() = testScope.runTest {
        val parsed = CardTextParser.parseLines(
            listOf(OcrLine("SWSH 123", topFraction = 0.94f, centerYFraction = 0.96f))
        )
        assertEquals("SWSH123", parsed.number)
        assertNull(parsed.total)
        assertEquals(CardEra.MODERN, parsed.era)
    }
}
