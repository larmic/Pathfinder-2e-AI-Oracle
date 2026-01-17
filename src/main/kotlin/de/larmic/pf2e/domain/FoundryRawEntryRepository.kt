package de.larmic.pf2e.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
interface FoundryRawEntryRepository : JpaRepository<FoundryRawEntry, UUID> {

    fun findByGithubPath(path: String): FoundryRawEntry?

    fun findByFoundryId(foundryId: String): FoundryRawEntry?

    fun findAllByFoundryType(foundryType: String): List<FoundryRawEntry>

    fun countByFoundryType(foundryType: String): Long

    @Query("SELECT DISTINCT e.foundryType FROM FoundryRawEntry e ORDER BY e.foundryType")
    fun findAllFoundryTypes(): List<String>

    // Incremental vectorization queries

    @Query("SELECT e FROM FoundryRawEntry e WHERE e.vectorizedSha IS NULL OR e.vectorizedSha <> e.githubSha")
    fun findAllPendingVectorization(): List<FoundryRawEntry>

    @Query("SELECT e FROM FoundryRawEntry e WHERE e.foundryType = :type AND (e.vectorizedSha IS NULL OR e.vectorizedSha <> e.githubSha)")
    fun findPendingVectorizationByType(@Param("type") type: String): List<FoundryRawEntry>

    @Query("SELECT COUNT(e) FROM FoundryRawEntry e WHERE e.vectorizedSha IS NULL OR e.vectorizedSha <> e.githubSha")
    fun countPendingVectorization(): Long

    @Modifying
    @Transactional
    @Query("UPDATE FoundryRawEntry e SET e.vectorizedSha = e.githubSha WHERE e.id = :id")
    fun markAsVectorized(@Param("id") id: UUID)

    @Modifying
    @Transactional
    @Query("UPDATE FoundryRawEntry e SET e.vectorizedSha = e.githubSha WHERE e.id IN :ids")
    fun markAsVectorized(@Param("ids") ids: List<UUID>)

    // Orphan cleanup queries

    @Query("SELECT e.id AS id, e.githubPath AS githubPath FROM FoundryRawEntry e")
    fun findAllIdAndPaths(): List<IdAndPath>

    @Modifying
    @Transactional
    @Query("DELETE FROM FoundryRawEntry e WHERE e.id IN :ids")
    fun deleteAllByIds(@Param("ids") ids: List<UUID>)
}

interface IdAndPath {
    val id: UUID
    val githubPath: String
}
