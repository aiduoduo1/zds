package com.jj.zds.feature.micsig.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import coil.load
import com.jj.common.exception.JiangErrorCode
import com.jj.storage.JiangStorage
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivityOscilloscopeScreenshotBinding
import com.jj.zds.feature.repair.data.buildRepairFileUrl
import com.jj.zds.viewmodel.ZdsViewModel

class OscilloscopeScreenshotActivity :
    JiangToolbarStateVmActivity<ActivityOscilloscopeScreenshotBinding, ZdsViewModel>() {

    private var currentFileId: Long? = null

    override fun createViewBinding(inflater: LayoutInflater): ActivityOscilloscopeScreenshotBinding {
        return ActivityOscilloscopeScreenshotBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnGetScreenshot.setOnClickListener {
            binding.btnGetScreenshot.isEnabled = false
            viewModel.captureOscilloscopeScreen()
        }
        binding.btnLandscapePreview.setOnClickListener {
            currentFileId?.let(::showLargeImagePopup)
        }
    }

    override fun initPageObserver() {
        viewModel.oscilloscopeScreenshotFileId.observe(this) { fileId ->
            currentFileId = fileId
            binding.btnGetScreenshot.isEnabled = true
            binding.btnLandscapePreview.isEnabled = true
            binding.tvFileId.text = "文件 ID：$fileId"
            binding.tvFileId.visibility = View.VISIBLE
            binding.ivScreenshot.load(buildRepairFileUrl(fileId.toString(), currentToken()))
        }
    }

    override fun toolbarTitle(): CharSequence = "示波器截图"

    override fun toolbarSubtitle(): CharSequence = "点击按钮获取最新截图"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.btnGetScreenshot.isEnabled = true
        dismissLoadingDialog()
        JConfirmDialog(this, this)
            .setTitle("获取截图失败")
            .setMessage(
                if (code == JiangErrorCode.TIMEOUT) {
                    "获取截图超时，请检查示波器连接后重试。"
                } else {
                    message
                },
            )
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .show()
    }

    private fun currentToken(): String {
        return JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
    }

    private fun showLargeImagePopup(fileId: Long) {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        val imageView = OscilloscopeZoomImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            contentDescription = "示波器截图大图"
            load(buildRepairFileUrl(fileId.toString(), currentToken()))
        }
        val closeView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(12)
                marginEnd = dp(12)
            }
            gravity = Gravity.CENTER
            text = "×"
            textSize = 32f
            setTextColor(Color.WHITE)
        }
        root.addView(imageView)
        root.addView(closeView)
        val popup = PopupWindow(
            root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
        closeView.setOnClickListener { popup.dismiss() }
        popup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
