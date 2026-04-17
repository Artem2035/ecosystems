package com.example.ecosystems.db.entity.table

import androidx.room.TypeConverter
import com.google.gson.annotations.SerializedName

enum class PropertyType {
    @SerializedName("string")  STRING,
    @SerializedName("number")  NUMBER,
    @SerializedName("enum")    ENUM
}

@TypeConverter
fun fromPropertyType(value: PropertyType): String = value.name

@TypeConverter
fun toPropertyType(value: String): PropertyType =
    PropertyType.valueOf(value)