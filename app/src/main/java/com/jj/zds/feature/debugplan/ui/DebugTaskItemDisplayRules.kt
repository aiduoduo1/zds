package com.jj.zds.feature.debugplan.ui

data class DebugTaskItemDisplay(
    val content: String,
    val description: String,
)

object DebugTaskItemDisplayRules {

    private const val MAX_TITLE_CHARS = 5

    fun build(standardOption: String?, description: String?): DebugTaskItemDisplay {
        return DebugTaskItemDisplay(
            content = standardOption?.trim().orEmpty(),
            description = description?.trim().orEmpty(),
        )
    }

    fun shortTitle(title: String): String {
        val value = title.trim()
        return if (value.length > MAX_TITLE_CHARS) {
            value.take(MAX_TITLE_CHARS) + "…"
        } else {
            value
        }
    }
}
