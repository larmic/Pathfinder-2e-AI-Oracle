package de.larmic.pf2e.domain

import com.fasterxml.uuid.Generators
import java.time.Instant
import java.util.UUID

enum class ItemType {
    FEAT,
    SPELL,
    ITEM,
    ACTION,
    ANCESTRY,
    CLASS_FEATURE,
    HERITAGE
}

data class PathfinderItem(
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),
    val foundryId: String,
    val itemType: ItemType,
    val itemName: String,
    val rawJsonContent: String,
    val githubSha: String,
    val lastSync: Instant = Instant.now(),
    val githubPath: String
)
