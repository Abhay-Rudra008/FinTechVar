package com.rudra.fintechvar.utils

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX



object Permissions {

    fun checkImagePermissions(
        activity: AppCompatActivity,
        onGranted: () -> Unit
    ) {
        val permissions = buildPermissions()

        PermissionX.init(activity)
            .permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "We need these permissions to select and crop an image",
                    "Allow",
                    "Cancel"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Please allow permissions in settings to continue",
                    "Settings",
                    "Cancel"
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    onGranted()
                } else {
                    Toast.makeText(
                        activity,
                        "Permissions denied: $deniedList",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    fun checkNotificationPermission(
        activity: AppCompatActivity,
        onGranted: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionX.init(activity)
                .permissions(Manifest.permission.POST_NOTIFICATIONS)
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(
                        deniedList,
                        "We need notification permission to send you daily and weekly expense reports.",
                        "Allow",
                        "Cancel"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        "Notification permission is permanently denied. Please enable it from settings to receive reports.",
                        "Open Settings",
                        "Cancel"
                    )
                }
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        onGranted()
                    }
                }
        } else {
            onGranted()
        }
    }

    private fun buildPermissions(): List<String> {
        val permissions = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return permissions
    }
}