package com.jj.zds.feature.twolevelscan.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jj.ui.base.JiangToolbarActivity
import com.jj.ui.dialog.JInputDialog
import com.jj.ui.recycler.ext.setupLinear
import com.jj.ui.recycler.multitype.JMultiBindingAdapter
import com.jj.ui.toast.JToast
import com.jj.zds.data.dto.twolevelscan.TwoLevelChildItem
import com.jj.zds.data.dto.twolevelscan.TwoLevelChildRow
import com.jj.zds.data.dto.twolevelscan.TwoLevelParentRow
import com.jj.zds.data.dto.twolevelscan.TwoLevelRow
import com.jj.zds.databinding.ActivityTwoLevelScanBinding
import com.jj.zds.feature.twolevelscan.adapter.TwoLevelChildBinder
import com.jj.zds.feature.twolevelscan.adapter.TwoLevelParentBinder
import com.jj.zds.feature.twolevelscan.data.TwoLevelScanMockData

class TwoLevelScanDemoActivity : JiangToolbarActivity<ActivityTwoLevelScanBinding>() {

    private var scanCode: String = ""
    private val groups = TwoLevelScanMockData.createGroups().toMutableList()
    private val rows = mutableListOf<TwoLevelRow>()
    private val adapter = JMultiBindingAdapter<TwoLevelRow>()
        .addItemBinder(TwoLevelParentBinder(::toggleParent))
        .addItemBinder(TwoLevelChildBinder(::editChild, ::refreshChild, ::deleteChild))

    override fun createViewBinding(inflater: LayoutInflater): ActivityTwoLevelScanBinding {
        return ActivityTwoLevelScanBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.inputScan.setText(scanCode)
        binding.inputScan.onTextChanged {
            scanCode = it
        }
        binding.inputScan.onTextDebounce(delayMillis = 260L) {
            if (it.isScannableCode()) {
                scanAndAdd()
            }
        }
        binding.inputScan.onInputDone {
            scanAndAdd()
        }

        adapter.setDiffCallback(
            areItemsTheSame = { oldItem, newItem -> oldItem.rowId == newItem.rowId },
            areContentsTheSame = { oldItem, newItem -> oldItem == newItem },
        )
        binding.recyclerTree.setupLinear(adapter)
        binding.recyclerTree.addTreeSpacing()
        renderRows()
        binding.inputScan.post {
            binding.inputScan.requestInputFocus(showKeyboard = false)
        }
    }

    override fun toolbarTitle(): CharSequence = "二层扫码"

    override fun toolbarSubtitle(): CharSequence = "Adapter Tree"

    private fun scanAndAdd() {
        val code = binding.inputScan.text().uppercase()
        if (code.isBlank()) {
            JToast.show("请先扫码或输入编码")
            return
        }

        val category = code.substringBefore("-", missingDelimiterValue = code.take(1)).take(1)
        val groupIndex = groups.indexOfFirst { it.categoryCode.equals(category, ignoreCase = true) }
        if (groupIndex < 0) {
            JToast.show("未找到分类：$category")
            return
        }
        if (groups.any { group ->
                group.children.any {
                    it.code.equals(
                        code,
                        ignoreCase = true
                    )
                }
            }) {
            JToast.show("重复扫码：$code")
            return
        }

        val group = groups[groupIndex]
        val child = TwoLevelChildItem(
            id = code,
            code = code,
            name = "${group.categoryCode} 子项 ${group.children.size + 1}",
            remark = "扫码加入 ${group.title}",
        )
        groups[groupIndex] = group.copy(
            expanded = true,
            children = group.children + child,
        )
        binding.inputScan.clearText()
        binding.inputScan.requestInputFocus(showKeyboard = false)
        scanCode = ""
        renderRows()
        scrollToChild(group.id, child.id)
        JToast.show("已加入 ${group.title}")
    }

    private fun toggleParent(parentId: String) {
        val index = groups.indexOfFirst { it.id == parentId }
        if (index < 0) return
        val group = groups[index]
        groups[index] = group.copy(expanded = !group.expanded)
        renderRows()
    }

    private fun editChild(parentId: String, childId: String) {
        val child = findChild(parentId, childId) ?: return
        JInputDialog(this, this)
            .setTitle(child.code)
            .setInputTitle("子项备注")
            .setHint("输入子项备注")
            .setText(child.remark)
            .setOnConfirm { remark ->
                updateChild(parentId, childId) {
                    it.copy(remark = remark.ifBlank { "已编辑" })
                }
            }
            .selectAll()
            .show()
    }

    private fun refreshChild(parentId: String, childId: String) {
        updateChild(parentId, childId) {
            it.copy(refreshCount = it.refreshCount + 1)
        }
        JToast.show("已刷新子项")
    }

    private fun deleteChild(parentId: String, childId: String) {
        val parentIndex = groups.indexOfFirst { it.id == parentId }
        if (parentIndex < 0) return
        val group = groups[parentIndex]
        groups[parentIndex] = group.copy(
            children = group.children.filterNot { it.id == childId },
        )
        renderRows()
        JToast.show("已删除子项")
    }

    private fun updateChild(
        parentId: String,
        childId: String,
        transform: (TwoLevelChildItem) -> TwoLevelChildItem,
    ) {
        val parentIndex = groups.indexOfFirst { it.id == parentId }
        if (parentIndex < 0) return
        val group = groups[parentIndex]
        val childIndex = group.children.indexOfFirst { it.id == childId }
        if (childIndex < 0) return
        val children = group.children.toMutableList()
        children[childIndex] = transform(children[childIndex])
        groups[parentIndex] = group.copy(children = children)
        renderRows()
    }

    private fun findChild(parentId: String, childId: String): TwoLevelChildItem? {
        return groups.firstOrNull { it.id == parentId }
            ?.children
            ?.firstOrNull { it.id == childId }
    }

    private fun renderRows() {
        rows.clear()
        groups.forEach { group ->
            rows += TwoLevelParentRow(group)
            if (group.expanded) {
                rows += group.children.map { child ->
                    TwoLevelChildRow(group.id, child)
                }
            }
        }
        adapter.submitListWithDiff(rows)
    }

    private fun scrollToChild(parentId: String, childId: String) {
        val position = rows.indexOfFirst {
            it is TwoLevelChildRow && it.parentId == parentId && it.child.id == childId
        }
        if (position >= 0) {
            binding.recyclerTree.post {
                binding.recyclerTree.smoothScrollToPosition(position)
            }
        }
    }

    private fun String.isScannableCode(): Boolean {
        return trim().matches(Regex("^[A-Za-z]-[A-Za-z0-9]+$"))
    }

    private fun RecyclerView.addTreeSpacing() {
        if (itemDecorationCount > 0) return
        val space = resources.getDimensionPixelSize(com.jj.ui.R.dimen.j_space_4)
        addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    outRect.bottom = space
                }
            },
        )
    }
}
