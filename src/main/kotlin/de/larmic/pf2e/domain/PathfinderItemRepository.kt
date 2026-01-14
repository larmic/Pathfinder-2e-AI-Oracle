package de.larmic.pf2e.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PathfinderItemRepository : JpaRepository<PathfinderItem, UUID> {

    fun findByGithubPath(path: String): PathfinderItem?

    fun findByFoundryId(foundryId: String): PathfinderItem?

    fun findAllByItemType(itemType: ItemType): List<PathfinderItem>

    fun countByItemType(itemType: ItemType): Long
}
