package com.jj.zds.data.dto.common

data class ZdsBaseResult<T>(
    val code: Int,
    val message: String? = null,
    val msg: String? = null,
    val total: Int? = null,
    val data: T? = null,
) {

    fun isSuccess(): Boolean = code == CODE_SUCCESS

    companion object {
        const val CODE_SUCCESS = 200
    }
}
