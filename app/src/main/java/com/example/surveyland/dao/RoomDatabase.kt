package com.example.surveyland.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.surveyland.entity.LandEntity

@Database(entities = [LandEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun landDao(): LandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "land_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}