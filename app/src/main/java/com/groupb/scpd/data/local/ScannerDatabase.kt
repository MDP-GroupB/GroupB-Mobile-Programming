package com.groupb.scpd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedBookingEntity::class,
        PendingAttendanceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScannerDatabase : RoomDatabase() {

    abstract fun scannerDao(): ScannerDao

    companion object {
        @Volatile
        private var INSTANCE: ScannerDatabase? = null

        fun getInstance(context: Context): ScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    "scpd_scanner.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}