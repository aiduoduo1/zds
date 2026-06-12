package com.jj.zds.data.dto.update

sealed class ZdsVersionCheckResult {
    data class HasUpdate(val data: ZdsApkVersionDto) : ZdsVersionCheckResult()
    data class NoUpdate(val message: String) : ZdsVersionCheckResult()
}
