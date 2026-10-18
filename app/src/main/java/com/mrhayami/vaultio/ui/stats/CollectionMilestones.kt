package com.mrhayami.vaultio.ui.stats

data class CollectionMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val unlocked: Boolean
)

fun computeCollectionMilestones(
    cardCount: Int,
    vintageCount: Int,
    completedSetCount: Int,
    uniqueSpeciesCount: Int
): List<CollectionMilestone> = listOf(
    CollectionMilestone(
        id = "first_card",
        title = "First Card",
        subtitle = "Add a card to your vault",
        unlocked = cardCount >= 1
    ),
    CollectionMilestone(
        id = "century",
        title = "Century Club",
        subtitle = "Hold 100 cards",
        unlocked = cardCount >= 100
    ),
    CollectionMilestone(
        id = "vintage",
        title = "Vintage Hunter",
        subtitle = "Own a vintage-era card",
        unlocked = vintageCount >= 1
    ),
    CollectionMilestone(
        id = "set_master",
        title = "Set Completer",
        subtitle = "Finish any set",
        unlocked = completedSetCount >= 1
    ),
    CollectionMilestone(
        id = "pokedex",
        title = "Pokédex Master",
        subtitle = "Collect 151 unique species",
        unlocked = uniqueSpeciesCount >= 151
    )
)
