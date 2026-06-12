package com.jj.zds.feature.permission.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import com.jj.core.permission.JiangPermission
import com.jj.core.permission.JiangPermissionFlowResult
import com.jj.core.permission.JiangPermissionResult
import com.jj.core.permission.hasJiangPermissions
import com.jj.core.permission.openJiangAppSettings
import com.jj.core.permission.toJiangPermissionResult
import com.jj.core.system.JiangVibrator
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.zds.databinding.ActivityPermissionAccessDemoBinding
import com.jj.zds.feature.message.ZdsMessageSseService
import com.jj.zds.viewmodel.ZdsViewModel

class PermissionAccessDemoActivity :
    JiangToolbarStateVmActivity<ActivityPermissionAccessDemoBinding, ZdsViewModel>() {

    private var pendingItem: PermissionItem? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResult ->
        val item = pendingItem ?: return@registerForActivityResult
        pendingItem = null
        renderPermissionResult(item, toJiangPermissionResult(grantResult))
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityPermissionAccessDemoBinding {
        return ActivityPermissionAccessDemoBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnForegroundService.setOnClickListener {
            requestPermission(PermissionItem.ForegroundService)
        }
        binding.btnCamera.setOnClickListener {
            requestPermission(PermissionItem.Camera)
        }
        binding.btnAlbum.setOnClickListener {
            requestPermission(PermissionItem.Album)
        }
        binding.btnBluetooth.setOnClickListener {
            requestPermission(PermissionItem.Bluetooth)
        }
        binding.btnLocation.setOnClickListener {
            requestPermission(PermissionItem.Location)
        }
        binding.btnVibrate.setOnClickListener {
            requestPermission(PermissionItem.Vibrate)
        }
        binding.btnOpenSettings.setOnClickListener {
            openJiangAppSettings()
        }
        renderOverview()
    }

    override fun toolbarTitle(): CharSequence = "Permission Demo"

    override fun toolbarSubtitle(): CharSequence = "request / grant / deny"

    override fun enableStateLayout(): Boolean = false

    private fun requestPermission(item: PermissionItem) {
        val permissions = item.permissions()
        if (permissions.isEmpty()) {
            runPermissionAction(item, "No runtime permission required on this Android version.")
            return
        }

        if (hasJiangPermissions(permissions)) {
            runPermissionAction(item, "Already granted.")
            return
        }

        pendingItem = item
        showConfirmDialog(
            title = "Request ${item.label}",
            message = item.reason,
            confirmText = "Ask",
            cancelText = "Cancel",
            onConfirm = {
                binding.tvResult.text = "${item.label}: asking permission..."
                permissionLauncher.launch(permissions.toTypedArray())
            },
            onCancel = {
                pendingItem = null
                binding.tvResult.text = "${item.label}: request cancelled."
            },
        )
    }

    private fun renderPermissionResult(
        item: PermissionItem,
        result: JiangPermissionResult,
    ) {
        when (val flowResult = JiangPermissionFlowResult.fromPermissionResult(result)) {
            JiangPermissionFlowResult.Granted -> runPermissionAction(item, "Granted.")
            is JiangPermissionFlowResult.Denied -> renderDenied(
                item = item,
                status = "Denied.",
                permissions = flowResult.permissions,
            )

            is JiangPermissionFlowResult.DeniedForever -> renderDenied(
                item = item,
                status = "Denied forever. Open app settings to enable.",
                permissions = flowResult.permissions,
            )
        }
    }

    private fun runPermissionAction(item: PermissionItem, status: String) {
        val actionResult = when (item) {
            PermissionItem.ForegroundService -> {
                ZdsMessageSseService.start(this)
                "Foreground message service start requested."
            }

            PermissionItem.Vibrate -> {
                if (JiangVibrator.vibrate(this)) {
                    "Vibration sent."
                } else {
                    "Device has no available vibrator."
                }
            }

            else -> "Permission is ready for business action."
        }
        binding.tvResult.text = buildString {
            appendLine("${item.label}: $status")
            appendLine(actionResult)
            appendLine()
            appendPermissions(item.permissions())
        }
    }

    private fun renderDenied(
        item: PermissionItem,
        status: String,
        permissions: List<String>,
    ) {
        binding.tvResult.text = buildString {
            appendLine("${item.label}: $status")
            appendLine("Denied permissions:")
            permissions.forEach { permission ->
                appendLine("- $permission")
            }
        }
    }

    private fun renderOverview() {
        binding.tvResult.text = buildString {
            appendLine("Choose a permission item.")
            appendLine()
            PermissionItem.entries.forEach { item ->
                appendLine("${item.label}:")
                val permissions = item.permissions()
                if (permissions.isEmpty()) {
                    appendLine("- no runtime permission required")
                } else {
                    appendPermissions(permissions)
                }
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendPermissions(permissions: List<String>) {
        permissions.forEach { permission ->
            appendLine("- $permission")
        }
    }

    private enum class PermissionItem(
        val label: String,
        val reason: String,
    ) {
        ForegroundService(
            label = "Foreground service notification",
            reason = "Android 13+ needs notification permission before showing foreground service notifications.",
        ),
        Camera(
            label = "Camera",
            reason = "Camera permission is required for scan code workflows.",
        ),
        Album(
            label = "Album",
            reason = "Photo/video permission is required before opening album picker.",
        ),
        Bluetooth(
            label = "Bluetooth",
            reason = "Bluetooth permission is required before scanning or connecting devices.",
        ),
        Location(
            label = "Location",
            reason = "Location permission is required before querying device location.",
        ),
        Vibrate(
            label = "Vibrate",
            reason = "Vibrate permission is declared in manifest and can be tested directly.",
        );

        fun permissions(): List<String> {
            return when (this) {
                ForegroundService -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }

                Camera -> JiangPermission.cameraPermissions()
                Album -> JiangPermission.mediaPermissions()
                Bluetooth -> JiangPermission.bluetoothPermissions()
                Location -> JiangPermission.locationPermissions()
                Vibrate -> JiangPermission.vibratePermissions()
            }
        }
    }
}
