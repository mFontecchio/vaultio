package com.mrhayami.vaultio.ui.grading

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.CardGradeEntity
import com.mrhayami.vaultio.data.repository.GeminiNanoClient

/** UI State for the Grading Screen/Mode */
@Immutable
data class GradingViewState(
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val gradeResult: CardGradeEntity? = null,
    val capturedImage: Bitmap? = null,
    val errorMessage: String? = null,
    val showModelDownloadPrompt: Boolean = false,
    val modelStatus: GeminiNanoClient.ModelStatus = GeminiNanoClient.ModelStatus.Unavailable,
    val pendingCard: com.mrhayami.vaultio.data.remote.TcgDexCard? = null,
    val folders: List<com.mrhayami.vaultio.data.local.FolderEntity> = emptyList(),
)

/** User actions for Grading */
sealed interface GradingEvent {
    data class StartAnalysis(val image: Bitmap, val userCardId: Long) : GradingEvent
    data class SaveGrade(val userCardId: Long) : GradingEvent
    data class SaveGradeWithMetadata(
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String,
        val folderIds: List<Long>,
    ) : GradingEvent

    data object Reset : GradingEvent
    data object DownloadModel : GradingEvent
}

/** One-time effects for Grading */
sealed interface GradingEffect {
    data class ShowToast(val message: String) : GradingEffect
    sealed interface Navigation : GradingEffect {
        data object GoBack : Navigation
        data class GoToResult(val gradeId: Long) : Navigation
    }
}
