package com.jj.zds.data.dto.device

data class OscilloscopeOptionDto(
    val id: Long = 0,
    val deviceKey: String = "",
    val deviceName: String = "",
    val host: String = "",
    val agentCode: String = "",
    val computerName: String = "",
)

fun OscilloscopeOptionDto.displayLabel(): String = buildString {
    append(deviceName.ifBlank { deviceKey })
    append("\n")
    append(computerName.ifBlank { agentCode })
    if (host.isNotBlank()) append("  ·  ").append(host)
}
