package de.larmic.pf2e.domain

import com.fasterxml.uuid.Generators
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "foundry_raw_entries",
    indexes = [
        Index(name = "idx_github_path", columnList = "githubPath", unique = true),
        Index(name = "idx_foundry_id", columnList = "foundryId"),
        Index(name = "idx_foundry_type", columnList = "foundryType")
    ]
)
data class FoundryRawEntry(
    @Id
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),

    @Column(nullable = false)
    val foundryId: String,

    @Column(nullable = false, length = 64)
    val foundryType: String,

    @Column(nullable = false)
    val name: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    val rawJsonContent: String,

    @Column(nullable = false, length = 64)
    val githubSha: String,

    @Column(nullable = false)
    val lastSync: Instant = Instant.now(),

    @Column(nullable = false)
    val githubPath: String
)
