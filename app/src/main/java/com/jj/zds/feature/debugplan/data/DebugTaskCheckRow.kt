package com.jj.zds.feature.debugplan.data

import com.jj.zds.data.dto.debugplan.DebugCheckItemDto
import com.jj.zds.feature.photo.PhotoUploadMode
import java.io.File

enum class DebugTaskCheckResult {
    PENDING,
    PASS,
    FAIL,
}

sealed interface DebugTaskCheckRow {
    val rowId: String
    val checkId: String
    val typeLabel: String
    val title: String
    val description: String
    val dto: DebugCheckItemDto
    val result: DebugTaskCheckResult
}

data class DebugTaskSwitchRow(
    override val rowId: String,
    override val checkId: String,
    override val typeLabel: String,
    override val title: String,
    override val description: String = "",
    override val dto: DebugCheckItemDto,
    override val result: DebugTaskCheckResult = DebugTaskCheckResult.PENDING,
) : DebugTaskCheckRow

data class DebugTaskValueRow(
    override val rowId: String,
    override val checkId: String,
    override val typeLabel: String,
    override val title: String,
    override val description: String = "",
    override val dto: DebugCheckItemDto,
    val inputValue: String = "",
    val referenceValue: String = "",
    val unit: String = "",
    override val result: DebugTaskCheckResult = DebugTaskCheckResult.PENDING,
) : DebugTaskCheckRow

data class DebugTaskTextRow(
    override val rowId: String,
    override val checkId: String,
    override val typeLabel: String,
    override val title: String,
    override val description: String = "",
    override val dto: DebugCheckItemDto,
    val remark: String = "",
    val expectedText: String = "",
    override val result: DebugTaskCheckResult = DebugTaskCheckResult.PENDING,
) : DebugTaskCheckRow

data class DebugTaskPhotoRow(
    override val rowId: String,
    override val checkId: String,
    override val typeLabel: String,
    override val title: String,
    override val description: String = "",
    override val dto: DebugCheckItemDto,
    val photos: List<DebugTaskPhotoItem> = emptyList(),
    val uploadMode: PhotoUploadMode = PhotoUploadMode.default(),
    val uploading: Boolean = false,
    override val result: DebugTaskCheckResult = DebugTaskCheckResult.PENDING,
) : DebugTaskCheckRow

data class DebugTaskPhotoItem(
    val fileId: String,
    val localFile: File? = null,
)
