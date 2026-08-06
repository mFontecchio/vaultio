package com.mrhayami.vaultio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PriceMetaDao {
    @Query("SELECT * FROM price_meta WHERE id = :id")
    suspend fun getPriceMeta(id: String): PriceMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceMeta(meta: PriceMetaEntity)

    @Query("DELETE FROM price_meta WHERE id = :id")
    suspend fun deletePriceMeta(id: String)
}
