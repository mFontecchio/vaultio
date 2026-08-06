package com.mrhayami.vaultio.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiNanoClientImpl : GeminiNanoClient {

    private var clientInstance: GenerativeModel? = null
    private var lastTotalBytes: Long = 0

    private fun getClient(): GenerativeModel {
        return clientInstance ?: synchronized(this) {
            val instance = Generation.getClient(
                generationConfig {
                    modelConfig = modelConfig {
                        releaseStage = ModelReleaseStage.STABLE
                        preference = ModelPreference.FULL
                    }
                }
            )
            clientInstance = instance
            instance
        }
    }

    private fun resetClient() {
        clientInstance = null
    }

    override fun getModelStatus(): Flow<GeminiNanoClient.ModelStatus> = flow {
        try {
            val status = getClient().checkStatus()
            when (status) {
                FeatureStatus.AVAILABLE -> emit(GeminiNanoClient.ModelStatus.Ready)
                FeatureStatus.DOWNLOADABLE -> emit(GeminiNanoClient.ModelStatus.Unavailable)
                FeatureStatus.DOWNLOADING -> emit(GeminiNanoClient.ModelStatus.Downloading(0f))
                FeatureStatus.UNAVAILABLE -> emit(GeminiNanoClient.ModelStatus.Unavailable)
                else -> emit(GeminiNanoClient.ModelStatus.Unavailable)
            }
        } catch (e: Exception) {
            emit(GeminiNanoClient.ModelStatus.Error(e.message ?: "Unknown error"))
        }
    }

    override fun downloadModel(): Flow<GeminiNanoClient.ModelStatus> = flow {
        try {
            // Force a re-initialization on download/retry to clear any "unavailable" cache
            resetClient()
            val client = getClient()

            val status = client.checkStatus()
            if (status == FeatureStatus.AVAILABLE) {
                emit(GeminiNanoClient.ModelStatus.Ready)
                return@flow
            }

            client.download().collect { downloadStatus ->
                when (downloadStatus) {
                    is DownloadStatus.DownloadStarted -> {
                        lastTotalBytes = downloadStatus.bytesToDownload
                        emit(GeminiNanoClient.ModelStatus.Downloading(0f))
                    }

                    is DownloadStatus.DownloadProgress -> {
                        val total = lastTotalBytes
                        val progress = if (total > 0) {
                            downloadStatus.totalBytesDownloaded.toFloat() / total
                        } else 0f
                        emit(GeminiNanoClient.ModelStatus.Downloading(progress))
                    }

                    is DownloadStatus.DownloadCompleted -> {
                        emit(GeminiNanoClient.ModelStatus.Ready)
                    }

                    is DownloadStatus.DownloadFailed -> {
                        emit(GeminiNanoClient.ModelStatus.Error("Download failed: ${downloadStatus.e.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            emit(GeminiNanoClient.ModelStatus.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun generateFromImage(image: Bitmap, prompt: String): Result<String> {
        return try {
            val client = getClient()
            val status = client.checkStatus()
            Log.d("GeminiNano", "Checking status before generation: $status")

            if (status != FeatureStatus.AVAILABLE) {
                Log.w("GeminiNano", "Feature not available: $status")
                Result.failure(IllegalStateException("Gemini Nano model is not ready (Status: $status)"))
            } else {
                // Defensive Copy: Create a copy for the AI to use, 
                // in case the SDK recycles it during inference.
                val aiBitmap = image.copy(image.config ?: Bitmap.Config.ARGB_8888, false)

                val request = generateContentRequest(
                    ImagePart(aiBitmap),
                    TextPart(prompt)
                ) {
                    temperature = 0.2f
                    maxOutputTokens = 256
                }

                Log.d("GeminiNano", "Sending request to Gemini Nano...")
                val response = client.generateContent(request)
                Log.d("GeminiNano", "Response received from Gemini Nano")

                val text = response.candidates.firstOrNull()?.text

                if (text != null) {
                    Log.d("GeminiNano", "Generation successful. Text length: ${text.length}")
                    Result.success(text)
                } else {
                    Log.w("GeminiNano", "AI returned empty response")
                    Result.failure(Exception("AI returned empty response"))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiNano", "Error during generation", e)
            Result.failure(e)
        }
    }
}
