package com.jj.zds.feature.repair.data

fun buildRepairFileUrl(
    fileId: String?,
    token: String,
    baseUrl: String = FINAL_MES_BASE_URL,
): String? {
    val id = fileId?.takeIf { it.isNotBlank() } ?: return null
    return "$baseUrl/task/taskFile/download/$id?token=$token"
}

private const val FINAL_MES_BASE_URL = "http://192.168.1.216:81"
