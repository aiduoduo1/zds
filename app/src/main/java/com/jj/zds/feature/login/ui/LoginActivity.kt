package com.jj.zds.feature.login.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import com.jj.core.permission.hasJiangPermission
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.zds.feature.main.ui.MainActivity
import com.jj.zds.databinding.ActivityLoginBinding
import com.jj.zds.feature.message.ZdsMessageSseService
import com.jj.zds.viewmodel.ZdsViewModel

class LoginActivity : JiangToolbarStateVmActivity<ActivityLoginBinding, ZdsViewModel>() {

    private var pendingOpenMain = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showToast("Notification permission granted.")
        } else {
            showToast("Notification permission denied. Foreground notification may be limited.")
        }
        if (pendingOpenMain) {
            pendingOpenMain = false
            startMessageServiceAndOpenMain()
        }
    }

    override fun createViewBinding(inflater: LayoutInflater): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (viewModel.hasToken()) {
            askNotificationPermissionThenOpenMain()
            return
        }

        binding.inputPassword.setInputType(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
        binding.inputAccount.onTextChanged {
            binding.inputAccount.clearState()
        }
        binding.inputPassword.onTextChanged {
            binding.inputPassword.clearState()
        }
        binding.inputPassword.onInputDone {
            submitLogin()
        }
        binding.btnLogin.setOnClickListener {
            submitLogin()
        }
    }

    override fun initPageObserver() {
        viewModel.loginSuccess.observe(this) {
            askNotificationPermissionThenOpenMain()
        }
    }

    override fun showBackButton(): Boolean = false

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        ZdsMessageSseService.stop(this)
        dismissLoadingDialog()
        showContentPage()
        JConfirmDialog(this, this)
            .setTitle("Login failed")
            .setMessage(message)
            .setConfirmText("OK")
            .setShowCancelButton(false)
            .show()
    }

    private fun submitLogin() {
        if (!validateInput()) {
            ZdsMessageSseService.stop(this)
            return
        }

        viewModel.login(
            account = binding.inputAccount.text(),
            password = binding.inputPassword.text(),
        )
    }

    private fun validateInput(): Boolean {
        val account = binding.inputAccount.text()
        val password = binding.inputPassword.text()

        if (account.isBlank()) {
            binding.inputAccount.showError("Please enter account.")
            binding.inputAccount.requestInputFocus()
            return false
        }
        if (password.isBlank()) {
            binding.inputPassword.showError("Please enter password.")
            binding.inputPassword.requestInputFocus()
            return false
        }
        return true
    }

    private fun askNotificationPermissionThenOpenMain() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasJiangPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            startMessageServiceAndOpenMain()
            return
        }

        pendingOpenMain = true
        showConfirmDialog(
            title = "Notification permission",
            message = "Android 13+ needs notification permission before showing foreground service notifications.",
            confirmText = "Ask",
            cancelText = "Skip",
            onConfirm = {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onCancel = {
                pendingOpenMain = false
                startMessageServiceAndOpenMain()
            },
        )
    }

    private fun startMessageServiceAndOpenMain() {
        ZdsMessageSseService.start(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
