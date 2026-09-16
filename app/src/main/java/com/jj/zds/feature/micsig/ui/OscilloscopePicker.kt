package com.jj.zds.feature.micsig.ui

import android.app.AlertDialog
import androidx.activity.ComponentActivity
import com.jj.zds.data.dto.device.displayLabel
import com.jj.zds.viewmodel.ZdsViewModel

fun ComponentActivity.observeOscilloscopePicker(
    viewModel: ZdsViewModel,
    onCancelled: () -> Unit = {},
) {
    viewModel.onlineOscilloscopes.observe(this) { devices ->
        if (devices == null) return@observe
        viewModel.consumeOscilloscopeChoices()
        if (devices.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("没有可用的示波器")
                .setMessage("请确认示波器和现场 Agent 已在线。")
                .setPositiveButton("知道了", null)
                .setOnDismissListener { onCancelled() }
                .show()
            return@observe
        }
        val labels = devices.map { it.displayLabel() }.toTypedArray()
        var selected = false
        AlertDialog.Builder(this)
            .setTitle("选择要截图的示波器")
            .setItems(labels) { _, index ->
                selected = true
                viewModel.captureOscilloscopeScreen(devices[index].id)
            }
            .setNegativeButton("取消", null)
            .setOnDismissListener { if (!selected) onCancelled() }
            .show()
    }
}
