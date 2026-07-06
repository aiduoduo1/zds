package com.jj.zds.feature.login.ui

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.jj.core.permission.hasJiangPermission
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.input.JInputLayout
import com.jj.zds.R
import com.jj.zds.feature.main.ui.MainActivity
import com.jj.zds.databinding.ActivityLoginBinding
import com.jj.zds.feature.message.ZdsMessageSseService
import com.jj.zds.feature.pda.PdaMainActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (viewModel.hasToken()) {
            askNotificationPermissionThenOpenMain()
            return
        }

        binding.inputPassword.setInputType(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
        styleLoginInput(binding.inputAccount)
        styleLoginInput(binding.inputPassword)
        binding.inputAccount.onTextChanged {
            binding.inputAccount.clearState()
            styleLoginInput(binding.inputAccount)
        }
        binding.inputPassword.onTextChanged {
            binding.inputPassword.clearState()
            styleLoginInput(binding.inputPassword)
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

    override fun showToolbar(): Boolean = false

    override fun showBackButton(): Boolean = false

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onDestroy()
    }

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
            binding.inputAccount.showError("请输入账号.")
            styleLoginInput(binding.inputAccount)
            binding.inputAccount.requestInputFocus()
            return false
        }
        if (password.isBlank()) {
            binding.inputPassword.showError("请输入密码.")
            styleLoginInput(binding.inputPassword)
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
        startActivity(Intent(this, PdaMainActivity::class.java))
        finish()
    }

    private fun hideStatusBar() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
    }

    private fun styleLoginInput(input: JInputLayout) {
        val titleRow = input.getChildAt(0) as? LinearLayout
        val inputRow = input.getChildAt(1) as? LinearLayout
        val message = input.getChildAt(2) as? TextView
        val titleIcon = titleRow?.getChildAt(0) as? ImageView
        val title = titleRow?.getChildAt(1) as? TextView
        val editText = inputRow?.getChildAt(0) as? EditText
        val clearIcon = inputRow?.getChildAt(1) as? ImageView

        titleIcon?.visibility = View.GONE
        title?.setTextColor(ContextCompat.getColor(this, R.color.login_text_white))
        title?.textSize = 15f
        editText?.setTextColor(ContextCompat.getColor(this, R.color.login_text_white))
        editText?.setHintTextColor(ContextCompat.getColor(this, R.color.login_text_hint))
        editText?.setBackgroundResource(R.drawable.login_bg_input)
        clearIcon?.let {
            ImageViewCompat.setImageTintList(
                it,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.login_blue_bright)),
            )
        }
        message?.setTextColor(Color.rgb(255, 105, 120))
    }
}
