package com.example.ecosystems.utils
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LocationPermissionManager(private val activity: AppCompatActivity) {

    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.values.any { it } // хотя бы COARSE
            if (granted) {
                onGranted?.invoke()
            } else {
                handleDenied()
            }
        }

    /* Проверить и запросить разрешения */
    fun requestIfNeeded(
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        this.onGranted = onGranted
        this.onDenied = onDenied

        if (hasPermission()) {
            onGranted()
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    fun hasPermission(): Boolean =
        LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(activity, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun handleDenied() {
        // Пользователь нажал "Никогда не спрашивать" — ведём в настройки
        val showRationale = LOCATION_PERMISSIONS.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }

        if (!showRationale) {
            // Разрешение заблокировано навсегда
            showSettingsDialog()
        } else {
            // Просто отказал — показываем объяснение и повторный запрос
            showRationaleDialog()
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Разрешение на геолокацию")
            .setMessage("Для отображения вашего местоположения на карте необходим доступ к геолокации.")
            .setPositiveButton("Разрешить") { _, _ ->
                permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
            .setNegativeButton("Отмена") { _, _ ->
                onDenied?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Геолокация отключена")
            .setMessage("Разрешение было запрещено. Некоторые функции приложения могут не работать! Включите его вручную в настройках приложения.")
            .setPositiveButton("Настройки") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Отмена") { _, _ ->
                onDenied?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}