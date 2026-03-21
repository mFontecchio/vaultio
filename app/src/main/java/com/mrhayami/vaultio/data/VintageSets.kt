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
        VintageSetConfig("base2", hasFirstEdition = false, justTcgSetId = "jungle-pokemon"),
        VintageSetConfig("base3", hasFirstEdition = true, justTcgSetId = "fossil-pokemon"),
        VintageSetConfig("base4", hasFirstEdition = true, justTcgSetId = "base-set-2-pokemon"),
        VintageSetConfig("base5", hasFirstEdition = true, justTcgSetId = "team-rocket-pokemon"),
        VintageSetConfig("gym1", hasFirstEdition = true, justTcgSetId = "gym-heroes-pokemon"),
        VintageSetConfig("gym2", hasFirstEdition = true, justTcgSetId = "gym-challenge-pokemon"),
        VintageSetConfig("neo1", hasFirstEdition = true, justTcgSetId = "neo-genesis-pokemon"),
        VintageSetConfig("neo2", hasFirstEdition = true, justTcgSetId = "neo-discovery-pokemon"),
        VintageSetConfig("neo3", hasFirstEdition = true, justTcgSetId = "neo-revelation-pokemon"),
        VintageSetConfig("neo4", hasFirstEdition = true, justTcgSetId = "neo-destiny-pokemon")
    )

    fun isVintageSet(setId: String): Boolean {
        return VINTAGE_SETS.any { it.id == setId }
    }

    fun getVintageConfig(setId: String): VintageSetConfig? {
        return VINTAGE_SETS.find { it.id == setId }
    }

    fun getJustTcgSetId(setId: String, printing: String): String? {
        val config = getVintageConfig(setId) ?: return null
        return if (printing.lowercase() == "shadowless" && config.shadowlessJustTcgSetId != null) {
            config.shadowlessJustTcgSetId
        } else {
            config.justTcgSetId
        }
    }
}
