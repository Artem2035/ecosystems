package com.example.ecosystems.Dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.ecosystems.R

class ProgressDialogFragment : DialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_progress, null)

        progressBar = view.findViewById(R.id.progressBarHorizontal)
        progressText = view.findViewById(R.id.progressText)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
    }

    fun updateProgress(current: Int, max: Int) {
        progressBar.max = max
        progressBar.progress = current
        progressText.text = "Прогресс скачивания данных ГИС объектов: $current/$max"
    }
}