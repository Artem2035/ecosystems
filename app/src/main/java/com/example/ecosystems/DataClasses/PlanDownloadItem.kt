package com.example.ecosystems.DataClasses

data class PlanDownloadItem(
    val plan: Plan,
    var isDownloaded: Boolean,
    var isLoading: Boolean = false
)