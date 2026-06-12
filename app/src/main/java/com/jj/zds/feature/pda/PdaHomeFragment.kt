package com.jj.zds.feature.pda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jj.arch.base.JiangFragment
import com.jj.ui.toast.JToast
import com.jj.zds.databinding.FragmentPdaHomeBinding
import com.jj.zds.feature.singledebug.ui.SingleDebugActivity

class PdaHomeFragment : JiangFragment<FragmentPdaHomeBinding>() {

    private var callback: Callback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? Callback
    }

    override fun onDetach() {
        callback = null
        super.onDetach()
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentPdaHomeBinding {
        return FragmentPdaHomeBinding.inflate(inflater, container, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        renderCurrentProduction(hasCurrentPlan = true)
        binding.btnContinueProduction.setOnClickListener {
            JToast.show("进入当前生产：WO20260611001")
        }
        binding.btnViewProducePlan.setOnClickListener {
            callback?.openProduceTab()
        }
        bindActionPlaceholders()
    }

    private fun renderCurrentProduction(hasCurrentPlan: Boolean) {
        binding.cardCurrentProduction.visibility = if (hasCurrentPlan) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
        binding.cardNoCurrentProduction.visibility = if (hasCurrentPlan) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun bindActionPlaceholders() {
        binding.homeActions.btnSingleDebug.setOnClickListener {
            startActivity(Intent(requireContext(), SingleDebugActivity::class.java))
        }
        binding.homeActions.btnRepair.setOnClickListener { JToast.show("进入维修") }
        binding.homeActions.btnRecheck.setOnClickListener { JToast.show("进入复检") }
        binding.homeActions.btnProductIn.setOnClickListener { JToast.show("进入成品入库") }
        binding.homeActions.btnProductOut.setOnClickListener { JToast.show("进入成品出库") }
        binding.homeActions.btnBind.setOnClickListener { JToast.show("进入绑定") }
        binding.homeActions.btnUnbind.setOnClickListener { JToast.show("进入解绑") }
        binding.homeActions.btnMaterialDetail.setOnClickListener { JToast.show("进入材料详情") }
    }

    interface Callback {
        fun openProduceTab()
    }
}
