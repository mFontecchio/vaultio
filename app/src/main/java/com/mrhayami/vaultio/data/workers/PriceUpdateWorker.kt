package com.mrhayami.vaultio.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.flow.first

/**
 * Worker responsible for updating prices for all cards in the user's collection.
 * 
 * Uses the robust dual-source pricing strategy:
 * 1. TCGdex as primary source for modern/standard cards.
 * 2. JustTCG as fallback and for vintage edition-specific pricing.
 * 3. Efficient batching for JustTCG to minimize API usage.
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
            // 1. Get all cards that exist in the user's collection
            // We use the distinct set of CardEntity from the collection
            val cardWithDetailsList = repository.allUserCards.first()
            if (cardWithDetailsList.isEmpty()) {
                Log.d("PriceUpdateWorker", "No cards found in collection. Skipping update.")
                return Result.success()
            }

            // Get unique cards to avoid duplicate API calls for the same card in different folders/quantities
            val uniqueCards = cardWithDetailsList.map { it.card }.distinctBy { it.id }
            
            Log.d("PriceUpdateWorker", "Found ${uniqueCards.size} unique cards to refresh.")

            // 2. Delegate to repository's batch update logic
            // This handles vintage vs modern partitioning and JustTCG batching
            repository.updatePricesBatch(uniqueCards)

            val duration = System.currentTimeMillis() - startTime
            Log.d("PriceUpdateWorker", "Successfully finished price update. Processed ${uniqueCards.size} cards. Time taken: ${duration}ms")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceUpdateWorker", "Critical failure during price update", e)
            Result.failure()
        }
    }
}
