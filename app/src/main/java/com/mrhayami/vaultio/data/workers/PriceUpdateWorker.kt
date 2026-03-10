package com.mrhayami.vaultio.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mrhayami.vaultio.BuildConfig
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.flow.first

class PriceUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository: VaultioRepository = (applicationContext as VaultioApplication).repository

    override suspend fun doWork(): Result {
        return try {
            val userCards = repository.allUserCards.first()

            for (cardWithDetails in userCards) {
                val card = cardWithDetails.card

                // Try TCGdex first
                val tcgDexCard = repository.searchTcgDex(card.name).find { it.id == card.id } 
                    ?: repository.searchTcgDexByLocalId(card.localId).firstOrNull()

                if (tcgDexCard?.tcgplayer?.prices != null) {
                    val prices = tcgDexCard.tcgplayer.prices
                    // Map TCGdex prices to PriceEntity (simplified for now)
                    val priceEntity = PriceEntity(
                        cardId = card.id,
                        finish = "Standard",
                        condition = "Near Mint",
                        marketPrice = prices.market,
                        lowPrice = prices.low,
                        midPrice = prices.average,
                        highPrice = prices.high,
                        source = "tcgdex"
                    )
                    repository.updatePrice(priceEntity)
                } 
                
                // Fallback or Supplement with JustTCG
                val tcgPlayerId = card.tcgPlayerId
                if (tcgPlayerId != null && repository.getApiUsage() < 100) {
                    try {
                        val justTcgResponse = repository.justTcgApi.getCardByTcgPlayerId(
                            apiKey = BuildConfig.JUST_TCG_API_KEY,
                            tcgplayerId = tcgPlayerId
                        ).data

                        justTcgResponse.firstOrNull()?.variants?.forEach { variant ->
                            val priceEntity = PriceEntity(
                                cardId = card.id,
                                finish = variant.printing, // e.g., "Normal", "Reverse Holo"
                                condition = variant.condition, // e.g., "Near Mint"
                                marketPrice = variant.prices?.market,
                                lowPrice = variant.prices?.low,
                                midPrice = variant.prices?.mid,
                                highPrice = variant.prices?.high,
                                source = "justtcg"
                            )
                            repository.updatePrice(priceEntity)
                        }
                        repository.incrementApiUsage()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
