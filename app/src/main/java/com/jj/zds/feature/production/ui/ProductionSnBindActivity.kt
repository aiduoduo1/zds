package com.jj.zds.feature.production.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.base.JiangToolbarStateVmActivity
import com.jj.ui.dialog.JConfirmDialog
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindRequest
import com.jj.zds.data.dto.productionplan.ProductionKeyPartBindResultDto
import com.jj.zds.data.dto.productionplan.ProductionSnBindCategoryDto
import com.jj.zds.data.dto.productionplan.ProductionSnBindInfoDto
import com.jj.zds.databinding.ActivityProductionSnBindBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionSnBindActivity :
    JiangToolbarStateVmActivity<ActivityProductionSnBindBinding, ZdsViewModel>() {

    private val planId: String by lazy { intent.getStringExtra(EXTRA_PLAN_ID).orEmpty() }
    private val planNo: String by lazy { intent.getStringExtra(EXTRA_PLAN_NO).orEmpty() }

    private var currentInfo: ProductionSnBindInfoDto? = null
    private var rows = mutableListOf<BindCategoryRow>()
    private var scanStep = ScanStep.PRO
    private var lastSubmitCode = ""
    private var lastSubmitAt = 0L
    private var ignoreNextDisplayCode = ""
    private var currentProSn = ""
    private var currentProBarcode = ""
    private var activeOperation = ActiveOperation.NONE
    private var pendingRawScan = ""
    private var pendingSuccessMessage = ""

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionSnBindBinding {
        return ActivityProductionSnBindBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.inputBind.onTextChanged {
            if (it != lastSubmitCode) {
                lastSubmitCode = ""
            }
        }
        binding.inputBind.onTextDebounce(delayMillis = 260L) {
            submitScan(it)
        }
        binding.inputBind.onInputDone {
            submitScan(it)
        }
        updateScanInput()
        renderRows()
        focusScanInput(selectAll = false)
    }

    override fun initPageObserver() {
        viewModel.productionBindProgress.observe(this) { progress ->
            binding.tvBindProgress.text = "绑定进度：${progress.finishedCount}/${progress.totalCount}"
        }
        viewModel.productionSnBindInfo.observe(this) { info ->
            val operation = activeOperation
            if (operation != ActiveOperation.LOAD_PRODUCT && operation != ActiveOperation.REFRESHING) {
                return@observe
            }
            val returnedProSn = info.sn.orEmpty()
            if (returnedProSn.isBlank()) {
                currentInfo = null
                currentProSn = ""
                currentProBarcode = ""
                rows.clear()
                scanStep = ScanStep.PRO
                activeOperation = ActiveOperation.NONE
                updateScanInput()
                renderRows()
                showScanError("接口未返回产品SN，无法进行绑定", pendingRawScan)
                return@observe
            }
            currentInfo = info
            currentProSn = returnedProSn
            rows = info.catList.mapIndexed { index, item ->
                item.toBindCategoryRow(index)
            }.toMutableList()
            scanStep = ScanStep.SN
            updateScanInput()
            renderRows()
            if (operation == ActiveOperation.LOAD_PRODUCT) {
                activeOperation = ActiveOperation.NONE
                pendingRawScan = ""
                ignoreNextDisplayCode = currentProSn
                binding.inputBind.setText(currentProSn)
                binding.inputBind.showSuccess("PRO码校验成功")
                focusScanInput(selectAll = true)
                JToast.show(if (rows.any { it.boundItems.isNotEmpty() }) "已恢复绑定信息" else "请继续扫描SN码")
            } else {
                val displayCode = pendingRawScan
                val successMessage = pendingSuccessMessage.ifBlank { "操作成功" }
                activeOperation = ActiveOperation.NONE
                pendingRawScan = ""
                pendingSuccessMessage = ""
                showScanSuccess(displayCode, successMessage)
                JToast.show(successMessage)
            }
        }
        viewModel.productionKeyPartBindResult.observe(this) { result ->
            when (activeOperation) {
                ActiveOperation.BIND_MATERIAL -> handleInitialBindResult(result)
                ActiveOperation.REPLACE_MATERIAL -> refreshAfterMutation("SN换绑成功")
                ActiveOperation.UNBIND_MATERIAL -> refreshAfterMutation("SN解绑成功")
                else -> Unit
            }
        }
    }

    override fun initData() {
        if (planId.isBlank()) {
            JToast.show("生产计划ID为空")
            finish()
            return
        }
        viewModel.loadProductionBindProgress(planId)
    }

    override fun toolbarTitle(): CharSequence = "扫码绑定"

    override fun toolbarSubtitle(): CharSequence = stepSubtitle()

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(JiangToolbarMenuItem(MENU_RESET, "重置"))
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        when (item.id) {
            MENU_RESET -> resetCurrentProduct()
            else -> super.onToolbarMenuClick(item)
        }
    }

    override fun showBackButton(): Boolean = true

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        val operation = activeOperation
        val displayCode = pendingRawScan.ifBlank { lastSubmitCode }
        activeOperation = ActiveOperation.NONE
        pendingSuccessMessage = ""
        super.onUiError(code, message, throwable)
        val displayMessage = if (operation == ActiveOperation.REFRESHING) {
            "操作成功，但绑定信息刷新失败：$message"
        } else {
            message
        }
        binding.tvBindInfo.text = displayMessage
        showScanError(displayMessage, displayCode)
    }

    private fun submitScan(rawCode: String) {
        val code = rawCode.trim()
        val now = SystemClock.elapsedRealtime()
        if (code.isBlank()) return
        if (code == ignoreNextDisplayCode) {
            ignoreNextDisplayCode = ""
            return
        }
        if (code == lastSubmitCode && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return
        lastSubmitCode = code
        lastSubmitAt = now

        when (scanStep) {
            ScanStep.PRO -> loadProInfo(code)
            ScanStep.SN -> bindMaterialSn(code)
        }
    }

    private fun loadProInfo(code: String) {
        activeOperation = ActiveOperation.LOAD_PRODUCT
        pendingRawScan = code
        currentProBarcode = code
        viewModel.loadProductionSnBindInfo(planId, code)
    }

    private fun bindMaterialSn(code: String) {
        if (currentInfo == null || currentProSn.isBlank()) {
            scanStep = ScanStep.PRO
            updateScanInput()
            showScanError("请先扫描PRO码", code)
            return
        }
        if (activeOperation != ActiveOperation.NONE) {
            showScanError("绑定处理中，请稍后再扫描", code)
            return
        }
        pendingRawScan = code
        activeOperation = ActiveOperation.BIND_MATERIAL
        binding.inputBind.verifying("SN绑定中...")
        viewModel.bindProductionKeyPart(
            ProductionKeyPartBindRequests.bind(planId, currentProSn, code),
        )
    }

    private fun handleInitialBindResult(result: ProductionKeyPartBindResultDto) {
        val replaceableSns = ProductionKeyPartBindRequests.replacementCandidates(result)
        when (replaceableSns.size) {
            0 -> refreshAfterMutation("SN绑定成功")
            1 -> replaceMaterial(replaceableSns.first())
            else -> showReplacementDialog(replaceableSns)
        }
    }

    private fun showReplacementDialog(replaceableSns: List<String>) {
        activeOperation = ActiveOperation.AWAITING_REPLACEMENT
        binding.inputBind.clearState()
        binding.tvBindInfo.text = "检测到同类型关键部件已绑定，请选择要替换的SN"
        val dialog = JSingleChoiceDialog<String>(this, this)
            .setTitle("选择要替换的SN")
            .setItems(replaceableSns) { it }
            .setOnConfirm { selected ->
                if (selected.isNullOrBlank()) {
                    activeOperation = ActiveOperation.NONE
                    binding.inputBind.clearState()
                    JToast.show("请选择要替换的SN")
                    focusScanInput(selectAll = true)
                } else {
                    replaceMaterial(selected)
                }
            }
            .show()
        dialog.setOnDismiss {
            if (activeOperation == ActiveOperation.AWAITING_REPLACEMENT) {
                activeOperation = ActiveOperation.NONE
                binding.inputBind.clearState()
                focusScanInput(selectAll = true)
            }
        }
    }

    private fun replaceMaterial(oldMaterialSn: String) {
        activeOperation = ActiveOperation.REPLACE_MATERIAL
        binding.inputBind.verifying("SN换绑中...")
        viewModel.bindProductionKeyPart(
            ProductionKeyPartBindRequests.replace(
                planId = planId,
                proSn = currentProSn,
                matSn = pendingRawScan,
                changeMatSn = oldMaterialSn,
            ),
        )
    }

    private fun unbindMaterial(materialSn: String) {
        if (currentProSn.isBlank()) {
            showScanError("产品SN为空，请重新扫描PRO码")
            return
        }
        if (activeOperation != ActiveOperation.NONE) {
            showScanError("绑定处理中，暂不能解绑")
            return
        }
        pendingRawScan = ""
        activeOperation = ActiveOperation.UNBIND_MATERIAL
        binding.inputBind.verifying("SN解绑中...")
        viewModel.bindProductionKeyPart(
            ProductionKeyPartBindRequest(
                planId = planId,
                proSn = currentProSn,
                matSn = "",
                changeMatSn = materialSn,
                operateType = 2,
            ),
        )
    }

    private fun refreshAfterMutation(message: String) {
        activeOperation = ActiveOperation.REFRESHING
        pendingSuccessMessage = message
        viewModel.loadProductionBindProgress(planId)
        viewModel.loadProductionSnBindInfo(planId, currentProBarcode)
    }

    private fun clearCurrentProduct(message: String) {
        currentInfo = null
        currentProSn = ""
        currentProBarcode = ""
        rows.clear()
        scanStep = ScanStep.PRO
        activeOperation = ActiveOperation.NONE
        pendingRawScan = ""
        pendingSuccessMessage = ""
        lastSubmitCode = ""
        lastSubmitAt = 0L
        ignoreNextDisplayCode = ""
        binding.inputBind.setText("")
        binding.inputBind.showSuccess(message)
        updateScanInput()
        renderRows()
        focusScanInput(selectAll = true)
    }

    private fun resetCurrentProduct() {
        if (activeOperation != ActiveOperation.NONE) {
            JToast.show("绑定处理中，请稍后重试")
            return
        }
        clearCurrentProduct("已重置，请扫描PRO码")
    }

    private fun updateScanInput() {
        toolbar.setSubtitleText(stepSubtitle())
        when (scanStep) {
            ScanStep.PRO -> {
                binding.inputBind.setTitle("请扫描PRO码")
                binding.inputBind.setHint("请扫描PRO码")
            }
            ScanStep.SN -> {
                binding.inputBind.setTitle("请扫描SN码")
                binding.inputBind.setHint("请扫描关键部件SN码")
            }
        }
    }

    private fun stepSubtitle(): String {
        return when (scanStep) {
            ScanStep.PRO -> "扫码绑定 · 步骤 1/2 扫描PRO码"
            ScanStep.SN -> "扫码绑定 · 步骤 2/2 扫描SN码"
        }
    }

    private fun renderRows() {
        val info = currentInfo
        binding.tvBindInfo.text = if (info == null) {
            "先扫描PRO码，加载关键部件绑定信息"
        } else {
            buildString {
                append("当前产品码：").append(info.sn.orDash())
//                append("\n工单：").append(info.orderCode.orDash(planNo))
//                append("    产品：").append(info.partCode.orDash()).append(" / ").append(info.partName.orDash())
//                append("\n当前工序：").append(info.currentProcessLabel.orDash(info.currentProcess, "关键件绑定"))
            }
        }

        binding.layoutCategories.removeAllViews()
        if (rows.isEmpty()) {
            binding.layoutCategories.addView(createEmptyView())
            return
        }
        rows.forEach { row ->
            binding.layoutCategories.addView(createCategoryView(row))
        }
    }

    private fun createCategoryView(row: BindCategoryRow): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.pda_bg_equipment_station)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(10)
            }
            addView(createStatusStrip(row.boundItems.size, row.needCount))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(
                        createHeaderRow(
                            title = row.categoryName.orDash(),
                            badge = "${row.boundItems.size}/${row.needCount}",
                            subtitle = "${row.productName.orDash()} / 用量：${row.needCount}",
                            complete = row.isFull(),
                        ),
                    )
                    if (row.boundItems.isEmpty()) {
                        addView(createHintView("暂无已绑定SN"))
                    } else {
                        row.boundItems.forEach { material ->
                            addView(createBoundMaterialView(row, material))
                        }
                    }
                },
            )
        }
    }

    private fun createHeaderRow(
        title: String,
        badge: String,
        subtitle: String,
        complete: Boolean,
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(
                        TextView(context).apply {
                            text = title
                            textSize = 15f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                        },
                    )
                    addView(
                        TextView(context).apply {
                            text = subtitle
                            textSize = 12f
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                topMargin = dp(3)
                            }
                        },
                    )
                },
            )
            addView(createBadge(badge, complete))
        }
    }

    private fun createBoundMaterialView(row: BindCategoryRow, material: BoundMaterial): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundResource(R.drawable.pda_bg_equipment_scanned)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(6)
            }
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(
                        TextView(context).apply {
                            text = material.materialCode
                            textSize = 13f
                            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                        },
                    )
                    if (material.localDraft) {
                        addView(
                            TextView(context).apply {
                                text = "本次扫描"
                                textSize = 11f
                                setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                            },
                        )
                    }
                },
            )
            addView(
                TextView(context).apply {
                    text = "删除"
                    gravity = Gravity.CENTER
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_danger))
                    setBackgroundResource(R.drawable.pda_bg_status_warning)
                    layoutParams = LinearLayout.LayoutParams(dp(56), dp(30))
                    setOnClickListener {
                        unbindMaterial(material.materialCode)
                    }
                },
            )
        }
    }

    private fun createBadge(textValue: String, complete: Boolean): TextView {
        return TextView(this).apply {
            text = textValue
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (complete) com.jj.ui.R.color.j_color_success else com.jj.ui.R.color.j_color_warning,
                ),
            )
            setBackgroundResource(
                if (complete) {
                    R.drawable.pda_bg_equipment_badge_success
                } else {
                    R.drawable.pda_bg_equipment_badge_warning
                },
            )
            minWidth = dp(56)
            setPadding(dp(8), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(26)).apply {
                marginStart = dp(8)
            }
        }
    }

    private fun createStatusStrip(done: Int, total: Int): View {
        val colorRes = when {
            total > 0 && done >= total -> com.jj.ui.R.color.j_color_success
            done > 0 -> com.jj.ui.R.color.j_color_warning
            else -> com.jj.ui.R.color.j_color_line_normal
        }
        return View(this).apply {
            setBackgroundColor(ContextCompat.getColor(context, colorRes))
            layoutParams = LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT)
        }
    }

    private fun createHintView(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36),
            ).apply {
                topMargin = dp(6)
            }
        }
    }

    private fun createEmptyView(): TextView {
        return TextView(this).apply {
            text = ""
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
            setBackgroundResource(R.drawable.pda_bg_card)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(120),
            )
        }
    }

    private fun showScanSuccess(displayCode: String, message: String) {
        ignoreNextDisplayCode = displayCode
        binding.inputBind.setText(displayCode)
        binding.inputBind.showSuccess(message)
        focusScanInput(selectAll = true)
    }

    private fun showScanError(message: String, displayCode: String? = null) {
        val code = displayCode?.trim().orEmpty()
        if (code.isNotBlank()) {
            ignoreNextDisplayCode = code
            binding.inputBind.setText(code)
        }
        binding.inputBind.showError(message)
        binding.tvBindInfo.text = message
        focusScanInput(selectAll = true)
        JConfirmDialog(this, this)
            .setTitle("操作失败")
            .setMessage(message)
            .setConfirmText("知道了")
            .setShowCancelButton(false)
            .setOnConfirm {
                focusScanInput(selectAll = true)
            }
            .setOnDismiss {
                focusScanInput(selectAll = true)
            }
            .show()
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputBind.post {
            binding.inputBind.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputBind.selectAll()
            }
        }
    }

    private fun ProductionSnBindCategoryDto.toBindCategoryRow(index: Int): BindCategoryRow {
        val categoryKey = categoryId.orDash(categoryName, "category_$index")
        return BindCategoryRow(
            categoryKey = categoryKey,
            categoryId = categoryId,
            categoryName = categoryName,
            productName = productName,
            needCount = quantity.takeIf { it > 0 } ?: 1,
            boundItems = matList.orEmpty().map { material ->
                BoundMaterial(
                    materialId = material.materialId,
                    materialCode = material.materialCode.orDash(),
                    localDraft = false,
                )
            }.filter { it.materialCode.isNotBlank() && it.materialCode != "-" }.toMutableList(),
        )
    }

    private fun BindCategoryRow.isFull(): Boolean {
        return boundItems.size >= needCount
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private data class BindCategoryRow(
        val categoryKey: String,
        val categoryId: String?,
        val categoryName: String?,
        val productName: String?,
        val needCount: Int,
        val boundItems: MutableList<BoundMaterial>,
    )

    private data class BoundMaterial(
        val materialId: String?,
        val materialCode: String,
        val localDraft: Boolean,
    )

    private enum class ActiveOperation {
        NONE,
        LOAD_PRODUCT,
        BIND_MATERIAL,
        AWAITING_REPLACEMENT,
        REPLACE_MATERIAL,
        UNBIND_MATERIAL,
        REFRESHING,
    }

    private enum class ScanStep {
        PRO,
        SN,
    }

    companion object {
        private const val MENU_RESET = 1
        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val EXTRA_PLAN_NO = "extra_plan_no"
        private const val SUBMIT_DEBOUNCE_WINDOW = 800L

        fun start(
            context: Context,
            planId: String,
            planNo: String = "",
        ) {
            context.startActivity(
                Intent(context, ProductionSnBindActivity::class.java)
                    .putExtra(EXTRA_PLAN_ID, planId)
                    .putExtra(EXTRA_PLAN_NO, planNo),
            )
        }
    }
}
