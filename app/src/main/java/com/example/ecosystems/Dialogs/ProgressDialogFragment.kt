package com.example.ecosystems.Dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.ecosystems.R

class ProgressDialogFragment(
    private val textTemplate: String = "Прогресс скачивания данных ГИС объектов: %d/%d") : DialogFragment() {

    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null

    // Запоминаем последние значения на случай вызова до инициализации View
    private var pendingCurrent: Int = 0
    private var pendingMax: Int = 0
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_progress, null)

        progressBar = view.findViewById(R.id.progressBarHorizontal)
        progressText = view.findViewById(R.id.progressText)

        applyProgress(pendingCurrent, pendingMax)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
    }

    fun updateProgress(current: Int, max: Int) {
        pendingCurrent = current
        pendingMax = max

        if (progressBar != null) {
            applyProgress(current, max)
        }
    }

    private fun applyProgress(current: Int, max: Int) {
        progressBar?.max = max
        progressBar?.progress = current
        progressText?.text = String.format(textTemplate, current, max)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressBar = null
        progressText = null
    }
}