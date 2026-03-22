package com.mrhayami.vaultio.data

import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.JustTcgPrice
import com.mrhayami.vaultio.data.remote.JustTcgVariant
import com.mrhayami.vaultio.data.remote.TcgDexPriceItem
import com.mrhayami.vaultio.data.remote.TcgDexTcgPlayerPricing

object PricingUtils {
    // Standardized Finishes from Source Project
    const val FINISH_NORMAL = "normal"
    const val FINISH_HOLOFOIL = "holofoil"
    const val FINISH_REVERSE_HOLO = "reverse holo"
    const val FINISH_TEXTURED = "textured"
    const val FINISH_GOLD = "gold"

    // Standardized Printings from Source Project
    const val PRINTING_UNLIMITED = "unlimited"
    const val PRINTING_SHADOWLESS = "shadowless"
    const val PRINTING_PROMO = "promo"
    const val PRINTING_1ST_EDITION = "1st edition"

    // Standardized Conditions from Source Project
    const val CONDITION_NM = "Near Mint"
    const val CONDITION_LP = "Lightly Played"
    const val CONDITION_MP = "Moderately Played"
    const val CONDITION_HP = "Heavily Played"
    const val CONDITION_DMG = "Damaged"

    fun normalizeCardNumber(localId: String): String {
        var cleaned = localId.split("/")[0]
        val match = Regex("^([A-Za-z]*)0*(\\d+)([A-Za-z]*)$").find(cleaned)
        if (match != null) {
            val (prefix, num, suffix) = match.destructured
            cleaned = "$prefix$num$suffix"
        }
        return cleaned
    }

    // OUR MODEL -> JUSTTCG (for queries and batch filters)
    fun mapToJustTcgPrinting(finish: String, printing: String): String {
        val f = finish.lowercase()
        val p = printing.lowercase()

        // JustTCG usually maps textured/gold to "Holofoil" for the sake of market pricing
        val jtcgFinish = when (f) {
            FINISH_REVERSE_HOLO -> "Reverse Holofoil"
            FINISH_HOLOFOIL, FINISH_TEXTURED, FINISH_GOLD -> "Holofoil"
            else -> "Normal"
        }

        return when (p) {
            PRINTING_1ST_EDITION -> "1st Edition $jtcgFinish"
            PRINTING_SHADOWLESS -> "Shadowless $jtcgFinish"
            else -> jtcgFinish // Unlimited and Promo usually don't have a prefix in the printing string
        }
    }

    // JUSTTCG -> OUR MODEL (for parsing responses)
    fun parseJustTcgPrinting(jtcgPrinting: String): Pair<String, String> {
        val lower = jtcgPrinting.lowercase()
        
        val edition = when {
            lower.contains("1st edition") -> PRINTING_1ST_EDITION
            lower.contains("shadowless") -> PRINTING_SHADOWLESS
            else -> PRINTING_UNLIMITED
        }
        
        val finish = when {
            lower.contains("reverse") -> FINISH_REVERSE_HOLO
            lower.contains("holofoil") || lower.contains("holo") -> FINISH_HOLOFOIL
            else -> FINISH_NORMAL
        }
        
        return Pair(finish, edition)
    }

    fun mapTcgDexPrices(cardId: String, pricing: TcgDexTcgPlayerPricing): List<PriceEntity> {
        val result = mutableListOf<PriceEntity>()
        pricing.normal?.let { if (hasAnyPrice(it)) result.add(createPriceEntity(cardId, FINISH_NORMAL, it)) }
        pricing.holofoil?.let { if (hasAnyPrice(it)) result.add(createPriceEntity(cardId, FINISH_HOLOFOIL, it)) }
        pricing.reverse?.let { if (hasAnyPrice(it)) result.add(createPriceEntity(cardId, FINISH_REVERSE_HOLO, it)) }
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
        val (finish, _) = parseJustTcgPrinting(variant.printing)
        // Conditions are already 1:1 strings
        
        val marketPrice = variant.prices?.market ?: variant.price ?: variant.avgPrice
        
        return PriceEntity(
            cardId = cardId,
            finish = finish,
            condition = variant.condition,
            marketPrice = marketPrice,
            lowPrice = variant.prices?.low ?: variant.minPrice7d,
            midPrice = variant.prices?.mid,
            highPrice = variant.prices?.high ?: variant.maxPrice7d,
            source = "justtcg"
        )
    }

    fun mapJustTcgVariantToVintagePrice(
        cardId: String, 
        variant: JustTcgVariant, 
        targetPrinting: String? = null
    ): VintagePriceEntity? {
        val (finish, edition) = parseJustTcgPrinting(variant.printing)
        val finalPrinting = targetPrinting ?: edition
        
        val marketPrice = variant.prices?.market ?: variant.price ?: variant.avgPrice
        
        return VintagePriceEntity(
            cardId = cardId,
            finish = finish,
            printing = finalPrinting,
            condition = variant.condition,
            marketPrice = marketPrice,
            lowPrice = variant.prices?.low ?: variant.minPrice7d,
            midPrice = variant.prices?.mid,
            highPrice = variant.prices?.high ?: variant.maxPrice7d,
            source = "justtcg"
        )
    }
}
