package com.mrhayami.vaultio.data.repository

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Gemini Nano on-device AI operations.
 * Following 2026 standards for AI integration.
 */
interface GeminiNanoClient {
    /**
     * Checks the status of the on-device model (downloaded, ready, etc.)
     */
    fun getModelStatus(): Flow<ModelStatus>

    /**
     * Generates a response from a multimodal prompt (text + image).
     */
    suspend fun generateFromImage(image: Bitmap, prompt: String): Result<String>

    sealed class ModelStatus {
        object Ready : ModelStatus()
        data class Downloading(val progress: Float) : ModelStatus()
        object Unavailable : ModelStatus()
        data class Error(val message: String) : ModelStatus()
    }

    /**
     * Triggers the download of the on-device model.
     */
    fun downloadModel(): Flow<ModelStatus>
}
