package com.jj.zds.feature.singledebug.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jj.ui.recycler.multitype.JItemBinder
import com.jj.zds.R
import com.jj.zds.data.dto.singledebug.CheckResult
import com.jj.zds.data.dto.singledebug.PhotoCheckItem
import com.jj.zds.data.dto.singledebug.ScopeCheckItem
import com.jj.zds.data.dto.singledebug.SwitchCheckItem
import com.jj.zds.data.dto.singledebug.TextCheckItem
import com.jj.zds.data.dto.singledebug.ValueCheckItem
import com.jj.zds.databinding.ItemSingleDebugCheckPhotoBinding
import com.jj.zds.databinding.ItemSingleDebugCheckScopeBinding
import com.jj.zds.databinding.ItemSingleDebugCheckSwitchBinding
import com.jj.zds.databinding.ItemSingleDebugCheckTextBinding
import com.jj.zds.databinding.ItemSingleDebugCheckValueBinding

class SwitchCheckBinder(
    private val onResultChange: (id: String, result: CheckResult) -> Unit,
) : JItemBinder<SwitchCheckItem, ItemSingleDebugCheckSwitchBinding>() {

    override fun isForViewType(item: Any): Boolean = item is SwitchCheckItem

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckSwitchBinding {
        return ItemSingleDebugCheckSwitchBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemSingleDebugCheckSwitchBinding,
        item: SwitchCheckItem,
        position: Int,
    ) {
        binding.viewStatus.visibility = android.view.View.GONE
        binding.tvDescription.visibility = android.view.View.GONE
        binding.tvTitle.text = item.title
        binding.btnPass.applyResultButton(item.result == CheckResult.PASS, CheckResult.PASS)
        binding.btnFail.applyResultButton(item.result == CheckResult.FAIL, CheckResult.FAIL)
        binding.btnPass.setOnClickListener { onResultChange(item.id, CheckResult.PASS) }
        binding.btnFail.setOnClickListener { onResultChange(item.id, CheckResult.FAIL) }
    }
}

class ValueCheckBinder(
    private val onValueChange: (id: String, value: String, result: CheckResult) -> Unit,
    private val onFocus: (id: String) -> Unit,
) : JItemBinder<ValueCheckItem, ItemSingleDebugCheckValueBinding>() {

    override fun isForViewType(item: Any): Boolean = item is ValueCheckItem

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckValueBinding {
        return ItemSingleDebugCheckValueBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemSingleDebugCheckValueBinding,
        item: ValueCheckItem,
        position: Int,
    ) {
        binding.viewStatus.visibility = android.view.View.GONE
        binding.tvDescription.visibility = android.view.View.GONE
        binding.tvTitle.text = item.title
        binding.tvUnit.text = item.unit
        binding.tvReference.text = "参考值：${item.referenceValue} ${item.unit}"
        binding.editValue.replaceAfterTextChanged(item.inputValue) { value ->
            onValueChange(item.id, value, value.toValueResult(item.referenceValue))
        }
        binding.editValue.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) onFocus(item.id)
        }
    }
}

class PhotoCheckBinder(
    private val onPhotoClick: (id: String) -> Unit,
) : JItemBinder<PhotoCheckItem, ItemSingleDebugCheckPhotoBinding>() {

    override fun isForViewType(item: Any): Boolean = item is PhotoCheckItem

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckPhotoBinding {
        return ItemSingleDebugCheckPhotoBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemSingleDebugCheckPhotoBinding,
        item: PhotoCheckItem,
        position: Int,
    ) {
        binding.viewStatus.visibility = android.view.View.GONE
        binding.tvDescription.visibility = android.view.View.GONE
        binding.tvTitle.text = item.title
        binding.tvPhotoState.text = if (item.photoCount > 0) {
            "${item.photoCount}张"
        } else {
            "未拍"
        }
        binding.btnPhoto.text = if (item.photoCount > 0) "继续拍照" else "拍照"
        binding.btnPhoto.applySmallActionButton(item.photoCount > 0)
        binding.btnPhoto.setOnClickListener { onPhotoClick(item.id) }
    }
}

class TextCheckBinder(
    private val onRemarkClick: (id: String) -> Unit,
) : JItemBinder<TextCheckItem, ItemSingleDebugCheckTextBinding>() {

    override fun isForViewType(item: Any): Boolean = item is TextCheckItem

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckTextBinding {
        return ItemSingleDebugCheckTextBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemSingleDebugCheckTextBinding,
        item: TextCheckItem,
        position: Int,
    ) {
        binding.viewStatus.visibility = android.view.View.GONE
        binding.tvDescription.visibility = android.view.View.GONE
        binding.tvTitle.text = item.title
        binding.tvRemarkState.text = if (item.remark.isBlank()) "未填写" else item.remark
        binding.btnRemark.text = if (item.remark.isBlank()) "填写" else "修改"
        binding.btnRemark.applySmallActionButton(item.result == CheckResult.PASS)
        binding.btnRemark.setOnClickListener { onRemarkClick(item.id) }
    }
}

class ScopeCheckBinder(
    private val onConfirmClick: (id: String) -> Unit,
) : JItemBinder<ScopeCheckItem, ItemSingleDebugCheckScopeBinding>() {

    override fun isForViewType(item: Any): Boolean = item is ScopeCheckItem

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckScopeBinding {
        return ItemSingleDebugCheckScopeBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemSingleDebugCheckScopeBinding,
        item: ScopeCheckItem,
        position: Int,
    ) {
        binding.tvType.text = item.typeLabel
        binding.tvTitle.text = item.title
        binding.btnConfirm.text = if (item.confirmed) "已确认" else "确认协议"
        binding.btnConfirm.applySmallActionButton(item.confirmed)
        binding.btnConfirm.setOnClickListener { onConfirmClick(item.id) }
    }
}

private fun TextView.applyResultButton(selected: Boolean, result: CheckResult) {
    val backgroundRes = when {
        !selected -> com.jj.ui.R.drawable.j_bg_button_secondary
        result == CheckResult.FAIL -> com.jj.ui.R.drawable.j_bg_button_danger
        else -> com.jj.ui.R.drawable.j_bg_button_primary
    }
    val textColorRes = if (selected) {
        com.jj.ui.R.color.j_color_text_white
    } else {
        com.jj.ui.R.color.j_color_primary
    }
    setBackgroundResource(backgroundRes)
    setTextColor(ContextCompat.getColor(context, textColorRes))
}

private fun TextView.applySmallActionButton(selected: Boolean) {
    setBackgroundResource(
        if (selected) {
            com.jj.ui.R.drawable.j_bg_button_primary
        } else {
            com.jj.ui.R.drawable.j_bg_button_secondary
        },
    )
    setTextColor(
        ContextCompat.getColor(
            context,
            if (selected) {
                com.jj.ui.R.color.j_color_text_white
            } else {
                com.jj.ui.R.color.j_color_primary
            },
        ),
    )
}

private fun EditText.replaceAfterTextChanged(
    text: String,
    listener: (String) -> Unit,
) {
    (getTag(R.id.tag_single_debug_text_watcher) as? TextWatcher)?.let(::removeTextChangedListener)
    if (this.text?.toString() != text) {
        setText(text)
        setSelection(this.text?.length ?: 0)
    }
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            listener(s?.toString().orEmpty())
        }
    }
    addTextChangedListener(watcher)
    setTag(R.id.tag_single_debug_text_watcher, watcher)
}

private fun String.toValueResult(reference: String): CheckResult {
    if (isBlank()) return CheckResult.PENDING
    return if (toBigDecimalOrNull() == reference.toBigDecimalOrNull()) {
        CheckResult.PASS
    } else {
        CheckResult.FAIL
    }
}
