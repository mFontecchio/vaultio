package com.mrhayami.vaultio.data

import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.JustTcgPrice
import com.mrhayami.vaultio.data.remote.JustTcgVariant
import com.mrhayami.vaultio.data.remote.TcgDexPriceItem
import com.mrhayami.vaultio.data.remote.TcgDexTcgPlayerPricing

object PricingUtils {
    // Standardized Finishes
    const val FINISH_NORMAL = "Normal"
    const val FINISH_HOLOFOIL = "Holofoil"
    const val FINISH_REVERSE_HOLOFOIL = "Reverse Holofoil"

    // Standardized Conditions
    const val CONDITION_NM = "Near Mint"
    const val CONDITION_LP = "Lightly Played"
    const val CONDITION_MP = "Moderately Played"
    const val CONDITION_HP = "Heavily Played"
    const val CONDITION_DMG = "Damaged"

    fun mapTcgDexPrices(cardId: String, pricing: TcgDexTcgPlayerPricing): List<PriceEntity> {
        val result = mutableListOf<PriceEntity>()
        
        // Normal
        pricing.normal?.let { item ->
            if (hasAnyPrice(item)) {
                result.add(createPriceEntity(cardId, FINISH_NORMAL, item))
            }
        }
        
        // Holofoil
        pricing.holofoil?.let { item ->
            if (hasAnyPrice(item)) {
                result.add(createPriceEntity(cardId, FINISH_HOLOFOIL, item))
            }
        }

        // Reverse
        pricing.reverse?.let { item ->
            if (hasAnyPrice(item)) {
                result.add(createPriceEntity(cardId, FINISH_REVERSE_HOLOFOIL, item))
            }
        }

        return result
    }

    private fun hasAnyPrice(item: TcgDexPriceItem): Boolean {
        return item.resolveMarket() != null || item.resolveLow() != null || item.resolveMid() != null || item.resolveHigh() != null
    }

    private fun createPriceEntity(cardId: String, finish: String, item: TcgDexPriceItem): PriceEntity {
        return PriceEntity(
            cardId = cardId,
            finish = finish,
            condition = CONDITION_NM,
            marketPrice = item.resolveMarket(),
            lowPrice = item.resolveLow(),
            midPrice = item.resolveMid(),
            highPrice = item.resolveHigh(),
            source = "tcgdex"
        )
    }

    fun mapJustTcgVariantToPrice(cardId: String, variant: JustTcgVariant): PriceEntity? {
        val finish = mapJustTcgPrintingToFinish(variant.printing) ?: return null
        val condition = variant.condition
        
        val marketPrice = variant.prices?.market ?: variant.price ?: variant.avgPrice
        
        return PriceEntity(
            cardId = cardId,
            finish = finish,
            condition = condition,
            marketPrice = marketPrice,
            lowPrice = variant.prices?.low ?: variant.minPrice7d,
            midPrice = variant.prices?.mid,
            highPrice = variant.prices?.high ?: variant.maxPrice7d,
            source = "justtcg"
        )
    }

    fun mapJustTcgVariantToVintagePrice(cardId: String, variant: JustTcgVariant): VintagePriceEntity? {
        val finish = mapJustTcgPrintingToFinish(variant.printing) ?: FINISH_NORMAL
        val marketPrice = variant.prices?.market ?: variant.price ?: variant.avgPrice
        
        return VintagePriceEntity(
            cardId = cardId,
            finish = finish,
            printing = variant.printing,
            condition = variant.condition,
            marketPrice = marketPrice,
            lowPrice = variant.prices?.low ?: variant.minPrice7d,
            midPrice = variant.prices?.mid,
            highPrice = variant.prices?.high ?: variant.maxPrice7d,
            source = "justtcg"
        )
    }

    private fun mapJustTcgPrintingToFinish(printing: String): String? {
        val p = printing.lowercase()
        return when {
            p.contains("reverse") -> FINISH_REVERSE_HOLOFOIL
            p.contains("holofoil") || p.contains("holo") -> FINISH_HOLOFOIL
            p.contains("normal") || p.contains("unlimited") || p.contains("standard") -> FINISH_NORMAL
            else -> FINISH_NORMAL
        }
    }
}
