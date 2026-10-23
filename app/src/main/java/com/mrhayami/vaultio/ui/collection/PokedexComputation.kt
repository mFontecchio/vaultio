package com.mrhayami.vaultio.ui.collection

import com.mrhayami.vaultio.data.PokemonUtils
import com.mrhayami.vaultio.data.local.CardWithDetails

/**
 * Builds Pokédex slots from owned cards.
 * Prefer stored [CardEntity.dexId]/[CardEntity.dexIds]; fall back to static name→dex lookup
 * when catalog metadata is missing (e.g. set-download stubs before enrichment).
 */
internal fun computePokedexEntries(
    userCards: List<CardWithDetails>,
    settings: PokedexSettings
): List<PokedexEntry> {
    val buckets = mutableMapOf<Int, MutableList<CardWithDetails>>()
    val idToNameMap = mutableMapOf<Int, String>()
    val nameToDexIdMap = mutableMapOf<String, Int>()

    fun rememberName(id: Int, normalizedName: String) {
        val currentName = idToNameMap[id]
        if (currentName == null || normalizedName.length < currentName.length) {
            idToNameMap[id] = normalizedName
        }
        nameToDexIdMap[normalizedName.lowercase()] = id
    }

    fun addToBucket(id: Int, cardWithDetails: CardWithDetails) {
        val bucket = buckets.getOrPut(id) { mutableListOf() }
        if (!bucket.any { it.userCard.id == cardWithDetails.userCard.id }) {
            bucket.add(cardWithDetails)
        }
    }

    userCards.forEach { cardWithDetails ->
        val card = cardWithDetails.card
        val dexIds = PokemonUtils.parseDexIds(card.dexIds, card.dexId)
        val normalizedName = card.pokemonName ?: PokemonUtils.extractPokemonName(card.name)

        dexIds.forEach { id ->
            addToBucket(id, cardWithDetails)
            rememberName(id, normalizedName)
        }
    }

    userCards.forEach { cardWithDetails ->
        val card = cardWithDetails.card
        val dexIds = PokemonUtils.parseDexIds(card.dexIds, card.dexId)
        if (dexIds.isNotEmpty()) return@forEach

        val normalizedName = card.pokemonName ?: PokemonUtils.extractPokemonName(card.name)
        val fallbackIds = nameToDexIdMap[normalizedName.lowercase()]?.let { listOf(it) }
            ?: PokemonUtils.lookupDexIds(card.name).ifEmpty {
                PokemonUtils.lookupDexId(normalizedName)?.let { listOf(it) }.orEmpty()
            }

        fallbackIds.forEach { id ->
            addToBucket(id, cardWithDetails)
            rememberName(id, normalizedName)
        }
    }

    val entries = mutableListOf<PokedexEntry>()
    val maxDexNumber = if (settings.showUncollected) 1025 else buckets.keys.maxOrNull() ?: 0

    for (i in 1..maxDexNumber) {
        val cardsInBucket = buckets[i] ?: emptyList()
        if (cardsInBucket.isNotEmpty() || settings.showUncollected) {
            val displayName = idToNameMap[i]
                ?: cardsInBucket.mapNotNull { it.card.pokemonName }.firstOrNull()
                ?: cardsInBucket.firstOrNull()?.card?.let { PokemonUtils.extractPokemonName(it.name) }

            val spriteUrl = if (settings.useOfficialArt) {
                if (settings.useShinySprites) {
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/shiny/$i.png"
                } else {
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$i.png"
                }
            } else {
                val spriteType = if (settings.useShinySprites) "shiny" else "pokemon"
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/$spriteType/$i.png"
            }

            entries.add(
                PokedexEntry(
                    dexNumber = i,
                    pokemonName = displayName,
                    cardCount = cardsInBucket.size,
                    totalQuantity = cardsInBucket.sumOf { it.userCard.quantity },
                    representativeImage = cardsInBucket.firstOrNull()?.card?.image,
                    spriteUrl = spriteUrl,
                    isCollected = cardsInBucket.isNotEmpty()
                )
            )
        }
    }
    return entries
}

data class PokedexGeneration(
    val id: Int,
    val shortName: String,
    val region: String,
    val start: Int,
    val endInclusive: Int
) {
    fun contains(dexNumber: Int): Boolean = dexNumber in start..endInclusive
}

val PokedexGenerations: List<PokedexGeneration> = listOf(
    PokedexGeneration(1, "Gen 1", "Kanto", 1, 151),
    PokedexGeneration(2, "Gen 2", "Johto", 152, 251),
    PokedexGeneration(3, "Gen 3", "Hoenn", 252, 386),
    PokedexGeneration(4, "Gen 4", "Sinnoh", 387, 493),
    PokedexGeneration(5, "Gen 5", "Unova", 494, 649),
    PokedexGeneration(6, "Gen 6", "Kalos", 650, 721),
    PokedexGeneration(7, "Gen 7", "Alola", 722, 809),
    PokedexGeneration(8, "Gen 8", "Galar", 810, 905),
    PokedexGeneration(9, "Gen 9", "Paldea", 906, 1025)
)

fun entriesForGeneration(
    entries: List<PokedexEntry>,
    generation: PokedexGeneration?
): List<PokedexEntry> {
    if (generation == null) return entries
    return entries.filter { generation.contains(it.dexNumber) }
}

data class GenerationProgress(
    val generation: PokedexGeneration,
    val collected: Int,
    val total: Int
) {
    val complete: Boolean get() = total > 0 && collected >= total
    val fraction: Float get() = if (total == 0) 0f else collected.toFloat() / total
}

fun generationProgress(
    entries: List<PokedexEntry>,
    generation: PokedexGeneration
): GenerationProgress {
    val inGen = entries.filter { generation.contains(it.dexNumber) }
    val total = (generation.endInclusive - generation.start + 1)
        .coerceAtMost(inGen.size.coerceAtLeast(generation.endInclusive - generation.start + 1))
    val collected = inGen.count { it.isCollected }
    val slotTotal = if (inGen.isNotEmpty()) inGen.size else total
    return GenerationProgress(generation, collected, slotTotal)
}
