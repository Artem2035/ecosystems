package com.example.ecosystems
import android.app.Application
import com.yandex.mapkit.MapKitFactory

class EcosystemsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Инициализируем только в основном процессе
        MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
        MapKitFactory.initialize(this)
    }
}
