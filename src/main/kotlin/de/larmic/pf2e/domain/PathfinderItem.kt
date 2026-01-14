package de.larmic.pf2e.domain

import com.fasterxml.uuid.Generators
import jakarta.persistence.*
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

@Entity
@Table(
    name = "pathfinder_items",
    indexes = [
        Index(name = "idx_github_path", columnList = "githubPath", unique = true),
        Index(name = "idx_foundry_id", columnList = "foundryId"),
        Index(name = "idx_item_type", columnList = "itemType")
    ]
)
data class PathfinderItem(
    @Id
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),

    @Column(nullable = false)
    val foundryId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val itemType: ItemType,

    @Column(nullable = false)
    val itemName: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val rawJsonContent: String,

    @Column(nullable = false, length = 64)
    val githubSha: String,

    @Column(nullable = false)
    val lastSync: Instant = Instant.now(),

    @Column(nullable = false)
    val githubPath: String
)
