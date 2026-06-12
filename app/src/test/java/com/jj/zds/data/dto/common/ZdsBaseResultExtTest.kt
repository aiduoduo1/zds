package com.jj.zds.data.dto.common

import com.jj.common.result.JiangResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZdsBaseResultExtTest {

    @Test
    fun toJiangResultReturnsSuccessWhenCodeIsSuccess() {
        val result = ZdsBaseResult(
            code = ZdsBaseResult.CODE_SUCCESS,
            message = "成功",
            data = "token",
        ).toJiangResult()

        assertTrue(result is JiangResult.Success<*>)
        assertEquals("token", (result as JiangResult.Success<*>).data)
    }

    @Test
    fun toJiangResultReturnsErrorWhenCodeFails() {
        val result = ZdsBaseResult<String>(
            code = 500,
            message = "login failed",
        ).toJiangResult()

        assertTrue(result is JiangResult.Error)
        assertEquals(500, (result as JiangResult.Error).exception.code)
        assertEquals("login failed", result.exception.message)
    }
}
