package com.jj.zds.feature.repair.data

import com.jj.zds.app.ZdsApplication

fun buildRepairFileUrl(
    fileId: String?,
    token: String,
): String? {
    val id = fileId?.takeIf { it.isNotBlank() } ?: return null
    return ZdsApplication.buildFileDownloadUrl(id, token)
}
