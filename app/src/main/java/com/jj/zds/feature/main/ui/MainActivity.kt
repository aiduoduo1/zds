package com.jj.zds.feature.main.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import coil.load
import com.jj.storage.JiangStorage
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.setupGrid
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.zds.R
import com.jj.zds.data.dto.menu.ZdsMenuDto
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivityMainBinding
import com.jj.zds.databinding.ItemZdsMenuBinding
import com.jj.zds.feature.aging.AgingActivity
import com.jj.zds.feature.debugplan.ui.CurrentBatchDebugRecordActivity
import com.jj.zds.feature.debugplan.ui.DebugPlanListActivity
import com.jj.zds.feature.login.ui.LoginActivity
import com.jj.zds.feature.message.ZdsMessageSseService
import com.jj.zds.feature.permission.ui.PermissionAccessDemoActivity
import com.jj.zds.feature.pda.PdaMainActivity
import com.jj.zds.feature.qrcode.ui.QrCodeDecodeActivity
import com.jj.zds.feature.repair.ui.RepairActivity
import com.jj.zds.feature.singledebug.ui.SingleDebugActivity
import com.jj.zds.feature.twolevelscan.ui.TwoLevelScanDemoActivity
import com.jj.zds.feature.update.ui.ZdsUpdateActivity
import com.jj.zds.viewmodel.ZdsViewModel

class MainActivity : JiangToolbarStateVmActivity<ActivityMainBinding, ZdsViewModel>() {

    private val menuAdapter = JBindingAdapter(
        inflate = ItemZdsMenuBinding::inflate,
        onBind = ::bindMenu,
    )

    override fun createViewBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        ZdsMessageSseService.Companion.start(this)
        binding.recyclerMenu.setupGrid(menuAdapter, spanCount = 2)
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadMenu()
        }
        menuAdapter.setOnItemClickListener { item, _ ->
            openMenu(item)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        })
        viewModel.loadMenu()
    }

    override fun initPageObserver() {
        viewModel.menus.observe(this) { menus ->
            binding.refreshLayout.isRefreshing = false
            menuAdapter.setList(menus)
            if (menus.isEmpty()) {
                showEmptyPage("No menu permission. Please contact administrator.")
            } else {
                showContentPage()
            }
        }
    }

    override fun toolbarTitle(): CharSequence = getString(R.string.app_name)

    override fun toolbarSubtitle(): CharSequence = JiangStorage.kv.getString(
        ZdsStorageKeys.USER_NAME,
        "",
    ).orEmpty()

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(
            JiangToolbarMenuItem(MENU_REFRESH, "Refresh"),
            JiangToolbarMenuItem(MENU_UPDATE, "Update"),
            JiangToolbarMenuItem(MENU_PERMISSION_DEMO, "Permissions"),
            JiangToolbarMenuItem(MENU_TWO_LEVEL_DEMO, "TwoLevel"),
            JiangToolbarMenuItem(MENU_QR_DECODE, "QrDecode"),
            JiangToolbarMenuItem(MENU_CURRENT_BATCH_RECORD, "BatchRecord"),
            JiangToolbarMenuItem(MENU_PDA_WORKBENCH, "PDA"),
            JiangToolbarMenuItem(MENU_AGING_MANAGEMENT, "老化管理"),
            JiangToolbarMenuItem(MENU_LOGIN_OUT, "Logout"),
        )
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        when (item.id) {
            MENU_REFRESH -> viewModel.loadMenu()
            MENU_UPDATE -> {
                startActivity(Intent(this, ZdsUpdateActivity::class.java))
            }

            MENU_PERMISSION_DEMO -> {
                startActivity(Intent(this, PermissionAccessDemoActivity::class.java))
            }

            MENU_TWO_LEVEL_DEMO -> {
                startActivity(Intent(this, TwoLevelScanDemoActivity::class.java))
            }

            MENU_QR_DECODE -> {
                startActivity(Intent(this, QrCodeDecodeActivity::class.java))
            }

            MENU_CURRENT_BATCH_RECORD -> {
                startActivity(Intent(this, CurrentBatchDebugRecordActivity::class.java))
            }

            MENU_PDA_WORKBENCH -> {
                startActivity(Intent(this, PdaMainActivity::class.java))
            }

            MENU_AGING_MANAGEMENT -> startActivity(Intent(this, AgingActivity::class.java))
            MENU_LOGIN_OUT -> clearLogin()
        }
    }

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        binding.refreshLayout.isRefreshing = false
        super.onUiError(code, message, throwable)
    }

    override fun showBackButton(): Boolean = false

    private fun clearLogin() {
        ZdsMessageSseService.Companion.stop(this)
        viewModel.clearLogin()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun bindMenu(
        itemBinding: ItemZdsMenuBinding,
        item: ZdsMenuDto,
        position: Int,
    ) {
        itemBinding.ivMenu.load(buildMenuIconUrl(item.icon))
        itemBinding.tvMenuText.text = item.title
        val count = item.count ?: 0
        itemBinding.tvTaskCount.visibility = if (count > 0) View.VISIBLE else View.INVISIBLE
        itemBinding.tvTaskCount.text = count.toString()
    }

    private fun buildMenuIconUrl(icon: String?): String? {
        val fileId = icon?.takeIf { it.isNotBlank() } ?: return null
        val token = JiangStorage.kv.getString(ZdsStorageKeys.MES_UP_TOKEN, "").orEmpty()
        return "$FINAL_MES_BASE_URL/task/taskFile/download/$fileId?token=$token"
    }

    private fun openMenu(item: ZdsMenuDto) {
        when (item.path) {
            PART_SWITCH -> startActivity(Intent(this, DebugPlanListActivity::class.java))
            PRODUCE -> startActivity(Intent(this, RepairActivity::class.java))
            RECEIVE-> startActivity(Intent(this, SingleDebugActivity::class.java))
            SALE,
            BACK,
            PRINT,
            -> showToast("${item.title} page pending migration.")

            else -> showToast("${item.title} page not configured.")
        }
    }

    private companion object {
        const val MENU_REFRESH = 1
        const val MENU_UPDATE = 2
        const val MENU_PERMISSION_DEMO = 3
        const val MENU_TWO_LEVEL_DEMO = 4
        const val MENU_QR_DECODE = 5
        const val MENU_CURRENT_BATCH_RECORD = 6
        const val MENU_PDA_WORKBENCH = 7
        const val MENU_LOGIN_OUT = 8
        const val MENU_AGING_MANAGEMENT = 9
        const val PART_SWITCH = "partSwitch"
        const val PRODUCE = "produce"
        const val RECEIVE = "receive"
        const val SALE = "sale"
        const val BACK = "back"
        const val PRINT = "print"
        const val FINAL_MES_BASE_URL = "http://192.168.1.216:81"
    }
}
