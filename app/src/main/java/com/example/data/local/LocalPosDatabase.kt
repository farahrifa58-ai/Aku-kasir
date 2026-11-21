package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StoreSettingsEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        ExpenseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LocalPosDatabase : RoomDatabase() {
    abstract fun posDao(): LocalPosDao

    companion object {
        @Volatile
        private var INSTANCE: LocalPosDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_settings ADD COLUMN storeIcon TEXT NOT NULL DEFAULT 'storefront'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN storeIconBgColor TEXT NOT NULL DEFAULT '#2563EB'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN receiptPrefix TEXT NOT NULL DEFAULT 'AK'")
            }
        }

        fun getInstance(context: Context): LocalPosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalPosDatabase::class.java,
                    "ada_kasir_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
