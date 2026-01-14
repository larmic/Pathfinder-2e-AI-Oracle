package de.larmic.pf2e.domain

import org.springframework.stereotype.Component

@Component
class PathfinderItemStore(
    private val repository: PathfinderItemRepository
) {

    fun save(item: PathfinderItem): PathfinderItem = repository.save(item)

    fun findByGithubPath(path: String): PathfinderItem? = repository.findByGithubPath(path)

    fun findByFoundryId(foundryId: String): PathfinderItem? = repository.findByFoundryId(foundryId)

    fun findAllByType(type: ItemType): List<PathfinderItem> = repository.findAllByItemType(type)

    fun findAll(): List<PathfinderItem> = repository.findAll()

    fun count(): Int = repository.count().toInt()

    fun countByType(type: ItemType): Int = repository.countByItemType(type).toInt()

    fun clear() = repository.deleteAll()
}
