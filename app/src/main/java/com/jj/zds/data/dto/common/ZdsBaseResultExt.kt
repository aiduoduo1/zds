package com.jj.zds.data.dto.common

import com.jj.common.exception.JiangErrorCode
import com.jj.common.exception.JiangException
import com.jj.common.result.JiangResult

fun <T> ZdsBaseResult<T>.toJiangResult(
    emptyMessage: String = "Response data is empty.",
): JiangResult<T> {
    if (!isSuccess()) {
        return JiangResult.Error(
            JiangException(
                code = code,
                message = message ?: msg ?: "Request failed.",
            ),
        )
    }
    return data?.let {
        JiangResult.Success(it)
    } ?: JiangResult.Error(
        JiangException(
            code = JiangErrorCode.UNKNOWN,
            message = emptyMessage,
        ),
    )
}
