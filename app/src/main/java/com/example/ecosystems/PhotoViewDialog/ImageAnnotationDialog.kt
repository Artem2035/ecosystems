package com.example.ecosystems.PhotoViewDialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.ecosystems.R
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.button.MaterialButton
import org.opencv.android.OpenCVLoader

class ImageAnnotationDialog(
    context: Context,
    private val imageUri: Uri,
    private val onConfirm: (Bitmap) -> Unit
) : Dialog(context) {

    private lateinit var photoView: PhotoView
    private lateinit var overlayView: DrawingOverlayView
    private lateinit var btnNavigate: MaterialButton
    private lateinit var btnRectangle: MaterialButton
    private lateinit var btnPolygon: MaterialButton
    private lateinit var btnMagicWand: MaterialButton
    private var wandTolerance: Float = 30f

    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Используем XML разметку
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_photo_view, null)
        setContentView(view)

        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Находим views
        photoView = view.findViewById(R.id.photoView)
        overlayView = view.findViewById(R.id.overlayView)
        btnNavigate = view.findViewById(R.id.btnNavigate)
        btnRectangle = view.findViewById(R.id.btnRectangle)
        btnPolygon = view.findViewById(R.id.btnPolygon)
        btnMagicWand = view.findViewById(R.id.btnMagicWand)

        val btnUndo = view.findViewById<MaterialButton>(R.id.btnUndo)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnClose = view.findViewById<Button>(R.id.btnClose)
        // Кнопка удалить выбранный
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            overlayView.deleteSelected()
        }

        // Начальное состояние
        btnNavigate.alpha = 1f
        btnRectangle.alpha = 0.5f

        // Кнопки
        btnClose.setOnClickListener { dismiss() }

        btnNavigate.setOnClickListener {
            setMode(DrawingOverlayView.Mode.NAVIGATE)
        }

        btnRectangle.setOnClickListener {
            setMode(DrawingOverlayView.Mode.RECTANGLE)
        }
        btnPolygon.setOnClickListener {
            setMode(DrawingOverlayView.Mode.POLYGON)
        }

        btnMagicWand.setOnClickListener {
            setMode(DrawingOverlayView.Mode.MAGIC_WAND)
        }

        btnUndo.setOnClickListener {
            overlayView.undo()
        }

        btnConfirm.setOnClickListener {
            confirmAndClose()
        }

        initOpenCv()
        loadImage()
        subscribeToMatrixChanges()
    }

    private fun subscribeToMatrixChanges() {
        photoView.setOnMatrixChangeListener {
            overlayView.updateImageMatrix(photoView.imageMatrix)
        }
        // Автопереход в навигацию после рисования прямоугольника
        overlayView.onRectangleDrawn = {
            setMode(DrawingOverlayView.Mode.NAVIGATE)
        }
        overlayView.onPolygonDrawn = {
            setMode(DrawingOverlayView.Mode.NAVIGATE)
        }
        overlayView.onMagicWandTap = { imagePoint ->
            originalBitmap?.let { bitmap ->
                Thread {
                    val polygonPoints = MagicWandProcessor.apply(
                        bitmap = bitmap,
                        imagePoint = imagePoint,
                        tolerance = wandTolerance.toDouble()
                    )
                    overlayView.post {
                        if (polygonPoints != null) {
                            overlayView.addPolygon(polygonPoints)
                            setMode(DrawingOverlayView.Mode.NAVIGATE)
                        } else {
                            Toast.makeText(context, "Не удалось выделить область", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
    }

    private fun setMode(mode: DrawingOverlayView.Mode) {
        overlayView.mode = mode
        btnNavigate.alpha = if (mode == DrawingOverlayView.Mode.NAVIGATE) 1f else 0.5f
        btnRectangle.alpha = if (mode == DrawingOverlayView.Mode.RECTANGLE) 1f else 0.5f
        btnPolygon.alpha   = if (mode == DrawingOverlayView.Mode.POLYGON)    1f else 0.5f
        btnMagicWand.alpha = if (mode == DrawingOverlayView.Mode.MAGIC_WAND) 1f else 0.5f
        // isZoomable НЕ трогаем — сбрасывает зум
    }

    private fun initOpenCv() {
        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(context, "OpenCV не инициализирован", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImage() {
        Glide.with(context)
            .asBitmap()
            .load(imageUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    originalBitmap = resource
                    photoView.setImageBitmap(resource)
                    photoView.post {
                        overlayView.updateImageMatrix(photoView.imageMatrix)
                    }
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
    }

    private fun confirmAndClose() {
        val bitmap = originalBitmap ?: run {
            Toast.makeText(context, "Изображение ещё не загружено", Toast.LENGTH_SHORT).show()
            return
        }

        val imageRects = overlayView.getRectangles()
        if (imageRects.isEmpty()) {
            Toast.makeText(context, "Добавьте хотя бы один прямоугольник", Toast.LENGTH_SHORT).show()
            return
        }

        val annotated = OpenCvProcessor.drawRectangles(bitmap, imageRects)
        onConfirm(annotated)
        dismiss()
    }
}