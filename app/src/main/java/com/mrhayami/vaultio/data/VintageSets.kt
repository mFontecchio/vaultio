package com.mrhayami.vaultio.data

data class VintageSetConfig(
    val id: String,
    val hasFirstEdition: Boolean = true,
    val hasShadowless: Boolean = false,
    val justTcgSetId: String,
    val shadowlessJustTcgSetId: String? = null
)

object VintageSets {
    private val VINTAGE_SETS = listOf(
        VintageSetConfig("base1", hasFirstEdition = true, hasShadowless = true, justTcgSetId = "base-set-pokemon", shadowlessJustTcgSetId = "base-set-shadowless-pokemon"),
        VintageSetConfig("base2", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "jungle-pokemon"),
        VintageSetConfig("base3", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "fossil-pokemon"),
        VintageSetConfig("base4", hasFirstEdition = false, hasShadowless = false, justTcgSetId = "base-set-2-pokemon"),
        VintageSetConfig("base5", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "team-rocket-pokemon"),
        VintageSetConfig("gym1", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "gym-heroes-pokemon"),
        VintageSetConfig("gym2", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "gym-challenge-pokemon"),
        VintageSetConfig("neo1", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-genesis-pokemon"),
        VintageSetConfig("neo2", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-discovery-pokemon"),
        VintageSetConfig("neo3", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-revelation-pokemon"),
        VintageSetConfig("neo4", hasFirstEdition = true, hasShadowless = false, justTcgSetId = "neo-destiny-pokemon")
    )

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
