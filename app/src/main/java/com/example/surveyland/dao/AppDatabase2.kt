package com.example.surveyland.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.surveyland.entity.MeasureEntity

@Database(entities = [MeasureEntity::class], version = 1)
abstract class AppDatabase2 : RoomDatabase() {

    abstract fun measureDao(): MeasureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase2? = null

        fun get(context: Context): AppDatabase2 {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase2::class.java,
                    "measure_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}