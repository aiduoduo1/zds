package com.jj.zds.feature.repair.ui

data class RepairTimelineText(
    val content: String,
    val status: String,
)

object RepairTimelineDisplay {

    private const val STATUS_MARKER = "状态："

    fun parse(description: String?): RepairTimelineText {
        val normalized = description.orEmpty().trim()
        val markerIndex = normalized.lastIndexOf(STATUS_MARKER)
        if (markerIndex < 0) {
            return RepairTimelineText(content = normalized, status = "")
        }
        val status = normalized.substring(markerIndex + STATUS_MARKER.length).trim()
        if (status.isBlank()) {
            return RepairTimelineText(content = normalized, status = "")
        }
        return RepairTimelineText(
            content = normalized.substring(0, markerIndex).trim(),
            status = status,
        )
    }
}
