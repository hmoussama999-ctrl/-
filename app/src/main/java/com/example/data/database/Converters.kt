package com.example.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SaleItem::class.java)
    private val adapter = moshi.adapter<List<SaleItem>>(listType)

    @TypeConverter
    fun fromString(value: String): List<SaleItem> {
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<SaleItem>?): String {
        return try {
            adapter.toJson(list ?: emptyList())
        } catch (e: Exception) {
            "[]"
        }
    }
}
