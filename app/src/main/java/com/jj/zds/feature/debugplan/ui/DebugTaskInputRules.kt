package com.jj.zds.feature.debugplan.ui

internal enum class DebugTaskCollectType {
    CONFIRM,
    NUM,
    INPUT,
    PHOTO,
}

internal object DebugTaskInputRules {
    const val MAX_INPUT_LENGTH = 50

    private val numPattern = Regex("^\\d{0,8}(\\.\\d{0,2})?$")

    fun resolveType(
        collectType: String?,
        minValue: Number?,
        maxValue: Number?,
    ): DebugTaskCollectType {
        return when (collectType.orEmpty().trim().lowercase()) {
            "confirm" -> DebugTaskCollectType.CONFIRM
            "num", "number", "value", "数值" -> DebugTaskCollectType.NUM
            "input", "text", "remark", "textarea", "文本", "说明" -> DebugTaskCollectType.INPUT
            "photo", "image", "picture", "file", "拍照", "照片", "图片" -> DebugTaskCollectType.PHOTO
            else -> if (minValue != null || maxValue != null) {
                DebugTaskCollectType.NUM
            } else {
                DebugTaskCollectType.CONFIRM
            }
        }
    }

    fun isValidNumCandidate(value: String): Boolean = numPattern.matches(value)

    fun isInputMatched(value: String, _reference: String): Boolean {
        return value.isNotBlank()
    }
}
