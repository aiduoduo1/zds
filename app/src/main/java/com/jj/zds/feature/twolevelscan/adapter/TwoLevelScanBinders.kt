package com.jj.zds.feature.twolevelscan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.recycler.multitype.JItemBinder
import com.jj.ui.widget.JStatusType
import com.jj.zds.data.dto.twolevelscan.TwoLevelChildRow
import com.jj.zds.data.dto.twolevelscan.TwoLevelParentRow
import com.jj.zds.databinding.ItemTwoLevelChildBinding
import com.jj.zds.databinding.ItemTwoLevelParentBinding

class TwoLevelParentBinder(
    private val onToggle: (parentId: String) -> Unit,
) : JItemBinder<TwoLevelParentRow, ItemTwoLevelParentBinding>() {

    override fun isForViewType(item: Any): Boolean = item is TwoLevelParentRow

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemTwoLevelParentBinding {
        return ItemTwoLevelParentBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemTwoLevelParentBinding,
        item: TwoLevelParentRow,
        position: Int,
    ) {
        val group = item.group
        binding.tvToggle.text = if (group.expanded) "∨" else ">"
        binding.tvTitle.text = group.title
        binding.tvSubtitle.text = "${group.categoryCode} · ${group.description}"
        binding.tagCount.setStatus("已扫 ${group.children.size}", if (group.children.isEmpty()) JStatusType.NEUTRAL else JStatusType.INFO)
        binding.root.setOnClickListener { onToggle(group.id) }
        binding.tvToggle.setOnClickListener { onToggle(group.id) }
    }
}

class TwoLevelChildBinder(
    private val onEdit: (parentId: String, childId: String) -> Unit,
    private val onRefresh: (parentId: String, childId: String) -> Unit,
    private val onDelete: (parentId: String, childId: String) -> Unit,
) : JItemBinder<TwoLevelChildRow, ItemTwoLevelChildBinding>() {

    override fun isForViewType(item: Any): Boolean = item is TwoLevelChildRow

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemTwoLevelChildBinding {
        return ItemTwoLevelChildBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemTwoLevelChildBinding,
        item: TwoLevelChildRow,
        position: Int,
    ) {
        val child = item.child
        binding.tvCode.text = "${child.code} · ${child.name}"
        binding.tvRemark.text = buildString {
            append(child.remark)
            if (child.refreshCount > 0) {
                append(" · 刷新 ")
                append(child.refreshCount)
                append(" 次")
            }
        }
        binding.btnEdit.applyActionButton(ActionTone.NORMAL)
        binding.btnRefresh.applyActionButton(ActionTone.INFO)
        binding.btnDelete.applyActionButton(ActionTone.DANGER)
        binding.btnEdit.setOnClickListener { onEdit(item.parentId, child.id) }
        binding.btnRefresh.setOnClickListener { onRefresh(item.parentId, child.id) }
        binding.btnDelete.setOnClickListener { onDelete(item.parentId, child.id) }
    }
}

private enum class ActionTone {
    NORMAL,
    INFO,
    DANGER,
}

private fun TextView.applyActionButton(tone: ActionTone) {
    val backgroundRes = when (tone) {
        ActionTone.NORMAL -> com.jj.ui.R.drawable.j_bg_button_secondary
        ActionTone.INFO -> com.jj.ui.R.drawable.j_bg_button_primary
        ActionTone.DANGER -> com.jj.ui.R.drawable.j_bg_button_danger
    }
    val textColorRes = if (tone == ActionTone.NORMAL) {
        com.jj.ui.R.color.j_color_primary
    } else {
        com.jj.ui.R.color.j_color_text_white
    }
    setBackgroundResource(backgroundRes)
    setTextColor(ContextCompat.getColor(context, textColorRes))
}
