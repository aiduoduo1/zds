package com.jj.zds.feature.pda

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.arch.base.JiangFragment
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.databinding.FragmentPdaHomeBinding
import com.jj.zds.feature.recheck.ui.RecheckActivity
import com.jj.zds.feature.repair.ui.RepairActivity
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
        renderCurrentProduction(mockLineWorks())
        binding.btnViewProducePlan.setOnClickListener {
            callback?.openProduceTab()
        }
        bindActionPlaceholders()
    }

    private fun renderCurrentProduction(lineWorks: List<HomeLineWork>) {
        val visibleWorks = lineWorks.take(MAX_HOME_LINE_WORKS)
        val hasCurrentPlan = visibleWorks.isNotEmpty()
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
        binding.layoutCurrentLines.removeAllViews()
        visibleWorks.forEachIndexed { index, work ->
            binding.layoutCurrentLines.addView(createLineWorkView(work, index))
        }
    }

    private fun createLineWorkView(work: HomeLineWork, index: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.pda_bg_home_current_row_dark)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(66),
            ).apply {
                if (index > 0) topMargin = dp(8)
            }

            addView(
                View(context).apply {
                    setBackgroundColor(ContextCompat.getColor(context, work.colorRes))
                    layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
                },
            )

            addView(
                TextView(context).apply {
                    text = work.lineName
                    gravity = Gravity.CENTER
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                    layoutParams = LinearLayout.LayoutParams(dp(58), LinearLayout.LayoutParams.MATCH_PARENT)
                },
            )

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    addView(
                        TextView(context).apply {
                            text = work.planNo
                            textSize = 14f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                            maxLines = 1
                        },
                    )
                    addView(
                        TextView(context).apply {
                            text = work.product
                            textSize = 12f
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                            maxLines = 1
                        },
                    )
                },
            )

            addView(createStatusTag(work))
            addView(createLineAction(work))
        }
    }

    private fun createStatusTag(work: HomeLineWork): TextView {
        return TextView(requireContext()).apply {
            text = work.statusText
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, work.colorRes))
            setBackgroundResource(work.statusBgRes)
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(26)).apply {
                marginStart = dp(6)
            }
        }
    }

    private fun createLineAction(work: HomeLineWork): TextView {
        return TextView(requireContext()).apply {
            text = "前往"
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_primary))
            setBackgroundResource(R.drawable.pda_bg_home_go)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(28)).apply {
                marginStart = dp(6)
                marginEnd = dp(8)
            }
            setOnClickListener {
                if (work.isIdle) {
                    callback?.openProduceTab()
                } else {
                    JToast.show("前往：${work.planNo}")
                }
            }
        }
    }

    private fun mockLineWorks(): List<HomeLineWork> {
        return listOf(
            HomeLineWork(
                lineName = "DIP-01",
                statusText = "生产中",
                planNo = "WO20260611001",
                product = "调光器主板  36/120",
                actionText = "前往",
                colorRes = com.jj.ui.R.color.j_color_success,
                statusBgRes = com.jj.ui.R.drawable.j_bg_status_success,
            ),
            HomeLineWork(
                lineName = "DIP-02",
                statusText = "切换中",
                planNo = "WO20260611002",
                product = "灯架控制板",
                actionText = "前往",
                colorRes = com.jj.ui.R.color.j_color_primary,
                statusBgRes = com.jj.ui.R.drawable.j_bg_status_info,
            ),
            HomeLineWork(
                lineName = "DIP-03",
                statusText = "空闲",
                planNo = "暂无进行中计划",
                product = "从生产计划列表选择计划",
                actionText = "前往",
                colorRes = com.jj.ui.R.color.j_color_neutral,
                statusBgRes = com.jj.ui.R.drawable.j_bg_status_neutral,
                isIdle = true,
            ),
        )
    }

    private fun bindActionPlaceholders() {
        binding.homeActions.btnSingleDebug.setOnClickListener {
            startActivity(Intent(requireContext(), SingleDebugActivity::class.java))
        }
        binding.homeActions.btnRepair.setOnClickListener {
            startActivity(Intent(requireContext(), RepairActivity::class.java))

        }
        binding.homeActions.btnRecheck.setOnClickListener {
            startActivity(Intent(requireContext(), RecheckActivity::class.java))
        }
        binding.homeActions.btnProductIn.setOnClickListener { JToast.show("进入成品入库") }
        binding.homeActions.btnProductOut.setOnClickListener { JToast.show("进入成品出库") }
        binding.homeActions.btnBind.setOnClickListener { JToast.show("进入绑定") }
        binding.homeActions.btnUnbind.setOnClickListener { JToast.show("进入解绑") }
        binding.homeActions.btnMaterialDetail.setOnClickListener { JToast.show("进入材料详情") }
    }

    interface Callback {
        fun openProduceTab()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private data class HomeLineWork(
        val lineName: String,
        val statusText: String,
        val planNo: String,
        val product: String,
        val actionText: String,
        val colorRes: Int,
        val statusBgRes: Int,
        val isIdle: Boolean = false,
    )

    private companion object {
        const val MAX_HOME_LINE_WORKS = 3
    }
}
