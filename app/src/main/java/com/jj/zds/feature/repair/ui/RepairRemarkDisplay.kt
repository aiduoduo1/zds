package com.jj.zds.feature.repair.ui

object RepairRemarkDisplay {

    fun format(value: String?): String {
        val text = value.orEmpty()
        val separators = if ('@' in text) charArrayOf('@') else charArrayOf(',', '，')
        return text
            .split(*separators)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}
