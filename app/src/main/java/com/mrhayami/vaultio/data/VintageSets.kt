package com.mrhayami.vaultio.data

data class VintageSetConfig(
    val id: String,
    val hasFirstEdition: Boolean = true,
    val hasShadowless: Boolean = false,
    val justTcgSetId: String,
    val shadowlessJustTcgSetId: String? = null,
    /** Number printed after the slash. Secrets can sit above this (Team Rocket 83/82). */
    val printedTotal: Int
)

object VintageSets {
    private val VINTAGE_SETS = listOf(
        VintageSetConfig("base1", hasFirstEdition = true, hasShadowless = true, justTcgSetId = "base-set-pokemon", shadowlessJustTcgSetId = "base-set-shadowless-pokemon", printedTotal = 102),
        VintageSetConfig("base2", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "jungle-pokemon", printedTotal = 64),
        VintageSetConfig("base3", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "fossil-pokemon", printedTotal = 62),
        VintageSetConfig("base4", hasFirstEdition = false, hasShadowless = false, justTcgSetId = "base-set-2-pokemon", printedTotal = 130),
        VintageSetConfig("base5", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "team-rocket-pokemon", printedTotal = 82),
        VintageSetConfig("gym1", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "gym-heroes-pokemon", printedTotal = 132),
        VintageSetConfig("gym2", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "gym-challenge-pokemon", printedTotal = 132),
        VintageSetConfig("neo1", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-genesis-pokemon", printedTotal = 111),
        VintageSetConfig("neo2", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-discovery-pokemon", printedTotal = 75),
        VintageSetConfig("neo3", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-revelation-pokemon", printedTotal = 64),
        VintageSetConfig("neo4", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-destiny-pokemon", printedTotal = 105)
    )

    fun isVintagePrintedTotal(total: Int): Boolean = VINTAGE_SETS.any { it.printedTotal == total }

    fun printedTotals(): Set<Int> = VINTAGE_SETS.map { it.printedTotal }.toSet()

    fun printedTotalFor(setId: String): Int? = getVintageConfig(setId)?.printedTotal

    fun isVintageSet(setId: String): Boolean {
        return VINTAGE_SETS.any { it.id == setId }
    }

    fun getVintageConfig(setId: String): VintageSetConfig? {
        return VINTAGE_SETS.find { it.id == setId }
    }

    fun getJustTcgSetId(setId: String, printing: String): String? {
        val config = getVintageConfig(setId) ?: return null
        return when (printing.lowercase()) {
            "shadowless" -> config.shadowlessJustTcgSetId
            "1st_edition" -> config.shadowlessJustTcgSetId ?: config.justTcgSetId
            else -> config.justTcgSetId
        }
    }
}
