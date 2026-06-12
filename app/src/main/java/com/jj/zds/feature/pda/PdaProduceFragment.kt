package com.jj.zds.feature.pda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jj.arch.base.JiangFragment
import com.jj.ui.recycler.adapter.JBindingAdapter
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.databinding.FragmentPdaProduceBinding
import com.jj.zds.databinding.ItemPdaProducePlanBinding

class PdaProduceFragment : JiangFragment<FragmentPdaProduceBinding>() {

    private val plans = listOf(
        ProducePlan("WO20260611001", "调光器主板", "DIP-01", 120, 36, "生产中"),
        ProducePlan("WO20260611002", "灯架控制板", "DIP-02", 100, 0, "待开工"),
        ProducePlan("WO20260611003", "顺闪控制器", "DIP-03", 60, 12, "已暂停"),
    )

    private val adapter = JBindingAdapter(
        inflate = ItemPdaProducePlanBinding::inflate,
        onBind = ::bindPlan,
    )

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentPdaProduceBinding {
        return FragmentPdaProduceBinding.inflate(inflater, container, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerProducePlans.setupLinear(adapter)
        binding.inputSearch.onInputDone { keyword -> filterPlans(keyword) }
        binding.inputSearch.onTextChanged { keyword -> filterPlans(keyword) }
        adapter.setList(plans)
    }

    private fun bindPlan(
        itemBinding: ItemPdaProducePlanBinding,
        item: ProducePlan,
        position: Int,
    ) {
        itemBinding.tvPlanNo.text = item.planNo
        itemBinding.tvProduct.text = "产品：${item.product}"
        itemBinding.tvMeta.text = "线体：${item.line}    计划：${item.planQty}    已完成：${item.doneQty}"
        itemBinding.tvStatus.text = item.status
        val running = item.status == "生产中"
        itemBinding.tvStatus.setBackgroundResource(
            if (running) R.drawable.pda_bg_status_success else R.drawable.pda_bg_status_warning,
        )
        itemBinding.tvStatus.setTextColor(
            ContextCompat.getColor(
                itemBinding.root.context,
                if (running) com.jj.ui.R.color.j_color_success else com.jj.ui.R.color.j_color_warning,
            ),
        )
        itemBinding.btnEnter.setOnClickListener {
            JToast.show("进入生产计划：${item.planNo}")
        }
    }

    private fun filterPlans(keyword: String) {
        val text = keyword.trim()
        adapter.setList(
            if (text.isBlank()) {
                plans
            } else {
                plans.filter { it.planNo.contains(text, ignoreCase = true) || it.product.contains(text) }
            },
        )
    }

    private data class ProducePlan(
        val planNo: String,
        val product: String,
        val line: String,
        val planQty: Int,
        val doneQty: Int,
        val status: String,
    )
}
