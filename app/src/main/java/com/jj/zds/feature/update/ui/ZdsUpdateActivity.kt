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
        binding.tvVersion.text = currentVersionName()
        binding.progressDownload.progress = 0
        binding.btnInstall.visibility = View.GONE

        binding.btnCheckUpdate.clickNoRepeat {
            binding.btnInstall.visibility = View.GONE
            downloadedApkFile = null
            binding.progressDownload.progress = 0
            binding.progressDownload.visibility = View.GONE
            binding.tvUpdateState.text = "正在检查"
            binding.tvStatus.text = "正在获取最新版本信息…"
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
                    binding.tvUpdateState.text = "已是最新"
                    binding.tvStatus.text = "当前已是最新版本，无需更新"
                }
            }
        }

        viewModel.apkDownloadProgress.observe(this) { progress ->
            binding.progressDownload.visibility = View.VISIBLE
            binding.progressDownload.progress = progress
            binding.tvUpdateState.text = "正在下载"
            binding.tvStatus.text = "更新包下载中：$progress%"
        }

        viewModel.apkDownloadResult.observe(this) { file ->
            downloadedApkFile = file
            binding.progressDownload.progress = 100
            binding.btnInstall.visibility = View.VISIBLE
            binding.tvUpdateState.text = "下载完成"
            binding.tvStatus.text = "更新包已下载，正在打开系统安装界面"
            installApk(file)
        }
    }

    override fun toolbarTitle(): CharSequence = "应用更新"

    override fun toolbarSubtitle(): CharSequence = currentVersionName()

    override fun errorMode(): JiangErrorMode = JiangErrorMode.TOAST

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.tvUpdateState.text = "操作失败"
        binding.tvStatus.text = "更新失败，请检查网络后重试"
        super.onUiError(code, "更新失败，请检查网络后重试", throwable)
    }

    private fun showUpdateDialog(result: ZdsVersionCheckResult.HasUpdate) {
        val update = result.data
        val fileId = update.fileId
        if (fileId.isNullOrBlank()) {
            binding.tvUpdateState.text = "更新不可用"
            binding.tvStatus.text = "更新包信息缺失，请联系管理员"
            showToast("更新包信息缺失，请联系管理员")
            return
        }

        showConfirmDialog(
            title = "发现新版本",
            message = update.description?.takeIf { it.isNotBlank() } ?: "发现可用更新，是否下载并安装？",
            confirmText = "下载更新",
            cancelText = "暂不更新",
            onConfirm = {
                binding.tvUpdateState.text = "准备下载"
                binding.tvStatus.text = "正在准备更新包…"
                viewModel.downloadUpdateApk(
                    fileId = fileId,
                    targetFile = createUpdateApkFile(),
                )
            },
        )
        binding.tvUpdateState.text = "发现新版本"
        binding.tvStatus.text = "有新版本可用，确认后即可下载"
    }

    private fun installApk(file: File) {
        when (val result = installJiangApk(file)) {
            JiangApkInstallResult.Started -> {
                binding.tvUpdateState.text = "等待安装"
                binding.tvStatus.text = "系统安装界面已打开，请按提示完成安装"
            }

            JiangApkInstallResult.PermissionRequired -> {
                binding.tvUpdateState.text = "需要授权"
                binding.tvStatus.text = "请允许此应用安装更新包"
                showConfirmDialog(
                    title = "需要安装权限",
                    message = "请在设置中允许此应用安装未知来源应用，返回后点击“安装已下载的更新”。",
                    confirmText = "前往设置",
                    cancelText = "稍后处理",
                    onConfirm = {
                        openJiangUnknownAppSourcesSettings()
                    },
                )
            }

            JiangApkInstallResult.FileNotFound -> {
                binding.tvUpdateState.text = "安装失败"
                binding.tvStatus.text = "未找到下载的更新包，请重新下载"
                showToast("未找到下载的更新包，请重新下载")
            }

            JiangApkInstallResult.NotApkFile -> {
                binding.tvUpdateState.text = "安装失败"
                binding.tvStatus.text = "下载的文件不是安装包，请重新下载"
                showToast("下载的文件不是安装包，请重新下载")
            }

            is JiangApkInstallResult.Error -> {
                val message = "打开安装界面失败，请重试"
                binding.tvUpdateState.text = "安装失败"
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
        return packageInfo.versionName ?: "未知版本"
    }
}
