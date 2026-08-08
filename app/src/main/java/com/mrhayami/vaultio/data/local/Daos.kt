package com.mrhayami.vaultio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

data class CardWithDetails(
    @Embedded
    val userCard: UserCardEntity,
    @Embedded(prefix = "card_")
    val card: CardEntity,
    @Embedded(prefix = "set_")
    val set: SetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "userCardId"
    )
    val grade: CardGradeEntity? = null
)

data class WishlistCardWithDetails(
    @Embedded
    val wishlistCard: WishlistCardEntity,
    @Embedded(prefix = "card_")
    val card: CardEntity,
    @Embedded(prefix = "set_")
    val set: SetEntity
)

@Dao
interface SetDao {
    @Query("SELECT * FROM sets ORDER BY releaseDate DESC")
    fun getAllSets(): Flow<List<SetEntity>>

    @Query("SELECT * FROM sets")
    suspend fun getSetsSync(): List<SetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntity>): List<Long>

    @Query("UPDATE sets SET isDownloaded = :isDownloaded WHERE id = :setId")
    suspend fun updateDownloadStatus(setId: String, isDownloaded: Boolean): Int

    @Query("SELECT * FROM sets WHERE id = :setId")
    suspend fun getSetById(setId: String): SetEntity?
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getCardsBySet(setId: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: String): CardEntity?

    @Query("SELECT * FROM cards WHERE localId = :localId")
    suspend fun getCardsByLocalId(localId: String): List<CardEntity>

    /**
     * Searches for cards by localId AND cross-checks the parent set's card count.
     * Used by the scanner to narrow down the correct set when the OCR reads the
     * "X/TOTAL" collector number (e.g. 123/191 → only sets with 191 cards).
     */
    @Query("""
        SELECT cards.* FROM cards
        INNER JOIN sets ON cards.setId = sets.id
        WHERE cards.localId = :localId
        AND (sets.totalCards = :total OR sets.officialCards = :total)
    """)
    suspend fun getCardsByLocalIdAndSetTotal(localId: String, total: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE name LIKE '%' || :name || '%'")
    suspend fun searchCardsByName(name: String): List<CardEntity>

    /** Exact match on the stored [pokemonName] extracted base name (case-insensitive). */
    @Query("SELECT * FROM cards WHERE pokemonName = :name COLLATE NOCASE AND dexId IS NOT NULL LIMIT 5")
    suspend fun getCardsByPokemonName(name: String): List<CardEntity>

    @Query("DELETE FROM cards WHERE setId = :setId AND id NOT IN (SELECT cardId FROM user_cards)")
    suspend fun deleteCardsBySet(setId: String): Int

    @Query("UPDATE cards SET pHash = :pHash WHERE id = :cardId")
    suspend fun updateCardPHash(cardId: String, pHash: Long)

    @Query("DELETE FROM cards WHERE id NOT IN (SELECT cardId FROM user_cards)")
    suspend fun deleteAllUnusedCards(): Int
    
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int

    @Query("SELECT COUNT(*) FROM cards")
    fun observeCardCount(): Flow<Int>

    /** Distinct owned catalog cards still missing National Dex metadata. */
    @Query("""
        SELECT DISTINCT c.id FROM cards c
        INNER JOIN user_cards uc ON uc.cardId = c.id
        WHERE c.dexId IS NULL
    """)
    suspend fun getOwnedCardIdsMissingDex(): List<String>
}

@Dao
interface UserCardDao {
    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.dexIds as card_dexIds, c.pokemonName as card_pokemonName, c.tcgPlayerId as card_tcgPlayerId, c.pHash as card_pHash, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        WHERE user_cards.id = :userCardId
        """)
    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?>

    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.dexIds as card_dexIds, c.pokemonName as card_pokemonName, c.tcgPlayerId as card_tcgPlayerId, c.pHash as card_pHash, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        WHERE user_cards.id = :userCardId
        """)
    suspend fun getUserCardByIdSync(userCardId: Long): CardWithDetails?

    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.dexIds as card_dexIds, c.pokemonName as card_pokemonName, c.tcgPlayerId as card_tcgPlayerId, c.pHash as card_pHash, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        """)
    fun getAllUserCardsWithDetails(): Flow<List<CardWithDetails>>

    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.dexIds as card_dexIds, c.pokemonName as card_pokemonName, c.tcgPlayerId as card_tcgPlayerId, c.pHash as card_pHash, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        INNER JOIN folder_cards fc ON user_cards.id = fc.userCardId
        WHERE fc.folderId = :folderId
        """)
    fun getUserCardsByFolder(folderId: Long): Flow<List<CardWithDetails>>

    @Query("SELECT * FROM user_cards WHERE cardId = :cardId ORDER BY id DESC LIMIT 1")
    suspend fun getLastUserCardByCardId(cardId: String): UserCardEntity?

    @Query("""
        SELECT * FROM user_cards 
        WHERE cardId = :cardId 
        AND condition = :condition 
        AND printing = :printing 
        AND finish = :finish 
        LIMIT 1
    """)
    suspend fun findExistingUserCard(
        cardId: String,
        condition: String,
        printing: String,
        finish: String
    ): UserCardEntity?

    @Query("SELECT folderId FROM folder_cards WHERE userCardId = :userCardId")
    suspend fun getFolderIdsForUserCardSync(userCardId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCard(userCard: UserCardEntity): Long

    @Update
    suspend fun updateUserCard(userCard: UserCardEntity): Int

    @Query("DELETE FROM user_cards WHERE id = :userCardId")
    suspend fun deleteUserCard(userCardId: Long): Int

    @Query("DELETE FROM user_cards WHERE id IN (:userCardIds)")
    suspend fun deleteUserCards(userCardIds: List<Long>): Int

    @Query("SELECT SUM(quantity) FROM user_cards")
    suspend fun getTotalQuantity(): Int?

    @Query("SELECT COUNT(*) FROM user_cards")
    suspend fun getDistinctCardCount(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderCardCrossRef(crossRef: FolderCardCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolderCardCrossRefs(crossRefs: List<FolderCardCrossRef>): List<Long>

    @Query("DELETE FROM folder_cards WHERE userCardId = :userCardId AND folderId = :folderId")
    suspend fun removeCardFromFolder(userCardId: Long, folderId: Long): Int

    @Query("DELETE FROM folder_cards WHERE userCardId = :userCardId")
    suspend fun deleteFolderCardCrossRefsForUserCard(userCardId: Long): Int

    @Transaction
    @RawQuery(observedEntities = [UserCardEntity::class, CardEntity::class, SetEntity::class])
    fun getFilteredUserCards(query: SupportSQLiteQuery): Flow<List<CardWithDetails>>

    @Query("SELECT * FROM folder_cards")
    fun getAllFolderCardCrossRefs(): Flow<List<FolderCardCrossRef>>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @Delete
    suspend fun deleteFolder(folder: FolderEntity): Int
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM prices WHERE cardId = :cardId")
    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>>

    @Query("SELECT * FROM prices")
    fun getAllPrices(): Flow<List<PriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<PriceEntity>): List<Long>

    @Query("SELECT * FROM vintage_prices WHERE cardId = :cardId")
    fun getVintagePricesForCard(cardId: String): Flow<List<VintagePriceEntity>>

    @Query("SELECT * FROM vintage_prices")
    fun getAllVintagePrices(): Flow<List<VintagePriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVintagePrices(prices: List<VintagePriceEntity>): List<Long>
}

@Dao
interface ApiUsageDao {
    @Query("SELECT * FROM api_usage WHERE date = :date")
    suspend fun getUsageForDate(date: String): ApiUsageEntity?

    @Query("SELECT * FROM api_usage WHERE date = :date")
    fun getUsageFlow(date: String): Flow<ApiUsageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: ApiUsageEntity): Long
}

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insertLog(log: TelemetryLogEntity): Long

    @Query("SELECT * FROM telemetry_log ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<TelemetryLogEntity>>
}

@Dao
interface CollectionSnapshotDao {
    @Query("SELECT * FROM collection_snapshots ORDER BY timestamp ASC")
    fun getAllSnapshots(): Flow<List<CollectionSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: CollectionSnapshotEntity): Long

    @Query("DELETE FROM collection_snapshots WHERE timestamp < :threshold")
    suspend fun deleteOldSnapshots(threshold: Long): Int
}

@Dao
interface CardGradeDao {
    @Query("SELECT * FROM card_grades WHERE userCardId = :userCardId")
    fun getGradeForUserCard(userCardId: Long): Flow<CardGradeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: CardGradeEntity): Long

    @Query("DELETE FROM card_grades WHERE userCardId = :userCardId")
    suspend fun deleteGradeForUserCard(userCardId: Long): Int
}

@Dao
interface WishlistDao {
    @Transaction
    @Query(
        """
        SELECT
            wishlist_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.dexIds as card_dexIds, c.pokemonName as card_pokemonName, c.tcgPlayerId as card_tcgPlayerId, c.pHash as card_pHash, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM wishlist_cards
        INNER JOIN cards c ON wishlist_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        """
    )
    fun getAllWishlistCards(): Flow<List<WishlistCardWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistCard(wishlistCard: WishlistCardEntity): Long

    @Update
    suspend fun updateWishlistCard(wishlistCard: WishlistCardEntity): Int

    @Query("DELETE FROM wishlist_cards WHERE id = :wishlistId")
    suspend fun deleteWishlistCard(wishlistId: Long): Int

    @Query("SELECT * FROM wishlist_cards WHERE id = :wishlistId")
    suspend fun getWishlistCardByIdSync(wishlistId: Long): WishlistCardEntity?
}
