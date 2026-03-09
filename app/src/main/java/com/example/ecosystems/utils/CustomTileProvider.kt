package com.example.ecosystems.utils

import com.yandex.mapkit.RawTile
import com.yandex.mapkit.TileId
import com.yandex.mapkit.Version
import com.yandex.mapkit.tiles.TileProvider

class OsmTileProvider(private val TileFileName: String) : TileProvider {

    override fun load(
        tileId: TileId,
        version: Version,
        features: Map<String, String>,
        etag: String
    ): RawTile {

        //val url = "https://smartecosystems.petrsu.ru/api/v1/orthophotoplans/tile_file/148f3abd-a16a-4a63-93b9-503fbe403f1b/${tileId.z}/${tileId.x}/${tileId.y}.png"
        val url = "https://smartecosystems.petrsu.ru/api/v1/orthophotoplans/tile_file/${TileFileName}/${tileId.z}/${tileId.x}/${tileId.y}.png"
        //Log.d("tile", "${TileFileName} ${tileId.z}/${tileId.x}/${tileId.y}.png")
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            // Передаём etag для кэширования (304 Not Modified)
            if (etag.isNotEmpty()) {
                connection.setRequestProperty("If-None-Match", etag)
            }

            val responseCode = connection.responseCode

            when (responseCode) {
                // Тайл успешно загружен
                200 -> {
                    val bytes = connection.inputStream.readBytes()
                    val newEtag = connection.getHeaderField("ETag") ?: etag
                    RawTile(
                        version,
                        features,
                        newEtag,
                        RawTile.UseCache.YES,       // кэшировать тайл
                        RawTile.State.OK,
                        bytes
                    )
                }

                // Тайл не изменился — взять из кэша
                304 -> {
                    RawTile(
                        version,
                        features,
                        etag,
                        RawTile.UseCache.YES,
                        RawTile.State.NOT_MODIFIED,
                        ByteArray(0)                // данные возьмутся из кэша
                    )
                }
                // Временная ошибка сервера — MapKit попробует снова позже
                else -> {
                    RawTile(
                        version,
                        features,
                        etag,
                        RawTile.UseCache.NO,
                        RawTile.State.OK,        // повторить запрос
                        ByteArray(0)
                    )
                }
            }

        } catch (e: java.net.SocketTimeoutException) {
            // Таймаут — попробовать снова
            RawTile(
                version,
                features,
                etag,
                RawTile.UseCache.NO,
                RawTile.State.ERROR,
                ByteArray(0)
            )
        } catch (e: Exception) {
            // Сетевая ошибка — вернуть ERROR
            RawTile(
                version,
                features,
                etag,
                RawTile.UseCache.NO,
                RawTile.State.ERROR,
                ByteArray(0)
            )
        }
    }
}