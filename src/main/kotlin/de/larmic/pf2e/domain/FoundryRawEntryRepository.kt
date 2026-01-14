package de.larmic.pf2e.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FoundryRawEntryRepository : JpaRepository<FoundryRawEntry, UUID> {

    fun findByGithubPath(path: String): FoundryRawEntry?

    fun findByFoundryId(foundryId: String): FoundryRawEntry?

    fun findAllByFoundryType(foundryType: String): List<FoundryRawEntry>

    fun countByFoundryType(foundryType: String): Long

    @Query("SELECT DISTINCT e.foundryType FROM FoundryRawEntry e ORDER BY e.foundryType")
    fun findAllFoundryTypes(): List<String>
}
