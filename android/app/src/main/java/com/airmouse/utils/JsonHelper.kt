
package com.airmouse.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object JsonHelper {

    @PublishedApi
    internal val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    inline fun <reified T> fromJson(json: String): T? {
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> fromJsonList(json: String): List<T>? {
        return try {
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> toJson(obj: T): String? {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> toJsonPretty(obj: T): String? {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> fromMap(map: Map<String, Any>): T? {
        return try {
            val json = gson.toJson(map)
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> toMap(obj: T): Map<String, Any>? {
        return try {
            val json = gson.toJson(obj)
            gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
}
