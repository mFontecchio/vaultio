package com.mrhayami.vaultio.ui.grading

import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.repository.GradingRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.coroutines.launch

class GradingViewModel(
    private val gradingRepository: GradingRepository,
    private val vaultioRepository: VaultioRepository
) : MviViewModel<GradingViewState, GradingEvent, GradingEffect>(
    initialState = GradingViewState(
        capturedImage = gradingRepository.activeGradingImage,
        pendingCard = gradingRepository.pendingCardToGrade
    )
) {
    init {
        viewModelScope.launch {
            vaultioRepository.allFolders.collect { folders ->
                updateState { copy(folders = folders) }
            }
        }
    }

    override fun onEvent(event: GradingEvent) {
        when (event) {
            is GradingEvent.StartAnalysis -> startGrading(event)
            GradingEvent.Reset -> updateState {
                GradingViewState(
                    capturedImage = gradingRepository.activeGradingImage,
                    pendingCard = gradingRepository.pendingCardToGrade,
                    folders = folders
                )
            }

            GradingEvent.DownloadModel -> {
                // Placeholder for model download logic in Phase 2
                updateState { copy(showModelDownloadPrompt = false) }
            }

            is GradingEvent.SaveGrade -> saveGrade(event.userCardId)
            is GradingEvent.SaveGradeWithMetadata -> saveGradeWithMetadata(event)
        }
    }

    private fun saveGradeWithMetadata(event: GradingEvent.SaveGradeWithMetadata) {
        viewModelScope.launch {
            val gradeResult = state.value.gradeResult ?: return@launch
            val pendingCard = state.value.pendingCard ?: return@launch

            try {
                val finalUserCardId = vaultioRepository.addUserCard(
                    pendingCard,
                    com.mrhayami.vaultio.data.local.UserCardEntity(
                        cardId = pendingCard.id,
                        quantity = event.quantity,
                        condition = event.condition,
                        printing = event.printing,
                        finish = event.finish
                    ),
                    event.folderIds
                )

                if (finalUserCardId != -1L) {
                    val entityToSave = gradeResult.copy(userCardId = finalUserCardId)
                    gradingRepository.insertGrade(entityToSave)

                    gradingRepository.pendingCardToGrade = null
                    emitEffect(GradingEffect.ShowToast("Grade saved successfully!"))
                    emitEffect(GradingEffect.Navigation.GoBack)
                } else {
                    emitEffect(GradingEffect.ShowToast("Error: No card data to save."))
                }
            } catch (e: Exception) {
                emitEffect(GradingEffect.ShowToast("Failed to save: ${e.message}"))
            }
        }
    }

    private fun saveGrade(userCardId: Long) {
        viewModelScope.launch {
            val gradeResult = state.value.gradeResult ?: return@launch

            try {
                val score = gradeResult.overallScore
                val mappedCondition = when {
                    score >= 8.5 -> com.mrhayami.vaultio.data.PricingUtils.CONDITION_NM
                    score >= 7.0 -> com.mrhayami.vaultio.data.PricingUtils.CONDITION_LP
                    score >= 5.0 -> com.mrhayami.vaultio.data.PricingUtils.CONDITION_MP
                    score >= 3.0 -> com.mrhayami.vaultio.data.PricingUtils.CONDITION_HP
                    else -> com.mrhayami.vaultio.data.PricingUtils.CONDITION_DMG
                }

                val finalUserCardId = if (userCardId == -1L) {
                    val pendingCard = gradingRepository.pendingCardToGrade
                    if (pendingCard != null) {
                        vaultioRepository.addUserCard(
                            pendingCard,
                            com.mrhayami.vaultio.data.local.UserCardEntity(
                                cardId = pendingCard.id,
                                quantity = 1,
                                condition = mappedCondition,
                                printing = com.mrhayami.vaultio.data.PricingUtils.PRINTING_UNLIMITED,
                                finish = com.mrhayami.vaultio.data.PricingUtils.FINISH_NORMAL
                            ),
                            emptyList()
                        )
                    } else {
                        -1L
                    }
                } else {
                    userCardId
                }

                if (finalUserCardId != -1L) {
                    val entityToSave = gradeResult.copy(userCardId = finalUserCardId)
                    gradingRepository.insertGrade(entityToSave)

                    if (userCardId != -1L) {
                        try {
                            vaultioRepository.getUserCardByIdSync(finalUserCardId)
                                ?.let { cardWithDetails ->
                                    vaultioRepository.updateUserCard(
                                        cardWithDetails.userCard.copy(
                                            condition = mappedCondition
                                        )
                                    )
                                }
                        } catch (e: Exception) {
                            // Optionally log
                        }
                    }

                    gradingRepository.pendingCardToGrade = null
                    emitEffect(GradingEffect.ShowToast("Grade saved successfully!"))
                    emitEffect(GradingEffect.Navigation.GoBack)
                } else {
                    emitEffect(GradingEffect.ShowToast("Error: No card data to save."))
                }
            } catch (e: Exception) {
                emitEffect(GradingEffect.ShowToast("Failed to save: ${e.message}"))
            }
        }
    }

    private fun startGrading(event: GradingEvent.StartAnalysis) {
        viewModelScope.launch {
            updateState {
                copy(
                    isAnalyzing = true,
                    capturedImage = event.image,
                    errorMessage = null
                )
            }

            gradingRepository.analyzeCardCondition(event.image, event.userCardId)
                .onSuccess { grade ->
                    updateState { copy(isAnalyzing = false, gradeResult = grade) }
                    emitEffect(GradingEffect.ShowToast("Analysis Complete!"))
                }
                .onFailure { error ->
                    updateState { copy(isAnalyzing = false, errorMessage = error.message) }
                    emitEffect(GradingEffect.ShowToast("Analysis Failed: ${error.message}"))
                }
        }
    }
}
