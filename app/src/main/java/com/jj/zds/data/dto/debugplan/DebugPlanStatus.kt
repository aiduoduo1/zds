package com.jj.zds.data.dto.debugplan

enum class DebugPlanStatus(
    val displayName: String,
) {
    TODO("待执行"),
    DOING("执行中"),
    DONE("已完工"),
    ;

    companion object {
        fun fromCode(code: Int): DebugPlanStatus {
            return when (code) {
                2 -> DOING
                3 -> DONE
                else -> TODO
            }
        }
    }
}
