package com.jj.zds.feature.update.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.jj.core.apk.JiangApkInstallResult
import com.jj.core.apk.installJiangApk
import com.jj.core.apk.openJiangUnknownAppSourcesSettings
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.ext.clickNoRepeat
import com.jj.ui.state.JiangErrorMode
import com.jj.zds.data.dto.update.ZdsVersionCheckResult
import com.jj.zds.databinding.ActivityZdsUpdateBinding
import com.jj.zds.viewmodel.ZdsViewModel
import java.io.File

class ZdsUpdateActivity : JiangToolbarStateVmActivity<ActivityZdsUpdateBinding, ZdsViewModel>() {

    private var downloadedApkFile: File? = null

    override fun createViewBinding(inflater: LayoutInflater): ActivityZdsUpdateBinding {
        return ActivityZdsUpdateBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.tvVersion.text = "Current version: ${currentVersionName()}"
        binding.tvStatus.text = "Tap check update to get latest APK information."
        binding.progressDownload.progress = 0
        binding.btnInstall.visibility = View.GONE

        binding.btnCheckUpdate.clickNoRepeat {
            binding.btnInstall.visibility = View.GONE
            downloadedApkFile = null
            binding.progressDownload.progress = 0
            binding.tvStatus.text = "Checking version..."
            viewModel.checkVersion(currentVersionName())
        }

        binding.btnInstall.clickNoRepeat {
            downloadedApkFile?.let { installApk(it) }
        }
    }

    override fun initPageObserver() {
        viewModel.versionCheckResult.observe(this) { result ->
            when (result) {
                is ZdsVersionCheckResult.HasUpdate -> showUpdateDialog(result)
                is ZdsVersionCheckResult.NoUpdate -> {
                    binding.tvStatus.text = result.message
                    showToast(result.message)
                }
            }
        }

        viewModel.apkDownloadProgress.observe(this) { progress ->
            binding.progressDownload.progress = progress
            binding.tvStatus.text = "Downloading APK... $progress%"
        }

        viewModel.apkDownloadResult.observe(this) { file ->
            downloadedApkFile = file
            binding.progressDownload.progress = 100
            binding.btnInstall.visibility = View.VISIBLE
            binding.tvStatus.text = "Download complete. Ready to install."
            installApk(file)
        }
    }

    override fun toolbarTitle(): CharSequence = "Version Update"

    override fun toolbarSubtitle(): CharSequence = currentVersionName()

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.tvStatus.text = message
        super.onUiError(code, message, throwable)
    }

    private fun showUpdateDialog(result: ZdsVersionCheckResult.HasUpdate) {
        val update = result.data
        val fileId = update.fileId
        if (fileId.isNullOrBlank()) {
            binding.tvStatus.text = "Update file is empty."
            showToast("Update file is empty.")
            return
        }

        showConfirmDialog(
            title = "New version found",
            message = update.description?.takeIf { it.isNotBlank() } ?: "Download and install latest APK?",
            confirmText = "Download",
            onConfirm = {
                binding.tvStatus.text = "Preparing download..."
                viewModel.downloadUpdateApk(
                    fileId = fileId,
                    targetFile = createUpdateApkFile(),
                )
            },
        )
    }

    private fun installApk(file: File) {
        when (val result = installJiangApk(file)) {
            JiangApkInstallResult.Started -> {
                binding.tvStatus.text = "System installer opened."
            }

            JiangApkInstallResult.PermissionRequired -> {
                binding.tvStatus.text = "Install permission required."
                showConfirmDialog(
                    title = "Install permission",
                    message = "Please allow this app to install unknown apps, then return and tap Install downloaded APK.",
                    confirmText = "Open settings",
                    onConfirm = {
                        openJiangUnknownAppSourcesSettings()
                    },
                )
            }

            JiangApkInstallResult.FileNotFound -> {
                binding.tvStatus.text = "Downloaded APK not found."
                showToast("Downloaded APK not found.")
            }

            JiangApkInstallResult.NotApkFile -> {
                binding.tvStatus.text = "Downloaded file is not APK."
                showToast("Downloaded file is not APK.")
            }

            is JiangApkInstallResult.Error -> {
                val message = result.throwable.message ?: "Open installer failed."
                binding.tvStatus.text = message
                showToast(message)
            }
        }
    }

    private fun createUpdateApkFile(): File {
        return File(cacheDir, "updates/zds-update-${System.currentTimeMillis()}.apk")
    }

    @Suppress("DEPRECATION")
    private fun currentVersionName(): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
        return packageInfo.versionName ?: "unknown"
    }
}
