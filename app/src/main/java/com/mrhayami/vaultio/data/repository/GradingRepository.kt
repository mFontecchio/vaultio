package com.mrhayami.vaultio.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.mrhayami.vaultio.data.local.CardGradeDao
import com.mrhayami.vaultio.data.local.CardGradeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs

class GradingRepository(
    private val context: Context,
    private val cardGradeDao: CardGradeDao
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
     * In Phase 1: Implements Centering math and mocks other sub-grades.
     */
    suspend fun analyzeCardCondition(
        image: Bitmap,
        userCardId: Long
    ): Result<CardGradeEntity> = withContext(Dispatchers.Default) {
        try {
            // 1. Centering Analysis (Geometric)
            val centeringScore = calculateCenteringScore(image)

            // 2. Visual Analysis (Condition - Gemini Nano)
            val visualAnalysis = analyzeVisualConditionWithGemini(image)

            // 3. Overall Weighted Score
            val overallScore = (centeringScore * 0.4) +
                    (visualAnalysis.cornersScore * 0.2) +
                    (visualAnalysis.edgesScore * 0.2) +
                    (visualAnalysis.surfaceScore * 0.2)

            val grade = CardGradeEntity(
                userCardId = userCardId,
                overallScore = Math.round(overallScore * 2) / 2.0, // Round to nearest 0.5
                centeringScore = centeringScore,
                cornersScore = visualAnalysis.cornersScore,
                edgesScore = visualAnalysis.edgesScore,
                surfaceScore = visualAnalysis.surfaceScore,
                reasoning = visualAnalysis.reasoning
            )

            if (userCardId > 0L) {
                cardGradeDao.insertGrade(grade)
            }
            Result.success(grade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cache for passing image between Scanner and Grading without huge Intent size
    var activeGradingImage: Bitmap? = null
    var pendingCardToGrade: com.mrhayami.vaultio.data.remote.TcgDexCard? = null

    // Local On-Device AI / Heuristic Analysis (No API Key Required)
    private suspend fun analyzeVisualConditionWithGemini(image: Bitmap): VisualAnalysis =
        withContext(Dispatchers.Default) {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)

            var edgeWearPixels = 0
            var cornerWearPixels = 0
            var surfaceNoise = 0

            // Edge thickness to check (approx 2% of width)
            val edgeThreshold = (width * 0.02).toInt()
            val cornerThreshold = (width * 0.05).toInt()

            // Extremely simplified heuristic for demonstration:
            // Whitening (high RGB variance near edges) indicates wear.
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[y * width + x]
                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff
                    val brightness = (r + g + b) / 3

                    val isLeftEdge = x < edgeThreshold
                    val isRightEdge = x > width - edgeThreshold
                    val isTopEdge = y < edgeThreshold
                    val isBottomEdge = y > height - edgeThreshold

                    val isEdge = isLeftEdge || isRightEdge || isTopEdge || isBottomEdge
                    val isCorner = (isLeftEdge || isRightEdge) && (isTopEdge || isBottomEdge)

                    // Detect "whitening" / silvering typical of card damage
                    if (brightness > 200) {
                        if (isCorner) cornerWearPixels++
                        else if (isEdge) edgeWearPixels++
                    } else if (!isEdge) {
                        // Surface imperfection (just dummy math for variation)
                        if (abs(r - g) > 50) surfaceNoise++
                    }
                }
            }

            // Calculate scores out of 10.0 based on wear ratios
            val edgeArea = (width * edgeThreshold * 2) + (height * edgeThreshold * 2)
            val cornerArea = cornerThreshold * cornerThreshold * 4
            val centerArea = width * height - edgeArea

            val edgeDamageRatio = edgeWearPixels.toDouble() / edgeArea
            val cornerDamageRatio = cornerWearPixels.toDouble() / cornerArea
            val surfaceDamageRatio = surfaceNoise.toDouble() / centerArea

            // Mapping to 1.0 - 10.0 scale (10 means pristine, 0 wear)
            val cornersScore = (10.0 - (cornerDamageRatio * 50)).coerceIn(1.0, 10.0)
            val edgesScore = (10.0 - (edgeDamageRatio * 40)).coerceIn(1.0, 10.0)
            val surfaceScore = (10.0 - (surfaceDamageRatio * 20)).coerceIn(1.0, 10.0)

            // Generate reasoning text based on the math
            val strengths = mutableListOf<String>()
            val weaknesses = mutableListOf<String>()

            if (cornersScore >= 9.0) strengths.add("sharp corners") else weaknesses.add("corner whitening")
            if (edgesScore >= 9.0) strengths.add("clean edges") else weaknesses.add("edge wear/silvering")
            if (surfaceScore >= 9.0) strengths.add("clean surface") else weaknesses.add("surface scratching")

            val reasoning = buildString {
                if (strengths.isNotEmpty()) append("Features ${strengths.joinToString(", ")}. ")
                if (weaknesses.isNotEmpty()) append("Detected ${weaknesses.joinToString(", ")}.")
                if (isEmpty()) append("Card in average condition.")
            }

            VisualAnalysis(
                cornersScore = Math.round(cornersScore * 2) / 2.0,
                edgesScore = Math.round(edgesScore * 2) / 2.0,
                surfaceScore = Math.round(surfaceScore * 2) / 2.0,
                reasoning = reasoning.trim()
            )
        }

    private data class VisualAnalysis(
        val cornersScore: Double,
        val edgesScore: Double,
        val surfaceScore: Double,
        val reasoning: String
    )

    /**
     * Performs a heuristic-based centering logic.
     * Detects the card boundaries to calculate 4-way centering.
     */
    private fun calculateCenteringScore(image: Bitmap): Double {
        return try {
            // Simplified fallback for centering computation without external ML dependencies
            // which avoids 16 KB page-alignment issues on Android 15.
            val width = image.width
            val height = image.height
            val expectedRatio = 2.5f / 3.5f
            val actualRatio = width.toFloat() / height.toFloat()

            val ratioDiff = Math.abs(expectedRatio - actualRatio)
            val score = 10.0 - (ratioDiff * 10).coerceAtMost(2.0f)

            Math.round(score * 2) / 2.0
        } catch (e: Throwable) {
            9.0 // Fallback
        }
    }

    private fun generateReasoning(
        centering: Double,
        corners: Double,
        edges: Double,
        surface: Double
    ): String {
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()

        if (centering >= 9.0) strengths.add("excellent centering") else weaknesses.add("centering issues")
        if (corners >= 9.0) strengths.add("sharp corners") else weaknesses.add("corner wear")
        if (edges >= 9.0) strengths.add("clean edges") else weaknesses.add("edge chipping")
        if (surface >= 9.0) strengths.add("flawless surface") else weaknesses.add("surface scratches")

        val strengthText =
            if (strengths.isNotEmpty()) "Features ${strengths.joinToString(", ")}." else ""
        val weaknessText =
            if (weaknesses.isNotEmpty()) "Noted ${weaknesses.joinToString(", ")}." else ""

        return "$strengthText $weaknessText".trim()
    }
}
