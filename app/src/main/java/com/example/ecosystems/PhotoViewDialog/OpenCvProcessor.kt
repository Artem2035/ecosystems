package com.example.ecosystems.PhotoViewDialog

import android.graphics.Bitmap
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object OpenCvProcessor {

    /**
     * Нарисовать прямоугольники на изображении через OpenCV
     * @param bitmap  — оригинальное изображение
     * @param rects   — прямоугольники в координатах ИЗОБРАЖЕНИЯ (после CoordinateMapper)
     */
    fun drawRectangles(bitmap: Bitmap, rects: List<RectF>): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        rects.forEach { rect ->
            // Гарантируем, что координаты не выходят за границы изображения
            val safeRect = clipRect(rect, mat.cols(), mat.rows())

            Imgproc.rectangle(
                mat,
                Point(safeRect.left.toDouble(), safeRect.top.toDouble()),
                Point(safeRect.right.toDouble(), safeRect.bottom.toDouble()),
                Scalar(0.0, 140.0, 255.0, 255.0),  // синий
                4  // толщина линии
            )
        }

        val result = bitmap.copy(bitmap.config, true)
        Utils.matToBitmap(mat, result)
        mat.release()
        return result
    }

    /**
     * Получить суб-изображение (ROI) по прямоугольнику
     */
    fun cropRegion(bitmap: Bitmap, rect: RectF): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val safeRect = clipRect(rect, mat.cols(), mat.rows())
        val roi = mat.submat(
            safeRect.top.toInt(),
            safeRect.bottom.toInt(),
            safeRect.left.toInt(),
            safeRect.right.toInt()
        )

        val result = Bitmap.createBitmap(roi.cols(), roi.rows(), bitmap.config)
        Utils.matToBitmap(roi, result)
        mat.release()
        return result
    }

    /**
     * Обрезать прямоугольник по границам изображения
     */
    private fun clipRect(rect: RectF, maxWidth: Int, maxHeight: Int) = RectF(
        rect.left.coerceIn(0f, maxWidth.toFloat()),
        rect.top.coerceIn(0f, maxHeight.toFloat()),
        rect.right.coerceIn(0f, maxWidth.toFloat()),
        rect.bottom.coerceIn(0f, maxHeight.toFloat())
    )
}