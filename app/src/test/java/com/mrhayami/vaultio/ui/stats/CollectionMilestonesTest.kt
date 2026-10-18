package com.mrhayami.vaultio.ui.stats

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionMilestonesTest {

    @Test
    fun `empty collection unlocks nothing`() {
        val milestones = computeCollectionMilestones(0, 0, 0, 0)
        assertTrue(milestones.none { it.unlocked })
    }

    @Test
    fun `thresholds unlock expected badges`() {
        val milestones = computeCollectionMilestones(
            cardCount = 120,
            vintageCount = 2,
            completedSetCount = 1,
            uniqueSpeciesCount = 151
        )
        assertTrue(milestones.first { it.id == "first_card" }.unlocked)
        assertTrue(milestones.first { it.id == "century" }.unlocked)
        assertTrue(milestones.first { it.id == "vintage" }.unlocked)
        assertTrue(milestones.first { it.id == "set_master" }.unlocked)
        assertTrue(milestones.first { it.id == "pokedex" }.unlocked)
        assertFalse(milestones.any { it.id == "missing" })
    }
}
