package com.mrhayami.vaultio.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mrhayami.vaultio.BuildConfig
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Worker responsible for updating prices for all cards in the user's collection.
 * 
 * Optimized to:
 * 1. Use direct API lookups instead of search queries.
 * 2. Process cards in parallel chunks to reduce execution time.
 * 3. Batch database transactions to minimize disk I/O and CPU overhead.
 * 4. Improved error handling and logging for performance monitoring.
 */
class PriceUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository: VaultioRepository = (applicationContext as VaultioApplication).repository

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d("PriceUpdateWorker", "Starting scheduled price update task...")

        return try {
            // Fetch all cards once to avoid multiple Flow emissions during processing
            val userCards = repository.allUserCards.first()
            if (userCards.isEmpty()) {
                Log.d("PriceUpdateWorker", "No cards found in collection. Skipping update.")
                return Result.success()
            }

            val allPricesToUpdate = mutableListOf<PriceEntity>()
            
            // Process cards in chunks to balance parallelism and resource usage (memory/network)
            // Parallelism significantly reduces the total time compared to sequential execution.
            val chunkSize = 5 
            coroutineScope {
                userCards.chunked(chunkSize).forEach { chunk ->
                    val deferredResults = chunk.map { cardWithDetails ->
                        async {
                            val card = cardWithDetails.card
                            val cardPrices = mutableListOf<PriceEntity>()

                            // 1. TCGdex Update - Using getCardDetail is O(1) compared to search queries
                            val tcgDexCard = repository.getCardDetail(card.id)
                            
                            if (tcgDexCard?.tcgplayer?.prices != null) {
                                val prices = tcgDexCard.tcgplayer.prices
                                // Standard mapping
                                cardPrices.add(PriceEntity(
                                    cardId = card.id,
                                    finish = "Standard",
                                    condition = "Near Mint",
                                    marketPrice = prices.market,
                                    lowPrice = prices.low,
                                    midPrice = prices.average,
                                    highPrice = prices.high,
                                    source = "tcgdex"
                                ))
                                
                                // Capture Holo/Reverse Holo if available to maximize data utility per API call
                                if (prices.reverseHoloMarket != null) {
                                    cardPrices.add(PriceEntity(
                                        cardId = card.id,
                                        finish = "Reverse Holo",
                                        condition = "Near Mint",
                                        marketPrice = prices.reverseHoloMarket,
                                        lowPrice = prices.reverseHoloLow,
                                        midPrice = prices.reverseHoloAvg,
                                        highPrice = prices.reverseHoloHigh,
                                        source = "tcgdex"
                                    ))
                                }
                            }

                            // 2. JustTCG Fallback/Supplement
                            val tcgPlayerId = card.tcgPlayerId
                            if (tcgPlayerId != null && repository.getApiUsage() < 100) {
                                try {
                                    val justTcgResponse = repository.justTcgApi.getCardByTcgPlayerId(
                                        apiKey = BuildConfig.JUST_TCG_API_KEY,
                                        tcgplayerId = tcgPlayerId
                                    ).data

                                    justTcgResponse.firstOrNull()?.variants?.forEach { variant ->
                                        cardPrices.add(PriceEntity(
                                            cardId = card.id,
                                            finish = variant.printing,
                                            condition = variant.condition,
                                            marketPrice = variant.prices?.market,
                                            lowPrice = variant.prices?.low,
                                            midPrice = variant.prices?.mid,
                                            highPrice = variant.prices?.high,
                                            source = "justtcg"
                                        ))
                                    }
                                    repository.incrementApiUsage()
                                } catch (e: Exception) {
                                    Log.w("PriceUpdateWorker", "Failed to fetch JustTCG prices for card ${card.id}: ${e.message}")
                                }
                            }
                            cardPrices
                        }
                    }
                    allPricesToUpdate.addAll(deferredResults.awaitAll().flatten())
                }
            }

            // Optimization: Batch database update. Performing one large transaction is significantly 
            // faster than multiple individual inserts, reducing CPU and battery usage.
            repository.updatePrices(allPricesToUpdate)

            val duration = System.currentTimeMillis() - startTime
            Log.d("PriceUpdateWorker", "Successfully finished price update. Processed ${userCards.size} cards. Total price entries updated: ${allPricesToUpdate.size}. Time taken: ${duration}ms")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceUpdateWorker", "Critical failure during price update", e)
            Result.failure()
        }
    }
}
