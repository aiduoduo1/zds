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
import com.jj.ui.dialog.JSingleChoiceDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.ui.toolbar.JiangToolbarMenuItem
import com.jj.zds.R
import com.jj.zds.data.dto.productionplan.ProductionEquipmentCheckedRequest
import com.jj.zds.data.dto.productionplan.ProductionSwitchEquipmentDto
import com.jj.zds.data.dto.productionplan.toEquipmentCheckRequest
import com.jj.zds.data.dto.qrcode.QrCodeDto
import com.jj.zds.databinding.ActivityProductionEquipmentCheckBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionEquipmentCheckActivity :
    JiangToolbarStateVmActivity<ActivityProductionEquipmentCheckBinding, ZdsViewModel>() {

    private val partCode: String by lazy {
        intent.getStringExtra(EXTRA_PART_CODE).orEmpty()
    }

    private val checkItems: List<ProductionSwitchEquipmentDto> by lazy {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        (intent.getSerializableExtra(EXTRA_EQUIPMENT_LIST) as? ArrayList<ProductionSwitchEquipmentDto>).orEmpty()
    }

    private var rows = mutableListOf<EquipmentCheckRow>()
    private var lastSubmitCode: String = ""
    private var lastSubmitAt: Long = 0L
    private var ignoreNextDisplayCode: String = ""
    private var scanStep = ScanStep.STATION
    private var selectedStationKey: String? = null
    private var pendingDraftChange: EquipmentDraftChange? = null

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionEquipmentCheckBinding {
        return ActivityProductionEquipmentCheckBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        rows = checkItems.mapIndexed { index, item ->
            EquipmentCheckRow(
                index = index,
                item = item,
                stationKey = item.staId.orDash(item.staCode, "station_$index"),
                stationName = item.stationCategoryLabel.orDash(item.staName, item.staCode, "未命名工位"),
                stationTypeName = item.stationTypeLabel.orDash(),
                toolKey = item.equId.orDash(item.equCatId, item.equCatCode, item.equCode, "tool_$index"),
                toolName = item.equCatName.orDash(item.equName, item.equCatCode, item.equCode, "未命名治工具"),
                toolCode = item.equCode.orDash(item.equCatCode, item.equCatId),
                needCount = item.equipNum.toNeedCount(),
                scannedItems = item.equips.map { checked ->
                    ScannedEquipment(
                        id = checked.id,
                        equipId = checked.equId.orDash(checked.id),
                        equipCode = checked.equCode.orDash(checked.equId, checked.id),
                        equipName = checked.equName.orDash(checked.equCode, checked.equId, checked.id),
                        activeAt = checked.activeAt,
                    )
                }.toMutableList(),
            )
        }.toMutableList()

        binding.inputEquipment.onTextChanged {
            if (it != lastSubmitCode) {
                lastSubmitCode = ""
            }
        }
        binding.inputEquipment.onTextDebounce(delayMillis = 260L) {
            submitDecode(it)
        }
        binding.inputEquipment.onInputDone {
            submitDecode(it)
        }
        renderRows()
        updateScanInputForStep()
        focusScanInput(selectAll = false)
    }

    override fun initPageObserver() {
        viewModel.qrCodeDecodeResult.observe(this) { result ->
            onQrCodeDecoded(result)
        }
        viewModel.productionCheckDone.observe(this) {
            pendingDraftChange = null
            JToast.show("设备核对已保存")
        }
    }

    override fun toolbarTitle(): CharSequence = partCode.ifBlank { "产品号" }

    override fun toolbarSubtitle(): CharSequence = stationScanSubtitle()

    override fun toolbarMenuItems(): List<JiangToolbarMenuItem> {
        return listOf(JiangToolbarMenuItem(MENU_SWITCH_STATION, "重置", visible = false))
    }

    override fun onToolbarMenuClick(item: JiangToolbarMenuItem) {
        when (item.id) {
            MENU_SWITCH_STATION -> switchToStationScan()
            else -> super.onToolbarMenuClick(item)
        }
    }

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        rollbackPendingDraftChange()
        super.onUiError(code, message, throwable)
        showScanError(message)
    }

    private fun submitDecode(rawCode: String) {
        val code = rawCode.trim()
        val now = SystemClock.elapsedRealtime()
        if (code.isBlank()) return
        if (code == ignoreNextDisplayCode) {
            ignoreNextDisplayCode = ""
            return
        }
        if (pendingDraftChange != null) {
            showScanError("设备核对保存中，请稍后再扫", code)
            return
        }
        if (code == lastSubmitCode && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return
        lastSubmitCode = code
        lastSubmitAt = now
        viewModel.decodeQrCode(code)
    }

    private fun onQrCodeDecoded(qrCode: QrCodeDto) {
        when (scanStep) {
            ScanStep.STATION -> onStationDecoded(qrCode)
            ScanStep.EQUIPMENT -> onEquipmentDecoded(qrCode)
        }
    }

    private fun onStationDecoded(qrCode: QrCodeDto) {
        val matchedStations = findMatchedStationGroups(qrCode)
        if (matchedStations.isEmpty()) {
            showScanError("工位不在核对范围内")
            return
        }

        handleMatchedStations(matchedStations)
    }

    private fun handleMatchedStations(matchedStations: List<StationCheckGroup>) {
        val unfinishedStations = matchedStations.filter { !it.isFull() }
        when {
            unfinishedStations.size == 1 -> selectStation(unfinishedStations.first())
            unfinishedStations.size > 1 -> showStationChoiceDialog(unfinishedStations)
            else -> showScanError("该工位已核对完成")
        }
    }

    private fun onEquipmentDecoded(qrCode: QrCodeDto) {
        val matchedStations = findMatchedStationGroups(qrCode)
        if (matchedStations.isNotEmpty() && qrCode.equipCode.isNullOrBlank()) {
            handleMatchedStations(matchedStations)
            return
        }

        val selectedRows = currentStationRows()
        if (selectedRows.isEmpty()) {
            scanStep = ScanStep.STATION
            updateScanInputForStep()
            showScanError("请先扫描工位二维码")
            return
        }
        if (selectedRows.all { it.isFull() }) {
            showScanError("该工位核对数量已满，删除后可继续添加")
            return
        }

        val equipCode = qrCode.equipCode?.trim().orEmpty()
        if (equipCode.isBlank()) {
            showScanError("二维码未解析到装备编码")
            return
        }

        val displayName = qrCode.equipmentDisplayName()

        if (rows.any { row -> row.scannedItems.any { it.equipCode == equipCode } }) {
            showScanError("该治工具已扫描", equipCode)
            return
        }

        val target = findTargetRow(qrCode, selectedRows)
        if (target == null) {
            showScanError("$equipCode 不在核对范围内", equipCode)
            return
        }

        showScanSuccess(equipCode)
        val scannedEquipment = ScannedEquipment(
            id = null,
            equipId = qrCode.id.orEmpty(),
            equipCode = equipCode,
            equipName = displayName,
        )
        target.scannedItems += scannedEquipment
        pendingDraftChange = EquipmentDraftChange(row = target, added = scannedEquipment)
        JToast.show("设备已提交核对")
        renderRows()
        submitEquipmentDraft()
    }

    private fun findMatchedStationGroups(qrCode: QrCodeDto): List<StationCheckGroup> {
        val stationValues = qrCode.stationMatchValues()
        if (stationValues.isEmpty()) return emptyList()
        return stationGroups().filter { group ->
            group.rows.any { row ->
                stationValues.any { value ->
                    row.item.staId == value ||
                        row.item.staCode == value ||
                        row.item.staName == value ||
                        row.item.stationCategoryLabel == value ||
                        row.item.stationTypeLabel == value ||
                        row.stationKey == value ||
                        row.stationName == value
                }
            }
        }
    }

    private fun selectStation(group: StationCheckGroup) {
        selectedStationKey = group.key
        scanStep = ScanStep.EQUIPMENT
        renderRows()
        binding.inputEquipment.setText(group.name)
        binding.inputEquipment.showSuccess("工位核对成功：${group.name}")
        updateScanInputForStep()
        ignoreNextDisplayCode = group.name
        focusScanInput(selectAll = true)
    }

    private fun showStationChoiceDialog(groups: List<StationCheckGroup>) {
        JSingleChoiceDialog<StationCheckGroup>(this, this)
            .setTitle("请选择核对工位")
            .setItems(groups) { group ->
                "${group.name}（${group.done}/${group.total}）"
            }
            .setOnConfirm { group ->
                if (group == null) return@setOnConfirm
                selectStation(group)
            }
            .show()
    }

    private fun updateScanInputForStep() {
        when (scanStep) {
            ScanStep.STATION -> {
                toolbar.setSubtitleText(stationScanSubtitle())
                toolbar.setMenuItemVisible(MENU_SWITCH_STATION, false)
                binding.inputEquipment.setTitle("请扫描工位二维码")
                binding.inputEquipment.setHint("请扫描工位二维码")
            }
            ScanStep.EQUIPMENT -> {
                toolbar.setSubtitleText(equipmentScanSubtitle())
                toolbar.setMenuItemVisible(MENU_SWITCH_STATION, true)
                binding.inputEquipment.setTitle("请扫描治工具二维码")
                binding.inputEquipment.setHint("请扫描治工具二维码")
            }
        }
    }

    private fun switchToStationScan() {
        scanStep = ScanStep.STATION
        selectedStationKey = null
        ignoreNextDisplayCode = ""
        lastSubmitCode = ""
        binding.inputEquipment.setText("")
        binding.inputEquipment.clearState()
        updateScanInputForStep()
        renderRows()
        focusScanInput(selectAll = true)
    }

    private fun findTargetRow(qrCode: QrCodeDto, candidates: List<EquipmentCheckRow>): EquipmentCheckRow? {
        val equipId = qrCode.id?.trim().orEmpty()
        val equipCode = qrCode.equipCode?.trim().orEmpty()

        val fixedMatch = candidates.firstOrNull { row ->
            !row.isFull() &&
                !row.item.equId.isNullOrBlank() &&
                (row.item.equId == equipId || row.item.equCode == equipCode)
        }
        if (fixedMatch != null) return fixedMatch

        return candidates.firstOrNull { row ->
            !row.isFull() &&
                row.item.equId.isNullOrBlank() &&
                row.matchesCategory(qrCode)
        }
    }

    private fun currentStationRows(): List<EquipmentCheckRow> {
        val stationKey = selectedStationKey ?: return emptyList()
        return rows.filter { it.stationKey == stationKey }
    }

    private fun stationScanSubtitle(): String = "装备核对 · 步骤 1/2 扫描工位"

    private fun equipmentScanSubtitle(): String = "装备核对 · 步骤 2/2 扫描治工具"

    private fun stationGroups(): List<StationCheckGroup> {
        return rows.groupBy { it.stationKey }.values.map { stationRows ->
            val done = stationRows.sumOf { it.scannedItems.size }
            val total = stationRows.sumOf { it.needCount }
            StationCheckGroup(
                key = stationRows.first().stationKey,
                name = stationRows.first().stationName,
                rows = stationRows,
                done = done,
                total = total,
            )
        }
    }

    private fun renderRows() {
        binding.layoutStations.removeAllViews()
        if (rows.isEmpty()) {
            binding.layoutStations.addView(createEmptyView())
            return
        }

        rows.groupBy { it.stationKey }.values.forEach { stationRows ->
            binding.layoutStations.addView(createStationView(stationRows))
        }
    }

    private fun createStationView(stationRows: List<EquipmentCheckRow>): View {
        val done = stationRows.sumOf { it.scannedItems.size }
        val total = stationRows.sumOf { it.needCount }
        val isSelected = selectedStationKey == stationRows.first().stationKey
        val stationGroup = StationCheckGroup(
            key = stationRows.first().stationKey,
            name = stationRows.first().stationName,
            rows = stationRows,
            done = done,
            total = total,
        )
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(
                if (isSelected) {
                    R.drawable.pda_bg_equipment_station_selected
                } else {
                    R.drawable.pda_bg_equipment_station
                },
            )
            elevation = dp(if (isSelected) 5 else 2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(10)
            }

            addView(createStatusStrip(done, total, isSelected))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                    addView(
                        createHeaderRow(
                            stationRows.first().stationName,
                            if (isSelected) "核对进度 $done/$total" else "$done/$total",
                            titleSuffix = stationRows.first().stationTypeName.takeIf { it.isNotBlank() && it != "-" },
                            complete = done >= total,
                        ),
                    )
                    stationRows.forEach { row ->
                        addView(createToolView(row))
                    }
                },
            )
            setOnClickListener {
                if (stationGroup.isFull()) {
                    showScanError("该工位核对数量已满，删除后可继续添加")
                } else {
                    selectStation(stationGroup)
                }
            }
        }
    }

    private fun createToolView(row: EquipmentCheckRow): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundResource(R.drawable.pda_bg_equipment_tool)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(8)
            }

            addView(
                createHeaderRow(
                    title = row.toolName,
                    badge = "${row.scannedItems.size}/${row.needCount}",
                    subtitle = "装备型号：${row.toolCode}",
                    complete = row.isFull(),
                ),
            )

            if (row.scannedItems.isEmpty()) {
                addView(createHintView("暂无已匹配治工具"))
            } else {
                row.scannedItems.forEach { scanned ->
                    addView(createScannedView(row, scanned))
                }
            }
        }
    }

    private fun createHeaderRow(
        title: String,
        badge: String,
        titleSuffix: String? = null,
        subtitle: String? = null,
        complete: Boolean = false,
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            addView(
                                TextView(context).apply {
                                    text = title
                                    textSize = 15f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                                },
                            )
                            if (!titleSuffix.isNullOrBlank()) {
                                addView(
                                    TextView(context).apply {
                                        text = titleSuffix
                                        textSize = 12f
                                        setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                        ).apply {
                                            marginStart = dp(6)
                                        }
                                    },
                                )
                            }
                        },
                    )
                    if (!subtitle.isNullOrBlank()) {
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
                    }
                },
            )
            addView(createBadge(badge, complete))
        }
    }

    private fun createScannedView(row: EquipmentCheckRow, scanned: ScannedEquipment): View {
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
                TextView(context).apply {
                    text = scanned.equipCode
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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
                        if (pendingDraftChange != null) {
                            JToast.show("设备核对保存中，请稍后操作")
                            return@setOnClickListener
                        }
                        row.scannedItems.remove(scanned)
                        pendingDraftChange = EquipmentDraftChange(row = row, deleted = scanned)
                        renderRows()
                        JToast.show("已删除")
                        submitEquipmentDraft()
                    }
                },
            )
        }
    }

    private fun submitEquipmentDraft() {
        viewModel.confirmProductionEquipmentCheck(
            rows.map { row ->
                row.item.toEquipmentCheckRequest(
                    checkedEquips = row.scannedItems.map { scanned ->
                        ProductionEquipmentCheckedRequest(
                            id = scanned.id,
                            equId = scanned.equipId,
                            activeAt = scanned.activeAt,
                        )
                    },
                )
            },
        )
    }

    private fun rollbackPendingDraftChange() {
        val change = pendingDraftChange ?: return
        change.added?.let { added ->
            change.row.scannedItems.remove(added)
        }
        change.deleted?.let { deleted ->
            if (change.row.scannedItems.none { it.equipId == deleted.equipId }) {
                change.row.scannedItems.add(deleted)
            }
        }
        pendingDraftChange = null
        renderRows()
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

    private fun createStatusStrip(done: Int, total: Int, selected: Boolean): View {
        val colorRes = when {
            done >= total -> com.jj.ui.R.color.j_color_success
            selected -> com.jj.ui.R.color.j_color_success
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
            text = "暂无装备核对项"
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

    private fun showScanSuccess(equipCode: String) {
        ignoreNextDisplayCode = equipCode
        binding.inputEquipment.setText(equipCode)
        binding.inputEquipment.showSuccess(equipCode)
        focusScanInput(selectAll = true)
    }

    private fun showScanError(message: String, displayCode: String? = null) {
        val code = displayCode?.trim().orEmpty()
        if (code.isNotBlank()) {
            ignoreNextDisplayCode = code
            binding.inputEquipment.setText(code)
        }
        binding.inputEquipment.showError(message)
        JToast.show(message)
        focusScanInput(selectAll = true)
    }

    private fun focusScanInput(selectAll: Boolean) {
        binding.inputEquipment.post {
            binding.inputEquipment.requestInputFocus(showKeyboard = false)
            if (selectAll) {
                binding.inputEquipment.selectAll()
            }
        }
    }

    private fun EquipmentCheckRow.matchesCategory(qrCode: QrCodeDto): Boolean {
        val equipCode = qrCode.equipCode?.trim().orEmpty()
        val categoryCode = qrCode.equipCategoryCode?.trim().orEmpty()
        val categoryName = qrCode.categoryName?.trim().orEmpty()
        return item.equCode == equipCode ||
            (!item.equCatCode.isNullOrBlank() && item.equCatCode == categoryCode) ||
            (!item.equCatName.isNullOrBlank() && item.equCatName == categoryName) ||
            (!item.equCatId.isNullOrBlank() && item.equCatId == qrCode.id)
    }

    private fun EquipmentCheckRow.isFull(): Boolean {
        return scannedItems.size >= needCount
    }

    private fun QrCodeDto.equipmentDisplayName(): String {
        return listOf(categoryName, equipType, equipCode).firstOrNull { !it.isNullOrBlank() } ?: "未知治工具"
    }

    private fun QrCodeDto.stationMatchValues(): List<String> {
        return listOf(id, staId, staCode, staName, siteCode, circulationCode, categoryName)
            .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
            .distinct()
    }

    private fun Int?.toNeedCount(): Int {
        return this?.takeIf { it > 0 } ?: 1
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private data class EquipmentCheckRow(
        val index: Int,
        val item: ProductionSwitchEquipmentDto,
        val stationKey: String,
        val stationName: String,
        val stationTypeName: String,
        val toolKey: String,
        val toolName: String,
        val toolCode: String,
        val needCount: Int,
        val scannedItems: MutableList<ScannedEquipment> = mutableListOf(),
    )

    private data class StationCheckGroup(
        val key: String,
        val name: String,
        val rows: List<EquipmentCheckRow>,
        val done: Int,
        val total: Int,
    ) {
        fun isFull(): Boolean = done >= total
    }

    private data class ScannedEquipment(
        val id: String? = null,
        val equipId: String,
        val equipCode: String,
        val equipName: String,
        val activeAt: String? = null,
    )

    private data class EquipmentDraftChange(
        val row: EquipmentCheckRow,
        val added: ScannedEquipment? = null,
        val deleted: ScannedEquipment? = null,
    )

    private enum class ScanStep {
        STATION,
        EQUIPMENT,
    }

    companion object {
        private const val MENU_SWITCH_STATION = 1
        private const val EXTRA_PART_CODE = "extra_part_code"
        private const val EXTRA_EQUIPMENT_LIST = "extra_equipment_list"
        private const val SUBMIT_DEBOUNCE_WINDOW = 800L

        fun start(
            context: Context,
            partCode: String,
            equipmentList: List<ProductionSwitchEquipmentDto>,
        ) {
            context.startActivity(
                Intent(context, ProductionEquipmentCheckActivity::class.java)
                    .putExtra(EXTRA_PART_CODE, partCode)
                    .putExtra(EXTRA_EQUIPMENT_LIST, ArrayList(equipmentList)),
            )
        }
    }
}
