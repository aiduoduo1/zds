package com.jj.zds.data.dto.singledebug

sealed interface DebugCheckItem {
    val id: String
    val title: String
    val typeLabel: String
    val result: CheckResult
}

data class SwitchCheckItem(
    override val id: String,
    override val title: String,
    override val result: CheckResult = CheckResult.PENDING,
) : DebugCheckItem {
    override val typeLabel: String = "开关"
}

data class ValueCheckItem(
    override val id: String,
    override val title: String,
    val referenceValue: String,
    val unit: String,
    val inputValue: String = "",
    override val result: CheckResult = CheckResult.PENDING,
) : DebugCheckItem {
    override val typeLabel: String = "数值"
}

data class PhotoCheckItem(
    override val id: String,
    override val title: String,
    val photoCount: Int = 0,
    override val result: CheckResult = CheckResult.PENDING,
) : DebugCheckItem {
    override val typeLabel: String = "拍照"
}

data class TextCheckItem(
    override val id: String,
    override val title: String,
    val remark: String = "",
    override val result: CheckResult = CheckResult.PENDING,
) : DebugCheckItem {
    override val typeLabel: String = "备注"
}

data class ScopeCheckItem(
    override val id: String,
    override val title: String,
    val confirmed: Boolean = false,
    override val result: CheckResult = CheckResult.PENDING,
) : DebugCheckItem {
    override val typeLabel: String = "示波器"
}
