package com.example.surveyland.util

import android.content.Context
import android.content.SharedPreferences

object SPUtils {
    private const val FILE_NAME = "app_sp"

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun put(key: String, value: Any) {

        val editor = sp.edit()

        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
            is Set<*> -> editor.putStringSet(key, value as Set<String>)
            else -> throw IllegalArgumentException("Unsupported type")
        }

        editor.apply()
    }

    fun getString(key: String, default: String = ""): String {
        return sp.getString(key, default) ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return sp.getInt(key, default)
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return sp.getBoolean(key, default)
    }

    fun getFloat(key: String, default: Float = 0f): Float {
        return sp.getFloat(key, default)
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return sp.getLong(key, default)
    }

    fun getStringSet(key: String): Set<String>? {
        return sp.getStringSet(key, null)
    }

    fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    fun contains(key: String): Boolean {
        return sp.contains(key)
    }
}