package com.mrhayami.vaultio.data.repository

import android.content.Context
import com.mrhayami.vaultio.data.local.CardGradeDao
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GradingRepositoryTest {

    private lateinit var repository: GradingRepository
    private val mockContext = mockk<Context>()
    private val mockDao = mockk<CardGradeDao>()
    private val mockClient = mockk<GeminiNanoClient>()

    @Before
    fun setup() {
        repository = GradingRepository(mockContext, mockDao, mockClient)
    }

    @Test
    fun `parseAiResponse extracts scores correctly from list format`() {
        val aiOutput = """
            Condition Analysis:
            - Centering: 9.5. The borders look very even.
            - Corners: 8.0. Slight whitening on bottom left.
            - Edges: 10.0. Perfect.
            - Surface: 7.5. Minor surface scratches detected.
        """.trimIndent()

        val method =
            GradingRepository::class.java.getDeclaredMethod("parseAiResponse", String::class.java)
        method.isAccessible = true
        val result = method.invoke(repository, aiOutput)

        // Using reflection to check internal data class
        val centering =
            result.javaClass.getDeclaredField("centeringScore").apply { isAccessible = true }
                .get(result) as Double
        val corners =
            result.javaClass.getDeclaredField("cornersScore").apply { isAccessible = true }
                .get(result) as Double
        val edges = result.javaClass.getDeclaredField("edgesScore").apply { isAccessible = true }
            .get(result) as Double
        val surface =
            result.javaClass.getDeclaredField("surfaceScore").apply { isAccessible = true }
                .get(result) as Double

        assertEquals(9.5, centering, 0.01)
        assertEquals(8.0, corners, 0.01)
        assertEquals(10.0, edges, 0.01)
        assertEquals(7.5, surface, 0.01)
    }

    @Test
    fun `parseAiResponse handles mixed formats`() {
        val aiOutput =
            "The card has Centering of 10, Corners: 9.0, Edges are 8.5 and Surface score is 6."

        val method =
            GradingRepository::class.java.getDeclaredMethod("parseAiResponse", String::class.java)
        method.isAccessible = true
        val result = method.invoke(repository, aiOutput)

        val centering =
            result.javaClass.getDeclaredField("centeringScore").apply { isAccessible = true }
                .get(result) as Double
        val corners =
            result.javaClass.getDeclaredField("cornersScore").apply { isAccessible = true }
                .get(result) as Double
        val edges = result.javaClass.getDeclaredField("edgesScore").apply { isAccessible = true }
            .get(result) as Double
        val surface =
            result.javaClass.getDeclaredField("surfaceScore").apply { isAccessible = true }
                .get(result) as Double

        assertEquals(10.0, centering, 0.01)
        assertEquals(9.0, corners, 0.01)
        assertEquals(8.5, edges, 0.01)
        assertEquals(6.0, surface, 0.01)
    }
}
