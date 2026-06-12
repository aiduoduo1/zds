package com.jj.zds.feature.pda

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.jj.storage.JiangStorage
import com.jj.ui.base.JStatusBar
import com.jj.zds.R
import com.jj.zds.data.local.ZdsStorageKeys
import com.jj.zds.databinding.ActivityPdaMainBinding
import com.jj.zds.feature.debugplan.ui.DebugPlanListFragment

class PdaMainActivity : FragmentActivity(), PdaHomeFragment.Callback {

    private lateinit var binding: ActivityPdaMainBinding
    private var currentTab = PdaTab.HOME

    private val homeFragment by lazy { PdaHomeFragment() }
    private val debugFragment by lazy { DebugPlanListFragment() }
    private val produceFragment by lazy { PdaProduceFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JStatusBar.apply(this)
        binding = ActivityPdaMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.toolbar.setBackVisible(false)
        bindTabs()
        switchTab(PdaTab.HOME)
    }

    override fun openProduceTab() {
        switchTab(PdaTab.PRODUCE)
    }

    private fun bindTabs() {
        binding.tabHome.setOnClickListener { switchTab(PdaTab.HOME) }
        binding.tabDebug.setOnClickListener { switchTab(PdaTab.DEBUG) }
        binding.tabProduce.setOnClickListener { switchTab(PdaTab.PRODUCE) }
    }

    private fun switchTab(tab: PdaTab) {
        currentTab = tab
        updateHeader(tab)
        renderTabs(tab)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, tab.fragment())
            .commit()
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

    private fun renderTabs(selected: PdaTab) {
        binding.tabHome.renderTab(selected == PdaTab.HOME)
        binding.tabDebug.renderTab(selected == PdaTab.DEBUG)
        binding.tabProduce.renderTab(selected == PdaTab.PRODUCE)
    }

    private fun TextView.renderTab(selected: Boolean) {
        setBackgroundResource(
            if (selected) {
                R.drawable.pda_bg_tab_selected
            } else {
                R.drawable.pda_bg_tab_unselected
            },
        )
        setTextColor(
            ContextCompat.getColor(
                this@PdaMainActivity,
                if (selected) com.jj.ui.R.color.j_color_text_white else com.jj.ui.R.color.j_color_primary,
            ),
        )
    }

    private fun PdaTab.fragment(): Fragment {
        return when (this) {
            PdaTab.HOME -> homeFragment
            PdaTab.DEBUG -> debugFragment
            PdaTab.PRODUCE -> produceFragment
        }
    }

    private enum class PdaTab {
        HOME,
        DEBUG,
        PRODUCE,
    }
}
