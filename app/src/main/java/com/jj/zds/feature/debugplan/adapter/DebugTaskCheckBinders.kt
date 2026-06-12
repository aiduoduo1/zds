package com.jj.zds.feature.debugplan.adapter

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import com.jj.ui.recycler.multitype.JItemBinder
import com.jj.zds.R
import com.jj.zds.databinding.ItemSingleDebugCheckPhotoBinding
import com.jj.zds.databinding.ItemSingleDebugCheckSwitchBinding
import com.jj.zds.databinding.ItemSingleDebugCheckTextBinding
import com.jj.zds.databinding.ItemSingleDebugCheckValueBinding
import com.jj.zds.feature.debugplan.data.DebugTaskCheckResult
import com.jj.zds.feature.debugplan.data.DebugTaskPhotoRow
import com.jj.zds.feature.debugplan.data.DebugTaskSwitchRow
import com.jj.zds.feature.debugplan.data.DebugTaskTextRow
import com.jj.zds.feature.debugplan.data.DebugTaskValueRow
import com.jj.zds.feature.photo.PhotoUploadMode
import com.jj.zds.feature.repair.data.buildRepairFileUrl

class DebugTaskSwitchBinder(
    private val onResultChange: (id: String, result: DebugTaskCheckResult) -> Unit,
) : JItemBinder<DebugTaskSwitchRow, ItemSingleDebugCheckSwitchBinding>() {

    override fun isForViewType(item: Any): Boolean = item is DebugTaskSwitchRow

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckSwitchBinding {
        return ItemSingleDebugCheckSwitchBinding.inflate(inflater, parent, false)
    }

    /**
     * 开关类检查项只有 PASS/FAIL 两个动作，点击后把结果交回 Activity 统一维护列表状态。
     */
    override fun bind(
        binding: ItemSingleDebugCheckSwitchBinding,
        item: DebugTaskSwitchRow,
        position: Int,
    ) {
        binding.viewStatus.bindStatus(item.result)
        binding.tvTitle.text = item.title
        binding.tvDescription.bindDescription(item.description)
        binding.btnPass.applyResultButton(item.result == DebugTaskCheckResult.PASS, DebugTaskCheckResult.PASS)
        binding.btnFail.applyResultButton(item.result == DebugTaskCheckResult.FAIL, DebugTaskCheckResult.FAIL)
        binding.btnPass.setOnClickListener { onResultChange(item.checkId, DebugTaskCheckResult.PASS) }
        binding.btnFail.setOnClickListener { onResultChange(item.checkId, DebugTaskCheckResult.FAIL) }
    }
}

class DebugTaskValueBinder(
    private val onValueChange: (id: String, value: String, result: DebugTaskCheckResult) -> Unit,
    private val onFocus: (id: String) -> Unit,
) : JItemBinder<DebugTaskValueRow, ItemSingleDebugCheckValueBinding>() {

    override fun isForViewType(item: Any): Boolean = item is DebugTaskValueRow

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckValueBinding {
        return ItemSingleDebugCheckValueBinding.inflate(inflater, parent, false)
    }

    /**
     * 数值类检查项在输入变化时即时计算 PASS/FAIL/PENDING，方便现场边输边看结果。
     */
    override fun bind(
        binding: ItemSingleDebugCheckValueBinding,
        item: DebugTaskValueRow,
        position: Int,
    ) {
        binding.viewStatus.bindStatus(item.result)
        binding.tvTitle.text = item.title
        binding.tvUnit.text = item.unit
        binding.tvReference.text = item.referenceValue.ifBlank { "参考值：-" }
        binding.tvDescription.bindDescription(item.description)
        binding.editValue.replaceAfterTextChanged(item.inputValue) { value ->
            val result = value.toValueResult(item.dto.minValue, item.dto.maxValue)
            binding.viewStatus.bindStatus(result)
            onValueChange(item.checkId, value, result)
        }
        binding.editValue.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) onFocus(item.checkId)
        }
    }
}

class DebugTaskTextBinder(
    private val onRemarkClick: (id: String) -> Unit,
) : JItemBinder<DebugTaskTextRow, ItemSingleDebugCheckTextBinding>() {

    override fun isForViewType(item: Any): Boolean = item is DebugTaskTextRow

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckTextBinding {
        return ItemSingleDebugCheckTextBinding.inflate(inflater, parent, false)
    }

    /**
     * 文本类检查项通过弹窗填写备注；是否填写备注决定当前项是否完成。
     */
    override fun bind(
        binding: ItemSingleDebugCheckTextBinding,
        item: DebugTaskTextRow,
        position: Int,
    ) {
        binding.viewStatus.bindStatus(item.result)
        binding.tvTitle.text = item.title
        binding.tvRemarkState.text = item.remark.ifBlank { "未填写" }
        binding.tvDescription.bindDescription(item.description)
        binding.btnRemark.text = if (item.remark.isBlank()) "填写" else "修改"
        binding.btnRemark.applySmallActionButton(item.result == DebugTaskCheckResult.PASS)
        binding.btnRemark.setOnClickListener { onRemarkClick(item.checkId) }
    }
}

class DebugTaskPhotoBinder(
    private val onPhotoClick: (id: String) -> Unit,
    private val onUploadModeChange: (id: String, mode: PhotoUploadMode) -> Unit,
    private val onPreviewClick: (id: String, index: Int) -> Unit,
    private val onDeleteClick: (id: String, index: Int) -> Unit,
    private val tokenProvider: () -> String,
) : JItemBinder<DebugTaskPhotoRow, ItemSingleDebugCheckPhotoBinding>() {

    override fun isForViewType(item: Any): Boolean = item is DebugTaskPhotoRow

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ItemSingleDebugCheckPhotoBinding {
        return ItemSingleDebugCheckPhotoBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        binding: ItemSingleDebugCheckPhotoBinding,
        item: DebugTaskPhotoRow,
        position: Int,
    ) {
        binding.viewStatus.bindStatus(item.result)
        binding.tvTitle.text = item.title
        binding.tvDescription.bindDescription(item.description)
        binding.tvPhotoState.text = if (item.photos.isNotEmpty()) {
            "${item.photos.size}张"
        } else {
            "未拍"
        }
        binding.btnPhoto.text = when {
            item.uploading -> "上传中"
            item.photos.isNotEmpty() -> "继续拍照"
            else -> "拍照"
        }
        binding.btnPhoto.isEnabled = !item.uploading && item.photos.size < MAX_PHOTO_COUNT
        binding.btnPhoto.alpha = if (binding.btnPhoto.isEnabled) 1f else 0.6f
        binding.btnPhoto.applySmallActionButton(item.result == DebugTaskCheckResult.PASS)
        binding.btnPhoto.setOnClickListener { onPhotoClick(item.checkId) }
        binding.groupPhotoUploadMode.setOnCheckedChangeListener(null)
        binding.groupPhotoUploadMode.check(
            if (item.uploadMode == PhotoUploadMode.ORIGINAL) {
                binding.rbPhotoOriginal.id
            } else {
                binding.rbPhotoCompressed.id
            },
        )
        binding.groupPhotoUploadMode.isEnabled = !item.uploading
        binding.rbPhotoCompressed.isEnabled = !item.uploading
        binding.rbPhotoOriginal.isEnabled = !item.uploading
        binding.groupPhotoUploadMode.alpha = if (item.uploading) 0.6f else 1f
        binding.groupPhotoUploadMode.setOnCheckedChangeListener { _, checkedId ->
            onUploadModeChange(
                item.checkId,
                if (checkedId == binding.rbPhotoOriginal.id) {
                    PhotoUploadMode.ORIGINAL
                } else {
                    PhotoUploadMode.COMPRESSED
                },
            )
        }
        binding.layoutPhotoList.removeAllViews()
        binding.scrollPhotoList.visibility = if (item.photos.isEmpty()) View.GONE else View.VISIBLE
        item.photos.forEachIndexed { index, photo ->
            binding.layoutPhotoList.addView(createPhotoThumb(binding.root, item.checkId, index, photo, item.uploading))
        }
    }

    private fun createPhotoThumb(
        root: View,
        checkId: String,
        index: Int,
        photo: com.jj.zds.feature.debugplan.data.DebugTaskPhotoItem,
        uploading: Boolean,
    ): View {
        val container = FrameLayout(root.context).apply {
            layoutParams = LinearLayout.LayoutParams(root.dp(72), root.dp(72)).apply {
                marginEnd = root.dp(8)
            }
        }
        val imageView = ImageView(root.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(com.jj.ui.R.drawable.j_bg_input)
            photo.localFile?.takeIf { it.exists() }?.let {
                load(it)
            } ?: buildRepairFileUrl(photo.fileId, tokenProvider())?.let {
                load(it)
            }
            setOnClickListener { onPreviewClick(checkId, index) }
        }
        container.addView(imageView)
        if (!uploading) {
            val deleteView = TextView(root.context).apply {
                layoutParams = FrameLayout.LayoutParams(root.dp(22), root.dp(22), Gravity.TOP or Gravity.END)
                gravity = Gravity.CENTER
                text = "×"
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(0x99000000.toInt())
                setOnClickListener { onDeleteClick(checkId, index) }
            }
            container.addView(deleteView)
        }
        return container
    }

    private companion object {
        const val MAX_PHOTO_COUNT = 9
    }
}

private fun TextView.applyResultButton(selected: Boolean, result: DebugTaskCheckResult) {
    val backgroundRes = when {
        !selected -> com.jj.ui.R.drawable.j_bg_button_secondary
        result == DebugTaskCheckResult.FAIL -> com.jj.ui.R.drawable.j_bg_button_danger
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

private fun TextView.bindDescription(description: String) {
    val textValue = description.trim()
    text = textValue
    visibility = if (textValue.isBlank()) View.GONE else View.VISIBLE
}

private fun View.bindStatus(result: DebugTaskCheckResult) {
    visibility = when (result) {
        DebugTaskCheckResult.PENDING -> View.GONE
        DebugTaskCheckResult.PASS,
        DebugTaskCheckResult.FAIL -> View.VISIBLE
    }
    setBackgroundColor(
        when (result) {
            DebugTaskCheckResult.PASS -> Color.rgb(38, 166, 91)
            DebugTaskCheckResult.FAIL -> Color.rgb(229, 57, 53)
            DebugTaskCheckResult.PENDING -> Color.TRANSPARENT
        },
    )
}

private fun View.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

/**
 * RecyclerView 复用输入框时先移除旧 TextWatcher，避免滚动后重复回调或写错行。
 */
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

/**
 * 数值判定规则：空值待处理，非数字失败，超过上下限失败，其余通过。
 */
private fun String.toValueResult(minValue: Int?, maxValue: Int?): DebugTaskCheckResult {
    if (isBlank()) return DebugTaskCheckResult.PENDING
    val number = toBigDecimalOrNull() ?: return DebugTaskCheckResult.FAIL
    minValue?.let {
        if (number < it.toBigDecimal()) return DebugTaskCheckResult.FAIL
    }
    maxValue?.let {
        if (number > it.toBigDecimal()) return DebugTaskCheckResult.FAIL
    }
    return DebugTaskCheckResult.PASS
}
