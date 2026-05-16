package com.mrhayami.vaultio.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.mrhayami.vaultio.data.local.CardGradeDao
import com.mrhayami.vaultio.data.local.CardGradeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GradingRepository(
    private val context: Context,
    private val cardGradeDao: CardGradeDao,
    val geminiNanoClient: GeminiNanoClient
) {
    /**
     * Get the latest grade for a specific user card.
     */
    fun getGradeForUserCard(userCardId: Long): Flow<CardGradeEntity?> {
        return cardGradeDao.getGradeForUserCard(userCardId)
    }

    suspend fun insertGrade(grade: CardGradeEntity) = withContext(Dispatchers.IO) {
        cardGradeDao.insertGrade(grade)
    }

    /**
     * Performs AI analysis on a card image.
     * Uses Gemini Nano on supported devices, falls back to Heuristics on Emulators/Old devices.
     */
    suspend fun analyzeCardCondition(
        image: Bitmap,
        userCardId: Long
    ): Result<CardGradeEntity> = withContext(Dispatchers.Default) {
        try {
            Log.d("VaultioGrading", "Starting analysis. userCardId=$userCardId")

            if (isEmulator()) {
                Log.d("VaultioGrading", "Emulator detected. Falling back to heuristics.")
                analyzeWithHeuristics(image, userCardId, "Emulator detected")
            } else {
                Log.d("VaultioGrading", "Physical device. Attempting Gemini analysis...")
                val geminiResult = analyzeWithGemini(image, userCardId)
                if (geminiResult.isFailure) {
                    val error = geminiResult.exceptionOrNull()?.message ?: "Unknown AI error"
                    Log.d(
                        "VaultioGrading",
                        "Gemini analysis failed: $error. Falling back to heuristics."
                    )
                    // Fallback to heuristics if AI fails (e.g. model not ready/unsupported)
                    analyzeWithHeuristics(image, userCardId, error)
                } else {
                    Log.d("VaultioGrading", "Gemini analysis successful!")
                    geminiResult
                }
            }
        } catch (e: Exception) {
            Log.e("VaultioGrading", "Analysis crashed", e)
            Result.failure(e)
        }
    }

    private suspend fun analyzeWithGemini(
        image: Bitmap,
        userCardId: Long
    ): Result<CardGradeEntity> {
        val prompt = """
            Analyze this Trading Card image for condition. 
            Provide scores from 1.0 to 10.0 (increments of 0.5) for: 
            - Centering
            - Corners
            - Edges
            - Surface
            
            Also provide a concise reasoning for each score.
            Format your response as a simple list.
        """.trimIndent()

        val aiResult = geminiNanoClient.generateFromImage(image, prompt)

        return aiResult.map { text ->
            val analysis = parseAiResponse(text)

            val overallScore = calculateOverall(analysis)

            val grade = CardGradeEntity(
                userCardId = userCardId,
                overallScore = Math.round(overallScore * 2) / 2.0,
                centeringScore = analysis.centeringScore,
                cornersScore = analysis.cornersScore,
                edgesScore = analysis.edgesScore,
                surfaceScore = analysis.surfaceScore,
                reasoning = analysis.reasoning
            )

            if (userCardId > 0L) {
                cardGradeDao.insertGrade(grade)
            }
            grade
        }
    }

    private fun analyzeWithHeuristics(
        image: Bitmap,
        userCardId: Long,
        reason: String? = null
    ): Result<CardGradeEntity> {
        val centering = calculateCenteringScore(image)
        val visual = runLegacyHeuristics(image)

        val overallScore =
            (centering * 0.4) + (visual.cornersScore * 0.2) + (visual.edgesScore * 0.2) + (visual.surfaceScore * 0.2)

        val fallbackReason =
            if (reason != null) "[Heuristic Fallback: $reason]" else "[Heuristic Fallback]"

        val grade = CardGradeEntity(
            userCardId = userCardId,
            overallScore = Math.round(overallScore * 2) / 2.0,
            centeringScore = centering,
            cornersScore = visual.cornersScore,
            edgesScore = visual.edgesScore,
            surfaceScore = visual.surfaceScore,
            reasoning = "$fallbackReason ${visual.reasoning}"
        )

        return Result.success(grade)
    }

    private fun isEmulator(): Boolean {
        val model = Build.MODEL
        val product = Build.PRODUCT
        val hardware = Build.HARDWARE
        val brand = Build.BRAND

        // Stricter emulator check to avoid false positives on real devices
        return (brand.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
    }

    private fun calculateOverall(analysis: VisualAnalysis): Double {
        return (analysis.centeringScore * 0.4) +
                (analysis.cornersScore * 0.2) +
                (analysis.edgesScore * 0.2) +
                (analysis.surfaceScore * 0.2)
    }

    // Cache for passing image between Scanner and Grading
    var activeGradingImage: Bitmap? = null
    var pendingCardToGrade: com.mrhayami.vaultio.data.remote.TcgDexCard? = null

    private fun parseAiResponse(text: String): VisualAnalysis {
        val centering = extractScore(text, "Centering") ?: 9.0
        val corners = extractScore(text, "Corners") ?: 9.0
        val edges = extractScore(text, "Edges") ?: 9.0
        val surface = extractScore(text, "Surface") ?: 9.0

        return VisualAnalysis(
            centeringScore = centering,
            cornersScore = corners,
            edgesScore = edges,
            surfaceScore = surface,
            reasoning = text.take(500)
        )
    }

    private fun extractScore(text: String, key: String): Double? {
        val regex = Regex("$key.*?(\\d+(\\.\\d+)?)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun calculateCenteringScore(image: Bitmap): Double {
        val width = image.width
        val height = image.height
        val expectedRatio = 2.5f / 3.5f
        val actualRatio = width.toFloat() / height.toFloat()
        val ratioDiff = Math.abs(expectedRatio - actualRatio)
        val score = 10.0 - (ratioDiff * 10).coerceAtMost(2.0f)
        return Math.round(score * 2) / 2.0
    }

    private fun runLegacyHeuristics(image: Bitmap): VisualAnalysis {
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        var edgeWear = 0
        var cornerWear = 0
        val edgeThreshold = (width * 0.02).toInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val brightness =
                    (((pixel shr 16) and 0xff) + ((pixel shr 8) and 0xff) + (pixel and 0xff)) / 3
                if (brightness > 210) {
                    if (x < edgeThreshold || x > width - edgeThreshold || y < edgeThreshold || y > height - edgeThreshold) {
                        edgeWear++
                        if ((x < edgeThreshold || x > width - edgeThreshold) && (y < edgeThreshold || y > height - edgeThreshold)) {
                            cornerWear++
                        }
                    }
                }
            }
        }

        val cornersScore =
            (10.0 - (cornerWear.toDouble() / (edgeThreshold * edgeThreshold * 4) * 50)).coerceIn(
                1.0,
                10.0
            )
        val edgesScore =
            (10.0 - (edgeWear.toDouble() / (width * height * 0.08) * 40)).coerceIn(1.0, 10.0)

        return VisualAnalysis(
            centeringScore = 0.0, // Calculated separately
            cornersScore = Math.round(cornersScore * 2) / 2.0,
            edgesScore = Math.round(edgesScore * 2) / 2.0,
            surfaceScore = 9.0, // Heuristic can't easily detect scratches
            reasoning = "Heuristic detection based on edge whitening."
        )
    }

    private data class VisualAnalysis(
        val centeringScore: Double,
        val cornersScore: Double,
        val edgesScore: Double,
        val surfaceScore: Double,
        val reasoning: String
    )
}
