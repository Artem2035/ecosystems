package com.example.ecosystems.PhotoViewDialog

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object MagicWandProcessor {

    /**
     * Волшебная палочка — floodFill от точки, затем контур → полигон
     *
     * @param bitmap      оригинальное изображение
     * @param imagePoint  точка касания в координатах изображения
     * @param tolerance   допуск по цвету (0–255), чем больше — тем шире захват
     * @return            список точек полигона в координатах изображения, или null если не удалось
     */
    fun apply(
        bitmap: Bitmap,
        imagePoint: PointF,
        tolerance: Double = 30.0
    ): List<PointF>? {

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Перевести в RGB (Bitmap приходит в RGBA)
        val srcRgb = Mat()
        Imgproc.cvtColor(src, srcRgb, Imgproc.COLOR_RGBA2RGB)

        // Маска для floodFill (должна быть на 2px больше изображения)
        val mask = Mat.zeros(srcRgb.rows() + 2, srcRgb.cols() + 2, CvType.CV_8UC1)

        val seedPoint = Point(
            imagePoint.x.toDouble().coerceIn(0.0, (bitmap.width - 1).toDouble()),
            imagePoint.y.toDouble().coerceIn(0.0, (bitmap.height - 1).toDouble())
        )

        val flags = 4 or (255 shl 8) or Imgproc.FLOODFILL_MASK_ONLY

        Imgproc.floodFill(
            srcRgb,
            mask,
            seedPoint,
            Scalar(255.0),
            Rect(),
            Scalar(tolerance, tolerance, tolerance),
            Scalar(tolerance, tolerance, tolerance),
            flags
        )

        // Обрезать маску до размера изображения
        val maskCropped = mask.submat(1, srcRgb.rows() + 1, 1, srcRgb.cols() + 1)

        // Найти контуры по маске
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            maskCropped,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) {
            src.release(); srcRgb.release(); mask.release(); maskCropped.release()
            return null
        }

        // Взять самый большой контур
        val largest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return null

        // Упростить контур (Douglas-Peucker) — уменьшить число точек полигона
        val epsilon = 0.01 * Imgproc.arcLength(MatOfPoint2f(*largest.toArray()), true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(MatOfPoint2f(*largest.toArray()), approx, epsilon, true)

        if (approx.rows() < 3) {
            src.release(); srcRgb.release(); mask.release(); maskCropped.release()
            return null
        }

        // Конвертировать в список PointF
        val result = approx.toArray().map { PointF(it.x.toFloat(), it.y.toFloat()) }

        src.release()
        srcRgb.release()
        mask.release()
        maskCropped.release()
        hierarchy.release()

        return result
    }
}