package de.larmic.pf2e.domain

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class PathfinderItemStore {

    private val items = ConcurrentHashMap<UUID, PathfinderItem>()

    fun save(item: PathfinderItem): PathfinderItem {
        items[item.id] = item
        return item
    }

    fun findByGithubPath(path: String): PathfinderItem? =
        items.values.find { it.githubPath == path }

    fun findByFoundryId(foundryId: String): PathfinderItem? =
        items.values.find { it.foundryId == foundryId }

    fun findAllByType(type: ItemType): List<PathfinderItem> =
        items.values.filter { it.itemType == type }

    fun findAll(): List<PathfinderItem> = items.values.toList()

    fun count(): Int = items.size

    fun countByType(type: ItemType): Int = items.values.count { it.itemType == type }

    fun clear() = items.clear()
}
