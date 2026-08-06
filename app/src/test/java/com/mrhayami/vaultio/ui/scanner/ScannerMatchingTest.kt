package com.mrhayami.vaultio.ui.scanner

import com.mrhayami.vaultio.data.repository.VaultioRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    }

    @Test
    fun `regex prioritized matching - number total`() = testScope.runTest {
        val numberTotalRegex = Regex("""([A-Z0-9]{1,5})\s*/\s*([A-Z0-9]{1,5})""")

        val match = numberTotalRegex.find("123 / 191")
        assertNotNull(match)
        assertEquals("123", match!!.groupValues[1])
        assertEquals("191", match!!.groupValues[2])
    }

    @Test
    fun `regex prioritized matching - set prefix`() = testScope.runTest {
        val setNumberRegex = Regex("""([A-Z]{1,4})\s*(\d{1,4})""")

        val match = setNumberRegex.find("SWSH 123")
        assertNotNull(match)
        assertEquals("SWSH", match!!.groupValues[1])
        assertEquals("123", match!!.groupValues[2])
    }
}
