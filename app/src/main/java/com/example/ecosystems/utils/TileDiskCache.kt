package com.example.ecosystems.utils

import android.content.Context
import android.util.Log
import java.io.File

/*
* Структура папок на диске - filesDir/tiles/{TileFileName}/{z}/{x}/{y}.png
* */
class TileDiskCache(context: Context) {

    // Корневая папка: /data/data/<package>/files/tiles/
    private val rootDir: File = File(context.filesDir, "tiles")

    /* Возвращает файл тайла. Не проверяет существование. */
    private fun tileFile(tileFileName: String, z: Int, x: Int, y: Int): File =
        File(rootDir, "$tileFileName/$z/$x/$y.png")

    /* true — тайл уже скачан */
    fun exists(tileFileName: String, z: Int, x: Int, y: Int): Boolean =
        tileFile(tileFileName, z, x, y).exists()

    /* Читает байты тайла. null — если файла нет */
    fun read(tileFileName: String, z: Int, x: Int, y: Int): ByteArray? {
        val file = tileFile(tileFileName, z, x, y)
        return if (file.exists()) file.readBytes() else null
    }

    /*
     * Атомарно записывает тайл:
     * сначала во временный файл, потом переименовывает.
     * Это защищает от битых файлов при краше в процессе записи.
     */
    fun write(tileFileName: String, z: Int, x: Int, y: Int, data: ByteArray) {
        val target = tileFile(tileFileName, z, x, y)
        target.parentFile?.mkdirs() // создать папки z/x/ если нет
        val tmp = File(target.parent, "${target.name}.tmp")
        try {
            tmp.writeBytes(data)
            tmp.renameTo(target)
        } catch (e: Exception) {
            Log.e("TileDiskCache", "Write FAILED: ${e.message}", e)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /* Удаляет все тайлы одного плана */
    fun clearPlan(tileFileName: String) {
        File(rootDir, tileFileName).deleteRecursively()
    }

    /* Размер кэша одного плана в байтах */
    fun planSizeBytes(tileFileName: String): Long =
        File(rootDir, tileFileName).walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
}