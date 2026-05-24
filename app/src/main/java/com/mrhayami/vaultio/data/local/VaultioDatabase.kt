package com.mrhayami.vaultio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SetEntity::class,
        CardEntity::class,
        UserCardEntity::class,
        FolderEntity::class,
        FolderCardCrossRef::class,
        PriceEntity::class,
        VintagePriceEntity::class,
        PriceMetaEntity::class,
        ApiUsageEntity::class,
        TelemetryLogEntity::class,
        CollectionSnapshotEntity::class,
        CardGradeEntity::class,
        WishlistCardEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class VaultioDatabase : RoomDatabase() {
    abstract fun setDao(): SetDao
    abstract fun cardDao(): CardDao
    abstract fun userCardDao(): UserCardDao
    abstract fun folderDao(): FolderDao
    abstract fun priceDao(): PriceDao
    abstract fun apiUsageDao(): ApiUsageDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun collectionSnapshotDao(): CollectionSnapshotDao
    abstract fun cardGradeDao(): CardGradeDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile
        private var INSTANCE: VaultioDatabase? = null

        fun getDatabase(context: Context): VaultioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultioDatabase::class.java,
                    "vaultio_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
