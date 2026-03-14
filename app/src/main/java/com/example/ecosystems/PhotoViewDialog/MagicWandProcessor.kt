package com.example.ecosystems.PhotoViewDialog

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

object MagicWandProcessor {

    private const val TAG = "MagicWand"

    fun apply(
        bitmap: Bitmap,
        imagePoint: PointF,
        tolerance: Double = 10.0
    ): List<PointF>? {

        if (imagePoint.x < 0 || imagePoint.y < 0 ||
            imagePoint.x >= bitmap.width || imagePoint.y >= bitmap.height) {
            Log.e(TAG, "Точка вне изображения")
            return null
        }

        val scaleFactor = calculateScaleFactor(bitmap.width, bitmap.height, maxSize = 1500)
        val scaledBitmap = if (scaleFactor < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scaleFactor).toInt(),
                (bitmap.height * scaleFactor).toInt(),
                true
            )
        } else bitmap

        val scaledPoint = PointF(imagePoint.x * scaleFactor, imagePoint.y * scaleFactor)

        val result = segment(scaledBitmap, scaledPoint, tolerance)

        if (scaledBitmap !== bitmap) scaledBitmap.recycle()
        return result?.map { PointF(it.x / scaleFactor, it.y / scaleFactor) }
    }

    private fun segment(bitmap: Bitmap, point: PointF, tolerance: Double): List<PointF>? {

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val srcRgb = Mat()
        Imgproc.cvtColor(src, srcRgb, Imgproc.COLOR_RGBA2RGB)
        src.release()

        val w = srcRgb.cols()
        val h = srcRgb.rows()
        val seedX = point.x.toInt().coerceIn(1, w - 2)
        val seedY = point.y.toInt().coerceIn(1, h - 2)

        // ── Шаг 1: bilateral filter ───────────────────────────────────
        val filtered = Mat()
        Imgproc.bilateralFilter(srcRgb, filtered, 9, 75.0, 75.0)

        // ── Шаг 2: построить маску схожести ──────────────────────────
        val pixelRgb = filtered.get(seedY, seedX)
        val seedR = pixelRgb[0]; val seedG = pixelRgb[1]; val seedB = pixelRgb[2]
        Log.d(TAG, "filtered RGB: R=$seedR G=$seedG B=$seedB")

        val similarMask = buildSimilarityMask(filtered, seedR, seedG, seedB, tolerance)

        // ── Шаг 3: Canny edges — найти границы объектов ───────────────
        val gray = Mat()
        Imgproc.cvtColor(srcRgb, gray, Imgproc.COLOR_RGB2GRAY)
        srcRgb.release()

        // Размыть перед Canny чтобы не было лишних краёв от листьев
        val grayBlurred = Mat()
        Imgproc.GaussianBlur(gray, grayBlurred, Size(5.0, 5.0), 0.0)
        gray.release()

        val edges = Mat()
        // Пороги Canny адаптируем к яркости изображения
        val seedBrightness = (seedR + seedG + seedB) / 3.0
        val cannyLow  = (seedBrightness * 0.3).coerceIn(10.0, 50.0)
        val cannyHigh = (seedBrightness * 1.2).coerceIn(40.0, 150.0)
        Imgproc.Canny(grayBlurred, edges, cannyLow, cannyHigh)
        grayBlurred.release()
        filtered.release()

        Log.d(TAG, "Canny thresholds: low=$cannyLow, high=$cannyHigh")

        // ── Шаг 4: "стены" из краёв — края блокируют заливку ─────────
        // Расширить края чтобы они были непрерывными барьерами
        val edgesDilated = Mat()
        val edgeKernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT, Size(3.0, 3.0)
        )
        Imgproc.dilate(edges, edgesDilated, edgeKernel)
        edgeKernel.release()
        edges.release()

        // Вычесть края из маски схожести — убрать пиксели на границах
        val edgesInverted = Mat()
        Core.bitwise_not(edgesDilated, edgesInverted)
        edgesDilated.release()

        val maskedBySimilarity = Mat()
        Core.bitwise_and(similarMask, edgesInverted, maskedBySimilarity)
        similarMask.release()
        edgesInverted.release()

        val nonZero = Core.countNonZero(maskedBySimilarity)
        Log.d(TAG, "after edge masking: $nonZero px")

        if (nonZero < 100) {
            Log.w(TAG, "После edge masking осталось мало пикселей")
            maskedBySimilarity.release()
            return null
        }

        // ── Шаг 5: морфология ─────────────────────────────────────────
        // Меньшее ядро close — не перепрыгивать через края
        val kernelClose = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(11.0, 11.0)  // было 19
        )
        val morphed = Mat()
        Imgproc.morphologyEx(maskedBySimilarity, morphed, Imgproc.MORPH_CLOSE, kernelClose)
        kernelClose.release()

        val kernelOpen = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0)
        )
        Imgproc.morphologyEx(morphed, morphed, Imgproc.MORPH_OPEN, kernelOpen)
        kernelOpen.release()
        maskedBySimilarity.release()

        // ── Шаг 6: связная область у точки нажатия ────────────────────
        val labels = Mat()
        Imgproc.connectedComponents(morphed, labels, 8, CvType.CV_32S)
        morphed.release()

        var targetLabel = labels.get(seedY, seedX)?.get(0)?.toInt() ?: 0
        if (targetLabel == 0) {
            targetLabel = findNearestLabel(labels, seedX, seedY, w, h)
        }
        Log.d(TAG, "targetLabel: $targetLabel")

        if (targetLabel == 0) {
            labels.release()
            return null
        }

        val connectedMask = Mat.zeros(h, w, CvType.CV_8UC1)
        Core.compare(labels, Scalar(targetLabel.toDouble()), connectedMask, Core.CMP_EQ)
        labels.release()

        val regionSize = Core.countNonZero(connectedMask)
        Log.d(TAG, "connected region: $regionSize px")

        if (regionSize < 100) {
            connectedMask.release()
            return null
        }

        return maskToPolygon(connectedMask)
    }

    /**
     * Построить маску пикселей похожих на seed цвет.
     * Используем взвешенное расстояние в RGB — больший вес для
     * относительного соотношения каналов (не абсолютной яркости).
     * Это позволяет выделять тёмную зелень так же как светлую.
     */
    private fun buildSimilarityMask(
        src: Mat,
        seedR: Double,
        seedG: Double,
        seedB: Double,
        tolerance: Double
    ): Mat {
        val h = src.rows()
        val w = src.cols()
        val mask = Mat.zeros(h, w, CvType.CV_8UC1)

        // Нормализовать seed цвет (избавиться от влияния яркости)
        val seedSum = seedR + seedG + seedB + 1e-6
        val seedRn = seedR / seedSum
        val seedGn = seedG / seedSum
        val seedBn = seedB / seedSum

        // Яркость seed пикселя
        val seedBrightness = seedSum / 3.0

        // Адаптивный порог: для тёмных пикселей допускаем больше разброса
        val brightnessScale = when {
            seedBrightness < 30  -> 3.5   // очень тёмный
            seedBrightness < 60  -> 2.5   // тёмный
            seedBrightness < 120 -> 1.5   // средний
            else                 -> 1.0   // светлый
        }

        val colorTol   = tolerance / 100.0 * brightnessScale
        val brightTol  = tolerance * brightnessScale * 2.5

        Log.d(TAG, "seedNorm: Rn=${"%.3f".format(seedRn)} Gn=${"%.3f".format(seedGn)} Bn=${"%.3f".format(seedBn)}")
        Log.d(TAG, "brightness=${"%.1f".format(seedBrightness)}, scale=$brightnessScale, colorTol=${"%.4f".format(colorTol)}, brightTol=${"%.1f".format(brightTol)}")

        // Обработать через Core.inRange + нормализованные каналы
        // Разделить каналы
        val channels = mutableListOf<Mat>()
        Core.split(src, channels)
        val r = channels[0]; val g = channels[1]; val b = channels[2]

        // Float версии для нормализованных вычислений
        val rF = Mat(); val gF = Mat(); val bF = Mat()
        r.convertTo(rF, CvType.CV_32F)
        g.convertTo(gF, CvType.CV_32F)
        b.convertTo(bF, CvType.CV_32F)
        r.release(); g.release(); b.release()

        // sum = R + G + B + 1
        val sumF = Mat()
        Core.add(rF, gF, sumF)
        Core.add(sumF, bF, sumF)
        Core.add(sumF, Scalar(1.0), sumF)

        // Нормализованные каналы
        val rnF = Mat(); val gnF = Mat(); val bnF = Mat()
        Core.divide(rF, sumF, rnF)
        Core.divide(gF, sumF, gnF)
        Core.divide(bF, sumF, bnF)
        rF.release(); gF.release(); bF.release()

        // Разница нормализованных каналов от seed
        val diffR = Mat(); val diffG = Mat(); val diffB = Mat()
        Core.absdiff(rnF, Scalar(seedRn), diffR)
        Core.absdiff(gnF, Scalar(seedGn), diffG)
        Core.absdiff(bnF, Scalar(seedBn), diffB)
        rnF.release(); gnF.release(); bnF.release()

        // Маска по нормализованному цвету — конвертировать в CV_8UC1
        val colorMaskR = Mat(); val colorMaskG = Mat(); val colorMaskB = Mat()
        val colorMaskR32 = Mat(); val colorMaskG32 = Mat(); val colorMaskB32 = Mat()
        Imgproc.threshold(diffR, colorMaskR32, colorTol, 255.0, Imgproc.THRESH_BINARY_INV)
        Imgproc.threshold(diffG, colorMaskG32, colorTol, 255.0, Imgproc.THRESH_BINARY_INV)
        Imgproc.threshold(diffB, colorMaskB32, colorTol, 255.0, Imgproc.THRESH_BINARY_INV)
        diffR.release(); diffG.release(); diffB.release()

        // Привести к CV_8UC1
        colorMaskR32.convertTo(colorMaskR, CvType.CV_8UC1)
        colorMaskG32.convertTo(colorMaskG, CvType.CV_8UC1)
        colorMaskB32.convertTo(colorMaskB, CvType.CV_8UC1)
        colorMaskR32.release(); colorMaskG32.release(); colorMaskB32.release()

        // Объединить: пиксель подходит если все 3 канала в допуске
        val colorMatch = Mat()
        Core.bitwise_and(colorMaskR, colorMaskG, colorMatch)
        Core.bitwise_and(colorMatch, colorMaskB, colorMatch)
        colorMaskR.release(); colorMaskG.release(); colorMaskB.release()

        // Маска по яркости
        val brightnessMat = Mat()
        Core.divide(sumF, Scalar(3.0), brightnessMat)
        sumF.release()

         // Конвертировать в CV_8UC1 через промежуточный шаг
        val brightnessClipped = Mat()
        Core.min(brightnessMat, Scalar(255.0), brightnessClipped)
        Core.max(brightnessClipped, Scalar(0.0), brightnessClipped)
        val brightness8u = Mat()
        brightnessClipped.convertTo(brightness8u, CvType.CV_8UC1)
        brightnessMat.release()
        brightnessClipped.release()

        val brightLow  = (seedBrightness - brightTol).coerceIn(0.0, 255.0)
        val brightHigh = (seedBrightness + brightTol).coerceIn(0.0, 255.0)
        val brightMatch = Mat()
        Core.inRange(brightness8u, Scalar(brightLow), Scalar(brightHigh), brightMatch)
        brightness8u.release()

        // Итог: оба операнда теперь CV_8UC1
        Core.bitwise_and(colorMatch, brightMatch, mask)
        colorMatch.release(); brightMatch.release()

        return mask
    }

    private fun maskToPolygon(mask: Mat): List<PointF>? {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            mask, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        mask.release()
        hierarchy.release()

        Log.d(TAG, "contours: ${contours.size}")
        if (contours.isEmpty()) return null

        val target = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return null
        val area = Imgproc.contourArea(target)
        Log.d(TAG, "target area: $area")
        if (area < 50.0) return null

        val contour2f = MatOfPoint2f(*target.toArray())
        val epsilon = 0.003 * Imgproc.arcLength(contour2f, true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(contour2f, approx, epsilon, true)

        Log.d(TAG, "polygon points: ${approx.rows()}")
        if (approx.rows() < 3) return null

        return approx.toArray().map { PointF(it.x.toFloat(), it.y.toFloat()) }
    }

    private fun findNearestLabel(labels: Mat, seedX: Int, seedY: Int, w: Int, h: Int): Int {
        for (radius in 1..30) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (abs(dx) != radius && abs(dy) != radius) continue
                    val nx = (seedX + dx).coerceIn(0, w - 1)
                    val ny = (seedY + dy).coerceIn(0, h - 1)
                    val label = labels.get(ny, nx)?.get(0)?.toInt() ?: 0
                    if (label > 0) return label
                }
            }
        }
        return 0
    }

    private fun calculateScaleFactor(width: Int, height: Int, maxSize: Int): Float {
        val maxDim = maxOf(width, height)
        return if (maxDim > maxSize) maxSize.toFloat() / maxDim else 1.0f
    }
}
