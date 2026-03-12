package com.example.ecosystems.PhotoViewDialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode { NAVIGATE, RECTANGLE, POLYGON, MAGIC_WAND }
    var mode: Mode = Mode.NAVIGATE
    var onMagicWandTap: ((imagePoint: PointF) -> Unit)? = null

    private enum class SelectType { RECT, POLYGON }
    private var selectedType = SelectType.RECT

    var onRectangleDrawn: (() -> Unit)? = null

    // Матрица PhotoView
    private var imageMatrix: Matrix = Matrix()
    private val inverseMatrix: Matrix = Matrix()

    // ── Данные ────────────────────────────────────────────────────
    private val imageRects = mutableListOf<RectF>()
    private var selectedIndex: Int = -1  // индекс выбранного прямоугольника

    // ── Состояние touch ───────────────────────────────────────────
    private enum class TouchAction { NONE, DRAWING, MOVING, RESIZING, MOVING_VERTEX }
    private var touchAction = TouchAction.NONE
    // Индекс перетаскиваемой вершины полигона
    private var movingVertexIndex: Int = -1


    // Рисование нового прямоугольника
    private var drawStartImage: PointF? = null
    private var drawCurrentImage: PointF? = null

    // Перемещение
    private var moveLastImage: PointF? = null

    // Изменение размера
    private var resizingCorner: Corner = Corner.NONE
    private enum class Corner { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    // ── Размеры маркеров ──────────────────────────────────────────
    private val cornerHandleRadius = 20f      // радиус угловых маркеров (px экрана)
    private val touchSlop = 30f               // зона захвата угла/центра

    // Текущий рисуемый полигон (точки в координатах изображения)
    private val currentPolygonPoints = mutableListOf<PointF>()
    // Сохранённые полигоны
    private val imagePolygons = mutableListOf<List<PointF>>()
    // Колбэк завершения полигона
    var onPolygonDrawn: (() -> Unit)? = null
    // Минимальное расстояние до первой точки для замыкания (px экрана)
    private val closePolygonRadius = 40f

    // ── Кисти ─────────────────────────────────────────────────────
    private val paintFill = Paint().apply {
        color = Color.argb(50, 0, 140, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintStroke = Paint().apply {
        color = Color.argb(220, 0, 140, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val paintSelected = Paint().apply {
        color = Color.argb(220, 0, 220, 100)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
    }
    private val paintSelectedFill = Paint().apply {
        color = Color.argb(70, 0, 220, 100)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintCornerFill = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintCornerStroke = Paint().apply {
        color = Color.argb(220, 0, 140, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val paintCornerSelectedStroke = Paint().apply {
        color = Color.argb(220, 0, 220, 100)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val paintActiveStroke = Paint().apply {
        color = Color.argb(255, 0, 200, 100)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val paintDeleteBg = Paint().apply {
        color = Color.argb(220, 220, 50, 50)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintDeleteText = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    // ── Матрица ───────────────────────────────────────────────────
    fun updateImageMatrix(matrix: Matrix) {
        imageMatrix.set(matrix)
        imageMatrix.invert(inverseMatrix)
        invalidate()
    }

    private fun screenToImage(x: Float, y: Float): PointF {
        val pts = floatArrayOf(x, y)
        inverseMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }

    private fun imageRectToScreen(rect: RectF): RectF {
        val pts = floatArrayOf(rect.left, rect.top, rect.right, rect.bottom)
        imageMatrix.mapPoints(pts)
        return RectF(
            minOf(pts[0], pts[2]), minOf(pts[1], pts[3]),
            maxOf(pts[0], pts[2]), maxOf(pts[1], pts[3])
        )
    }

    private fun imageToScreen(point: PointF): PointF {
        val pts = floatArrayOf(point.x, point.y)
        imageMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }

    // ── Touch ──────────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        return when (mode) {
            Mode.RECTANGLE -> handleRectangleMode(event, x, y)
            Mode.POLYGON   -> handlePolygonMode(event, x, y)
            Mode.NAVIGATE  -> handleNavigateMode(event, x, y)
            Mode.MAGIC_WAND -> handleMagicWandMode(event, x, y)
        }
    }

    // ── Режим рисования ───────────────────────────────────────────
    private fun handleRectangleMode(event: MotionEvent, x: Float, y: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                selectedIndex = -1
                drawStartImage = screenToImage(x, y)
                drawCurrentImage = screenToImage(x, y)
                touchAction = TouchAction.DRAWING
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                drawCurrentImage = screenToImage(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.action == MotionEvent.ACTION_UP) {
                    val start = drawStartImage
                    if (start != null) {
                        val end = screenToImage(x, y)
                        val rect = makeRect(start, end)
                        if (rect.width() > 5f && rect.height() > 5f) {
                            imageRects.add(rect)
                            selectedIndex = imageRects.lastIndex
                            onRectangleDrawn?.invoke()
                        }
                    }
                }
                drawStartImage = null
                drawCurrentImage = null
                touchAction = TouchAction.NONE
                invalidate()
                return true
            }
        }
        return false
    }

    // ── Режим навигации (выбор / перемещение / ресайз) ────────────
    private fun handleNavigateMode(event: MotionEvent, x: Float, y: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 0. Кнопка удаления
                if (selectedIndex >= 0 && tryDeleteAt(x, y)) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }

                // 1. Угловые маркеры прямоугольника
                if (selectedIndex >= 0 && selectedType == SelectType.RECT) {
                    val corner = hitTestCorners(selectedIndex, x, y)
                    if (corner != Corner.NONE) {
                        resizingCorner = corner
                        touchAction = TouchAction.RESIZING
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
                if (selectedIndex >= 0 && selectedType == SelectType.POLYGON) {
                    val vertexIndex = hitTestPolygonVertex(selectedIndex, x, y)
                    if (vertexIndex >= 0) {
                        movingVertexIndex = vertexIndex
                        touchAction = TouchAction.MOVING_VERTEX
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }

                // 2. Клик внутри выбранной фигуры — перемещение
                if (selectedIndex >= 0) {
                    val hit = when (selectedType) {
                        SelectType.RECT    -> hitTestRect(selectedIndex, x, y)
                        SelectType.POLYGON -> hitTestPolygon(selectedIndex, x, y)
                    }
                    if (hit) {
                        touchAction = TouchAction.MOVING
                        moveLastImage = screenToImage(x, y)
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }

                // 3. Клик по любому прямоугольнику
                val hitRect = findRectAt(x, y)
                if (hitRect >= 0) {
                    selectedIndex = hitRect
                    selectedType = SelectType.RECT
                    touchAction = TouchAction.MOVING
                    moveLastImage = screenToImage(x, y)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }

                // 4. Клик по любому полигону
                val hitPoly = findPolygonAt(x, y)
                if (hitPoly >= 0) {
                    selectedIndex = hitPoly
                    selectedType = SelectType.POLYGON
                    touchAction = TouchAction.MOVING
                    moveLastImage = screenToImage(x, y)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }

                // 5. Пустота — снять выделение
                selectedIndex = -1
                touchAction = TouchAction.NONE
                invalidate()
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                when (touchAction) {
                    TouchAction.MOVING -> {
                        val imgPoint = screenToImage(x, y)
                        val last = moveLastImage ?: return true
                        val dx = imgPoint.x - last.x
                        val dy = imgPoint.y - last.y

                        when (selectedType) {
                            SelectType.RECT -> {
                                if (selectedIndex >= 0) imageRects[selectedIndex].offset(dx, dy)
                            }
                            SelectType.POLYGON -> {
                                if (selectedIndex >= 0) {
                                    val moved = imagePolygons[selectedIndex].map {
                                        PointF(it.x + dx, it.y + dy)
                                    }
                                    imagePolygons[selectedIndex] = moved
                                }
                            }
                        }
                        moveLastImage = imgPoint
                        invalidate()
                        return true
                    }
                    TouchAction.RESIZING -> {
                        if (selectedIndex >= 0) {
                            val imgPoint = screenToImage(x, y)
                            resizeRect(selectedIndex, resizingCorner, imgPoint)
                            invalidate()
                        }
                        return true
                    }
                    TouchAction.MOVING_VERTEX -> {
                        if (selectedIndex >= 0 && movingVertexIndex >= 0) {
                            val imgPoint = screenToImage(x, y)
                            // Заменить конкретную вершину
                            val points = imagePolygons[selectedIndex].toMutableList()
                            points[movingVertexIndex] = imgPoint
                            imagePolygons[selectedIndex] = points
                            invalidate()
                        }
                        return true
                    }
                    else -> return false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchAction != TouchAction.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    touchAction = TouchAction.NONE
                    moveLastImage = null
                    resizingCorner = Corner.NONE
                    movingVertexIndex = -1
                    invalidate()
                    return true
                }
                return false
            }
        }
        return false
    }

    private fun handlePolygonMode(event: MotionEvent, x: Float, y: Float): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        parent?.requestDisallowInterceptTouchEvent(true)

        val imgPoint = screenToImage(x, y)

        // Если точек >= 3 — проверить замыкание (тап рядом с первой точкой)
        if (currentPolygonPoints.size >= 3) {
            val firstScreen = imageToScreen(currentPolygonPoints.first())
            val dist = Math.hypot(
                (x - firstScreen.x).toDouble(),
                (y - firstScreen.y).toDouble()
            )
            if (dist <= closePolygonRadius) {
                // Замкнуть полигон
                imagePolygons.add(currentPolygonPoints.toList())
                currentPolygonPoints.clear()
                invalidate()
                onPolygonDrawn?.invoke()
                return true
            }
        }

        // Добавить новую точку
        currentPolygonPoints.add(imgPoint)
        invalidate()
        return true
    }

    // ── Hit testing ───────────────────────────────────────────────

    /** Найти индекс прямоугольника под точкой (с конца — верхний сначала) */
    private fun findRectAt(screenX: Float, screenY: Float): Int {
        for (i in imageRects.indices.reversed()) {
            if (hitTestRect(i, screenX, screenY)) return i
        }
        return -1
    }

    /** Попадает ли точка экрана в прямоугольник i */
    private fun hitTestRect(index: Int, screenX: Float, screenY: Float): Boolean {
        val screenRect = imageRectToScreen(imageRects[index])
        return screenRect.contains(screenX, screenY)
    }

    /** Определить угол прямоугольника под точкой экрана */
    private fun hitTestCorners(index: Int, screenX: Float, screenY: Float): Corner {
        val sr = imageRectToScreen(imageRects[index])
        val corners = mapOf(
            Corner.TOP_LEFT     to PointF(sr.left, sr.top),
            Corner.TOP_RIGHT    to PointF(sr.right, sr.top),
            Corner.BOTTOM_LEFT  to PointF(sr.left, sr.bottom),
            Corner.BOTTOM_RIGHT to PointF(sr.right, sr.bottom)
        )
        corners.forEach { (corner, point) ->
            val dist = Math.hypot(
                (screenX - point.x).toDouble(),
                (screenY - point.y).toDouble()
            )
            if (dist <= touchSlop) return corner
        }
        return Corner.NONE
    }

    private fun findPolygonAt(screenX: Float, screenY: Float): Int {
        for (i in imagePolygons.indices.reversed()) {
            if (hitTestPolygon(i, screenX, screenY)) return i
        }
        return -1
    }

    private fun hitTestPolygon(index: Int, screenX: Float, screenY: Float): Boolean {
        val screenPoints = imagePolygons[index].map { imageToScreen(it) }
        val path = Path()
        screenPoints.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        val region = android.graphics.Region()
        val bounds = android.graphics.RectF()
        path.computeBounds(bounds, true)
        region.setPath(
            path,
            android.graphics.Region(
                bounds.left.toInt(), bounds.top.toInt(),
                bounds.right.toInt(), bounds.bottom.toInt()
            )
        )
        return region.contains(screenX.toInt(), screenY.toInt())
    }

    /** Найти индекс вершины полигона под точкой экрана, -1 если не найдено */
    private fun hitTestPolygonVertex(polygonIndex: Int, screenX: Float, screenY: Float): Int {
        val points = imagePolygons[polygonIndex]
        points.forEachIndexed { i, imgPoint ->
            val screenPoint = imageToScreen(imgPoint)
            val dist = Math.hypot(
                (screenX - screenPoint.x).toDouble(),
                (screenY - screenPoint.y).toDouble()
            )
            if (dist <= touchSlop) return i
        }
        return -1
    }

    private fun handleMagicWandMode(event: MotionEvent, x: Float, y: Float): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        parent?.requestDisallowInterceptTouchEvent(true)
        // Передать точку в координатах изображения наружу — в Dialog
        val imgPoint = screenToImage(x, y)
        onMagicWandTap?.invoke(imgPoint)
        return true
    }

    // ── Изменение размера ─────────────────────────────────────────
    private fun resizeRect(index: Int, corner: Corner, imgPoint: PointF) {
        val rect = imageRects[index]
        val minSize = 10f
        when (corner) {
            Corner.TOP_LEFT -> {
                rect.left = minOf(imgPoint.x, rect.right - minSize)
                rect.top  = minOf(imgPoint.y, rect.bottom - minSize)
            }
            Corner.TOP_RIGHT -> {
                rect.right = maxOf(imgPoint.x, rect.left + minSize)
                rect.top   = minOf(imgPoint.y, rect.bottom - minSize)
            }
            Corner.BOTTOM_LEFT -> {
                rect.left   = minOf(imgPoint.x, rect.right - minSize)
                rect.bottom = maxOf(imgPoint.y, rect.top + minSize)
            }
            Corner.BOTTOM_RIGHT -> {
                rect.right  = maxOf(imgPoint.x, rect.left + minSize)
                rect.bottom = maxOf(imgPoint.y, rect.top + minSize)
            }
            Corner.NONE -> {}
        }
    }

    // ── Отрисовка ─────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Прямоугольники
        imageRects.forEachIndexed { index, imageRect ->
            val screenRect = imageRectToScreen(imageRect)
            val isSelected = index == selectedIndex && selectedType == SelectType.RECT
            canvas.drawRect(screenRect, if (isSelected) paintSelectedFill else paintFill)
            canvas.drawRect(screenRect, if (isSelected) paintSelected else paintStroke)
            drawCornerHandles(canvas, screenRect, isSelected)
            if (isSelected) drawDeleteButton(canvas, screenRect.right, screenRect.top)
        }

        // Сохранённые полигоны
        imagePolygons.forEachIndexed { index, points ->
            val isSelected = index == selectedIndex && selectedType == SelectType.POLYGON
            val screenPoints = points.map { imageToScreen(it) }
            drawPolygon(canvas, screenPoints, isSelected)
            if (isSelected) {
                val topRight = screenPoints.minByOrNull { it.y }
                    ?: screenPoints.first()
                drawDeleteButton(canvas, topRight.x, topRight.y)
            }
        }

        // Текущий рисуемый прямоугольник
        val startImg = drawStartImage
        val curImg = drawCurrentImage
        if (startImg != null && curImg != null) {
            val activeScreenRect = imageRectToScreen(makeRect(startImg, curImg))
            canvas.drawRect(activeScreenRect, paintFill)
            canvas.drawRect(activeScreenRect, paintActiveStroke)
            drawCornerHandles(canvas, activeScreenRect, false)
        }

        // Текущий рисуемый полигон
        if (currentPolygonPoints.isNotEmpty()) {
            val screenPoints = currentPolygonPoints.map { imageToScreen(it) }
            drawPolygonInProgress(canvas, screenPoints)
        }
    }

    private fun drawPolygon(canvas: Canvas, screenPoints: List<PointF>, isSelected: Boolean) {
        if (screenPoints.size < 2) return
        val path = Path()
        screenPoints.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        canvas.drawPath(path, if (isSelected) paintSelectedFill else paintFill)
        canvas.drawPath(path, if (isSelected) paintSelected else paintStroke)

        if (isSelected) {
            screenPoints.forEachIndexed { i, p ->
                // Подсветить перетаскиваемую вершину
                val isMoving = i == movingVertexIndex
                canvas.drawCircle(p.x, p.y, cornerHandleRadius,
                    if (isMoving) paintDeleteBg else paintCornerFill)
                canvas.drawCircle(p.x, p.y, cornerHandleRadius, paintCornerSelectedStroke)
            }
        }
    }

    private fun drawPolygonInProgress(canvas: Canvas, screenPoints: List<PointF>) {
        // Линии между точками
        val path = Path()
        screenPoints.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        canvas.drawPath(path, paintActiveStroke)

        // Точки
        screenPoints.forEachIndexed { i, p ->
            // Первая точка — зелёный кружок (зона замыкания)
            val paint = if (i == 0) paintDeleteBg else paintCornerFill
            canvas.drawCircle(p.x, p.y, cornerHandleRadius, paint)
            canvas.drawCircle(p.x, p.y, cornerHandleRadius, paintCornerSelectedStroke)
        }

        // Подсказка на первой точке если >= 3 точек
        if (screenPoints.size >= 3) {
            paintDeleteText.textSize = 20f
            canvas.drawText("✓", screenPoints.first().x, screenPoints.first().y + 8f, paintDeleteText)
            paintDeleteText.textSize = 28f
        }
    }

    private fun drawCornerHandles(canvas: Canvas, rect: RectF, selected: Boolean) {
        val stroke = if (selected) paintCornerSelectedStroke else paintCornerStroke
        listOf(
            PointF(rect.left, rect.top),
            PointF(rect.right, rect.top),
            PointF(rect.left, rect.bottom),
            PointF(rect.right, rect.bottom)
        ).forEach { p ->
            canvas.drawCircle(p.x, p.y, cornerHandleRadius, paintCornerFill)
            canvas.drawCircle(p.x, p.y, cornerHandleRadius, stroke)
        }
    }

    /** Кнопка удаления — красный кружок с ✕ в правом верхнем углу */
    private fun drawDeleteButton(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawCircle(cx, cy, cornerHandleRadius, paintDeleteBg)
        canvas.drawText("✕", cx, cy + 10f, paintDeleteText)
    }

    private fun makeRect(p1: PointF, p2: PointF) = RectF(
        minOf(p1.x, p2.x), minOf(p1.y, p2.y),
        maxOf(p1.x, p2.x), maxOf(p1.y, p2.y)
    )

    // ── Публичные методы ───────────────────────────────────────────

    /** Проверить тап на кнопку удаления и удалить если попали */
    fun tryDeleteAt(screenX: Float, screenY: Float): Boolean {
        if (selectedIndex < 0) return false

        val (cx, cy) = when (selectedType) {
            SelectType.RECT -> {
                val sr = imageRectToScreen(imageRects[selectedIndex])
                Pair(sr.right, sr.top)
            }
            SelectType.POLYGON -> {
                val points = imagePolygons[selectedIndex].map { imageToScreen(it) }
                val top = points.minByOrNull { it.y } ?: return false
                Pair(top.x, top.y)
            }
        }

        val dist = Math.hypot((screenX - cx).toDouble(), (screenY - cy).toDouble())
        if (dist <= touchSlop) {
            deleteSelected()
            return true
        }
        return false
    }

    fun deleteSelected() {
        when (selectedType) {
            SelectType.RECT -> {
                if (selectedIndex >= 0 && selectedIndex < imageRects.size) {
                    imageRects.removeAt(selectedIndex)
                }
            }
            SelectType.POLYGON -> {
                if (selectedIndex >= 0 && selectedIndex < imagePolygons.size) {
                    imagePolygons.removeAt(selectedIndex)
                }
            }
        }
        selectedIndex = -1
        invalidate()
    }

    fun undo() {
        // Сначала отменить незавершённый полигон
        if (currentPolygonPoints.isNotEmpty()) {
            currentPolygonPoints.removeLast()
            invalidate()
            return
        }
        // Затем последний полигон
        if (imagePolygons.isNotEmpty()) {
            imagePolygons.removeLast()
            invalidate()
            return
        }
        // Затем последний прямоугольник
        if (imageRects.isNotEmpty()) {
            imageRects.removeLast()
            invalidate()
        }
        selectedIndex = -1
    }

    fun clear() {
        imageRects.clear()
        selectedIndex = -1
        drawStartImage = null
        drawCurrentImage = null
        invalidate()
    }

    fun addPolygon(points: List<PointF>) {
        if (points.size >= 3) {
            imagePolygons.add(points)
            selectedIndex = imagePolygons.lastIndex
            selectedType = SelectType.POLYGON
            invalidate()
        }
    }

    fun getRectangles(): List<RectF> = imageRects.toList()

}