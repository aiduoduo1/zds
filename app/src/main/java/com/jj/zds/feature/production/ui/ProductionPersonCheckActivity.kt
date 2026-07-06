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
import com.jj.ui.dialog.JBaseDialog
import com.jj.ui.dialog.JScanInputDialog
import com.jj.ui.state.JiangErrorMode
import com.jj.ui.toast.JToast
import com.jj.zds.R
import com.jj.zds.data.dto.common.CurUserDto
import com.jj.zds.data.dto.productionplan.ProductionPersonCheckedRequest
import com.jj.zds.data.dto.productionplan.ProductionSwitchUserDto
import com.jj.zds.data.dto.productionplan.toPersonCheckRequest
import com.jj.zds.databinding.ActivityProductionPersonCheckBinding
import com.jj.zds.viewmodel.ZdsViewModel

class ProductionPersonCheckActivity :
    JiangToolbarStateVmActivity<ActivityProductionPersonCheckBinding, ZdsViewModel>() {

    private val partCode: String by lazy { intent.getStringExtra(EXTRA_PART_CODE).orEmpty() }

    private val checkItems: List<ProductionSwitchUserDto> by lazy {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        (intent.getSerializableExtra(EXTRA_PERSON_LIST) as? ArrayList<ProductionSwitchUserDto>).orEmpty()
    }

    private var rows = mutableListOf<PersonCheckRow>()
    private var pendingRow: PersonCheckRow? = null
    private var pendingAccount: String = ""
    private var scanDialog: JScanInputDialog? = null
    private var scanBaseDialog: JBaseDialog? = null
    private var lastSubmitAccount: String = ""
    private var lastSubmitAt: Long = 0L
    private var pendingDraftChange: PersonDraftChange? = null

    override fun createViewBinding(inflater: LayoutInflater): ActivityProductionPersonCheckBinding {
        return ActivityProductionPersonCheckBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        rows = checkItems.mapIndexed { index, item ->
            PersonCheckRow(
                index = index,
                item = item,
                stationKey = item.staId.orDash(item.staCode, "station_$index"),
                stationName = item.staName.orDash(item.staCode, item.stationCategoryLabel, "未命名工位"),
                skillName = item.skiName.orDash(item.skiId, "未命名资质"),
                checkedUsers = item.users.mapNotNull { checked ->
                    val userId = checked.userId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    CheckedPerson(
                        id = checked.id,
                        userId = userId,
                        account = checked.account.orEmpty(),
                        userName = checked.userName.orEmpty(),
                        activeAt = checked.activeAt,
                    )
                }.toMutableList(),
            )
        }.toMutableList()
        renderRows()
    }

    override fun initPageObserver() {
        viewModel.userByAccount.observe(this) { user -> onUserLoaded(user) }
        viewModel.productionCheckDone.observe(this) {
            val successToast = pendingDraftChange?.successToast ?: "保存成功"
            pendingDraftChange = null
            JToast.show(successToast)
        }
    }

    override fun toolbarTitle(): CharSequence = partCode.ifBlank { "产品号" }

    override fun toolbarSubtitle(): CharSequence = "人员资质核对"

    override fun enableStateLayout(): Boolean = false

    override fun errorMode(): JiangErrorMode = JiangErrorMode.NONE

    override fun onUiError(code: Int, message: String, throwable: Throwable?) {
        rollbackPendingAddition()
        super.onUiError(code, message, throwable)
        showDialogError(message)
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

    private fun createStationView(stationRows: List<PersonCheckRow>): View {
        val done = stationRows.count { it.checkedUsers.isNotEmpty() }
        val total = stationRows.size
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.pda_bg_equipment_station)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(10) }

            addView(createStatusStrip(done, total))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(
                        createHeaderRow(
                            title = stationRows.first().stationName,
                            badge = "$done/$total",
                            titleSuffix = "人员资质",
                            complete = done >= total,
                        ),
                    )
                    stationRows.forEach { row -> addView(createSkillView(row)) }
                },
            )
        }
    }

    private fun createSkillView(row: PersonCheckRow): View {
        val checkedUsers = row.checkedUsers
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundResource(R.drawable.pda_bg_equipment_tool)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }

            addView(
                createHeaderRow(
                    title = row.skillName,
                    badge = "${checkedUsers.size}/1",
                    complete = checkedUsers.isNotEmpty(),
                ),
            )
            if (checkedUsers.isEmpty()) {
                addView(createHintView("暂无已核对人员，点击扫描人员码"))
            } else {
                checkedUsers.forEach { checked -> addView(createCheckedUserView(row, checked)) }
                addView(createHintView("点击继续添加人员"))
            }
            setOnClickListener { showPersonScanDialog(row) }
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
                                ).apply { topMargin = dp(3) }
                            },
                        )
                    }
                },
            )
            addView(createBadge(badge, complete))
        }
    }

    private fun createCheckedUserView(row: PersonCheckRow, user: CheckedPerson): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundResource(R.drawable.pda_bg_equipment_scanned)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) }

            addView(
                TextView(context).apply {
                    text = user.displayName
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
                            JToast.show("人员资质保存中，请稍后操作")
                            return@setOnClickListener
                        }
                        row.checkedUsers.remove(user)
                        pendingDraftChange = PersonDraftChange(row = row, deleted = user, successToast = "删除成功")
                        renderRows()
                        submitPersonDraft()
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
            ).apply { topMargin = dp(6) }
        }
    }

    private fun createEmptyView(): TextView {
        return TextView(this).apply {
            text = "暂无人员资质核对项"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, com.jj.ui.R.color.j_color_text_secondary))
            setBackgroundResource(R.drawable.pda_bg_card)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(120))
        }
    }

    private fun showPersonScanDialog(row: PersonCheckRow) {
        pendingRow = row
        pendingAccount = ""
        scanDialog = JScanInputDialog(this, this)
            .setTitle(row.skillName)
            .setInputTitle("请扫描人员码")
            .setHint("人员码")
            .setConfirmVisible(false)
            .onTextDebounce(delayMillis = 260L) { account -> submitPersonCode(row, account) }
            .setOnSubmit { account -> submitPersonCode(row, account) }
            .selectAllInput()
        scanBaseDialog = scanDialog?.show()
    }

    private fun submitPersonCode(row: PersonCheckRow, account: String) {
        val trimmedAccount = account.trim()
        val now = SystemClock.elapsedRealtime()
        if (trimmedAccount.isBlank()) {
            showDialogError("人员码为空")
            return
        }
        if (trimmedAccount == lastSubmitAccount && now - lastSubmitAt < SUBMIT_DEBOUNCE_WINDOW) return
        lastSubmitAccount = trimmedAccount
        lastSubmitAt = now
        pendingRow = row
        pendingAccount = trimmedAccount
        viewModel.initUserByAccount(trimmedAccount)
    }

    private fun onUserLoaded(user: CurUserDto) {
        val row = pendingRow ?: return
        if (user.id.isNullOrBlank()) {
            showDialogError("用户ID为空")
            return
        }
        if (row.checkedUsers.any { it.userId == user.id }) {
            showDialogError("该人员已核对")
            return
        }
        if (!row.matchesSkill(user)) {
            showDialogError("人员资质不符合")
            return
        }
        val checkedPerson = CheckedPerson(
            id = null,
            userId = user.id,
            account = pendingAccount,
            userName = user.userName.orEmpty(),
            activeAt = null,
        )
        row.checkedUsers.add(checkedPerson)
        pendingDraftChange = PersonDraftChange(row = row, addedUserId = checkedPerson.userId, successToast = "核对成功")
        showDialogSuccess(user)
        pendingRow = null
        pendingAccount = ""
        renderRows()
        submitPersonDraft()
    }

    private fun submitPersonDraft() {
        viewModel.confirmProductionPersonCheck(
            rows.map { row ->
                row.item.toPersonCheckRequest(
                    checkedUsers = row.checkedUsers.map { checked ->
                        ProductionPersonCheckedRequest(
                            id = checked.id,
                            userId = checked.userId,
                            activeAt = checked.activeAt,
                        )
                    },
                )
            },
        )
    }

    private fun showDialogError(message: String) {
        scanDialog?.let { dialog ->
            dialog.showError(message).selectAllInput()
            scanBaseDialog?.show()
        }
        JToast.show(message)
    }

    private fun showDialogSuccess(user: CurUserDto) {
        scanDialog?.let { dialog ->
            dialog.showSuccess(user.toScanSuccessMessage()).selectAllInput()
            scanBaseDialog?.show()
        }
    }

    private fun PersonCheckRow.matchesSkill(user: CurUserDto): Boolean {
        val requiredSkillId = item.skiId?.trim()?.takeIf { it.isNotBlank() } ?: return true
        return user.skiList
            .orEmpty()
            .any { it.skillId?.trim() == requiredSkillId }
    }

    private fun rollbackPendingAddition() {
        val change = pendingDraftChange ?: return
        change.addedUserId?.let { userId ->
            change.row.checkedUsers.removeAll { it.userId == userId }
        }
        change.deleted?.let { deleted ->
            if (change.row.checkedUsers.none { it.userId == deleted.userId }) {
                change.row.checkedUsers.add(deleted)
            }
        }
        pendingDraftChange = null
        renderRows()
    }

    private fun String?.orDash(vararg fallback: String?): String {
        return sequenceOf(this, *fallback).firstOrNull { !it.isNullOrBlank() } ?: "-"
    }

    private fun CurUserDto.displayName(account: String): String = userName.orDash(id, account)

    private fun CurUserDto.toScanSuccessMessage(): String {
        val name = displayName(pendingAccount)
        val skillsText = skiList
            .orEmpty()
            .map { skill ->
                buildList {
                    add(skill.skillName.orDash(skill.skillId))
                    skill.skillLevel?.takeIf { it.isNotBlank() }?.let { add("等级$it") }
                    skill.activeAtLabel?.takeIf { it.isNotBlank() }?.let { add("生效$it") }
                    skill.inActiveAtLabel?.takeIf { it.isNotBlank() }?.let { add("失效$it") }
                }.joinToString(" / ")
            }
            .filter { it.isNotBlank() }
            .joinToString("、")
            .ifBlank { "-" }
        return "用户：$name\n资质：$skillsText"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private data class PersonCheckRow(
        val index: Int,
        val item: ProductionSwitchUserDto,
        val stationKey: String,
        val stationName: String,
        val skillName: String,
        val checkedUsers: MutableList<CheckedPerson> = mutableListOf(),
    )

    private data class CheckedPerson(
        val id: String?,
        val userId: String,
        val account: String,
        val userName: String,
        val activeAt: String?,
    ) {
        val displayName: String get() = listOf(userName, account, userId).firstOrNull { it.isNotBlank() } ?: "-"
    }

    private data class PersonDraftChange(
        val row: PersonCheckRow,
        val addedUserId: String? = null,
        val deleted: CheckedPerson? = null,
        val successToast: String,
    )

    companion object {
        private const val EXTRA_PART_CODE = "extra_part_code"
        private const val EXTRA_PERSON_LIST = "extra_person_list"
        private const val SUBMIT_DEBOUNCE_WINDOW = 800L

        fun start(context: Context, partCode: String, personList: List<ProductionSwitchUserDto>) {
            context.startActivity(
                Intent(context, ProductionPersonCheckActivity::class.java)
                    .putExtra(EXTRA_PART_CODE, partCode)
                    .putExtra(EXTRA_PERSON_LIST, ArrayList(personList)),
            )
        }
    }
}
