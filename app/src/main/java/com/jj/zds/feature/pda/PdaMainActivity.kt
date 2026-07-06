package com.jj.zds.feature.pda

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.jj.storage.JiangStorage
import com.jj.ui.base.JStatusBar
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.zds.R
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivityPdaMainBinding
import com.jj.zds.feature.debugplan.ui.DebugPlanListFragment
import com.jj.zds.feature.login.ui.LoginActivity
import com.jj.zds.feature.message.ZdsMessageSseService
import com.jj.zds.feature.update.ui.ZdsUpdateActivity

class PdaMainActivity : FragmentActivity(), PdaHomeFragment.Callback {

    private lateinit var binding: ActivityPdaMainBinding
    private var currentTab = PdaTab.HOME

    private lateinit var homeFragment: PdaHomeFragment
    private lateinit var debugFragment: DebugPlanListFragment
    private lateinit var produceFragment: PdaProduceFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JStatusBar.apply(this)
        binding = ActivityPdaMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.toolbar.setBackVisible(false)
        binding.toolbar.setMenuItems(
            listOf(
                JiangToolbarMenuItem(MENU_UPDATE, "版本更新"),
                JiangToolbarMenuItem(MENU_LOGOUT, "退出登录"),
            ),
        ) { item ->
            onToolbarMenuClick(item)
        }
        restoreFragments()
        bindBackNavigation()
        bindTabs()
        switchTab(PdaTab.HOME)
    }

    override fun openProduceTab(lineId: String?) {
        produceFragment.selectLine(lineId)
        switchTab(PdaTab.PRODUCE)
    }

    private fun bindTabs() {
        binding.tabHome.setOnClickListener { switchTab(PdaTab.HOME) }
        binding.tabDebug.setOnClickListener { switchTab(PdaTab.DEBUG) }
        binding.tabProduce.setOnClickListener { switchTab(PdaTab.PRODUCE) }
    }

    private fun bindBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (currentTab == PdaTab.HOME) {
                        moveTaskToBack(true)
                    } else {
                        switchTab(PdaTab.HOME)
                    }
                }
            },
        )
    }

    private fun switchTab(tab: PdaTab) {
        currentTab = tab
        updateHeader(tab)
        renderTabs(tab)
        val targetFragment = tab.fragment()
        val transaction = supportFragmentManager.beginTransaction()
        listOf(homeFragment, debugFragment, produceFragment).forEach { fragment ->
            if (fragment.isAdded && fragment !== targetFragment) {
                transaction.hide(fragment)
            }
        }
        if (targetFragment.isAdded) {
            transaction.show(targetFragment)
        } else {
            transaction.add(R.id.fragmentContainer, targetFragment, tab.fragmentTag())
        }
        transaction.setPrimaryNavigationFragment(targetFragment)
        transaction.commit()
    }

    private fun restoreFragments() {
        homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? PdaHomeFragment
            ?: PdaHomeFragment()
        debugFragment = supportFragmentManager.findFragmentByTag(TAG_DEBUG) as? DebugPlanListFragment
            ?: DebugPlanListFragment()
        produceFragment = supportFragmentManager.findFragmentByTag(TAG_PRODUCE) as? PdaProduceFragment
            ?: PdaProduceFragment()
    }

    private fun updateHeader(tab: PdaTab) {
        when (tab) {
            PdaTab.HOME -> {
                val userName = JiangStorage.kv.getString(ZdsStorageKeys.USER_NAME, "").orEmpty()
                    .ifBlank { "Admin" }
                updateHeader("宗益 MES", "登录人：$userName")
            }
            PdaTab.DEBUG -> updateHeader("调试计划", "请选择调试计划")
            PdaTab.PRODUCE -> updateHeader("生产计划", "请选择生产计划")
        }
    }

    private fun updateHeader(title: String, subTitle: String) {
        binding.toolbar.setTitleText(title)
        binding.toolbar.setSubtitleText(subTitle)
    }

    private fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        when (item.id) {
            MENU_UPDATE -> startActivity(Intent(this, ZdsUpdateActivity::class.java))
            MENU_LOGOUT -> logout()
        }
    }

    private fun logout() {
        ZdsMessageSseService.stop(this)
        JiangStorage.kv.remove(ZdsStorageKeys.MES_UP_TOKEN)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_ID)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_NAME)
        JiangStorage.kv.remove(ZdsStorageKeys.USER_SKILLS)
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    private fun renderTabs(selected: PdaTab) {
        binding.tabHome.renderTab(selected == PdaTab.HOME, R.drawable.pda_ic_tab_home)
        binding.tabDebug.renderTab(selected == PdaTab.DEBUG, R.drawable.pda_ic_tab_debug)
        binding.tabProduce.renderTab(selected == PdaTab.PRODUCE, R.drawable.pda_ic_tab_produce)
    }

    private fun TextView.renderTab(selected: Boolean, iconRes: Int) {
        val color = ContextCompat.getColor(
            this@PdaMainActivity,
            if (selected) com.jj.ui.R.color.j_color_primary else com.jj.ui.R.color.j_color_text_hint,
        )
        setBackgroundResource(
            if (selected) {
                R.drawable.pda_bg_bottom_tab_selected
            } else {
                R.drawable.pda_bg_bottom_tab_unselected
            },
        )
        setTextColor(color)
        setCompoundDrawablesWithIntrinsicBounds(0, iconRes, 0, 0)
        compoundDrawablePadding = dp(2)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun PdaTab.fragment(): Fragment {
        return when (this) {
            PdaTab.HOME -> homeFragment
            PdaTab.DEBUG -> debugFragment
            PdaTab.PRODUCE -> produceFragment
        }
    }

    private fun PdaTab.fragmentTag(): String {
        return when (this) {
            PdaTab.HOME -> TAG_HOME
            PdaTab.DEBUG -> TAG_DEBUG
            PdaTab.PRODUCE -> TAG_PRODUCE
        }
    }

    private enum class PdaTab {
        HOME,
        DEBUG,
        PRODUCE,
    }

    private companion object {
        const val TAG_HOME = "pda_home"
        const val TAG_DEBUG = "pda_debug"
        const val TAG_PRODUCE = "pda_produce"
        const val MENU_UPDATE = 1
        const val MENU_LOGOUT = 2
    }
}
