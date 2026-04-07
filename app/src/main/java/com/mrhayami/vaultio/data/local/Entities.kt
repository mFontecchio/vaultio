package com.mrhayami.vaultio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mrhayami.vaultio.data.PricingUtils

@Entity(tableName = "sets")
data class SetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val series: String?,
    val logo: String?,
    val symbol: String?,
    val totalCards: Int,
    val officialCards: Int = 0,
    val releaseDate: String?,
    val isDownloaded: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cards",
    indices = [Index(value = ["setId"])]
)
data class CardEntity(
    @PrimaryKey val id: String,
    val localId: String,
    val name: String,
    val image: String?,
    val setId: String,
    val rarity: String?,
    val category: String?,
    val types: String?, // Comma-separated or JSON
    val dexId: String?, // Primary National Dex number
    val dexIds: String? = null, // JSON array of all dex IDs for multi-Pokemon cards
    val pokemonName: String? = null, // Extracted Pokemon base name
    val tcgPlayerId: String? = null,
    val pHash: Long? = null, // Perceptual hash for image-based disambiguation
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_cards",
    indices = [Index(value = ["cardId"])]
)
data class UserCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val quantity: Int = 1,
    val condition: String = PricingUtils.CONDITION_NM,
    val printing: String = PricingUtils.PRINTING_UNLIMITED,
    val finish: String = PricingUtils.FINISH_NORMAL,
    val manualPrice: Double? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "folder_cards",
    primaryKeys = ["folderId", "userCardId"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["userCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userCardId"])]
)
data class FolderCardCrossRef(
    val folderId: Long,
    val userCardId: Long
)

@Entity(
    tableName = "prices",
    primaryKeys = ["cardId", "finish", "condition"]
)
data class PriceEntity(
    val cardId: String,
    val finish: String,
    val condition: String,
    val marketPrice: Double?,
    val lowPrice: Double?,
    val midPrice: Double?,
    val highPrice: Double?,
    val source: String, // "tcgdex" or "justtcg"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vintage_prices",
    primaryKeys = ["cardId", "finish", "printing", "condition"]
)
data class VintagePriceEntity(
    val cardId: String,
    val finish: String,
    val printing: String,
    val condition: String,
    val marketPrice: Double?,
    val lowPrice: Double?,
    val midPrice: Double?,
    val highPrice: Double?,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_meta")
data class PriceMetaEntity(
    @PrimaryKey val id: String, // "cardId-finish-condition" or similar
    val lastFetch: Long,
    val lastError: String? = null
)

@Entity(tableName = "api_usage")
data class ApiUsageEntity(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val count: Int = 0,
    val dailyLimit: Int = 100,
    val dailyRemaining: Int = 100,
    val planLimit: Int = 1000,
    val planUsed: Int = 0,
    val planRemaining: Int = 1000,
    val planName: String = "Free",
    /** Epoch-millis of the last successful GET /health sync; 0 = never synced. */
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "telemetry_log")
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val api: String,
    val endpoint: String,
    val status: Int,
    val latency: Long,
    val timestamp: Long = System.currentTimeMillis()
)
