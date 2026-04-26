package com.example.ecosystems.utils

import android.content.Context
import android.util.Log
import com.example.ecosystems.network.BASE_URL
import com.yandex.mapkit.RawTile
import com.yandex.mapkit.TileId
import com.yandex.mapkit.Version
import com.yandex.mapkit.tiles.TileProvider

class OsmTileProvider(private val tileFileName: String,
                      private val diskCache: TileDiskCache,
                      private val context: Context
) : TileProvider {

    override fun load(
        tileId: TileId,
        version: Version,
        features: Map<String, String>,
        etag: String
    ): RawTile {

        val z = tileId.z
        val x = tileId.x
        val y = tileId.y

        val cached = diskCache.read(tileFileName, z, x, y)
        if (cached != null) {
            Log.d("OsmTile", "HIT disk: $tileFileName/$z/$x/$y")
            // Тайл есть на диске — отдаём без сетевого запроса.
            // UseCache.YES говорит Yandex Maps тоже закэшировать в своём слое.
            return RawTile(version, features, etag, RawTile.UseCache.YES, RawTile.State.OK, cached)
        }

        if (!context.isInternetAvailable()) {
            // Возвращаем пустой тайл без ошибки — карта просто покажет пустую плитку
            return RawTile(
                version, features, etag,
                RawTile.UseCache.NO,
                RawTile.State.OK,
                ByteArray(0)
            )
        }

        val url = "${BASE_URL}api/v1/orthophotoplans/tile_file/${tileFileName}/${z}/${x}/${y}.png"
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            // Передаём etag для кэширования (304 Not Modified)
            if (etag.isNotEmpty() && diskCache.exists(tileFileName, z,x,y)) {
                connection.setRequestProperty("If-None-Match", etag)
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                // Тайл успешно загружен
                200 -> {
                    val bytes = connection.inputStream.readBytes()
                    val newEtag = connection.getHeaderField("ETag") ?: etag

                    // Сохраняем на диск — следующий запрос пойдёт из кэша
                    diskCache.write(tileFileName, z, x, y, bytes)

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