package com.example.ecosystems.db.entity.layer

import com.google.gson.annotations.SerializedName

enum class LayerType {
    @SerializedName("points") POINTS,
    @SerializedName("library_images") LIBRARY_IMAGES
}