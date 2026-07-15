package com.jj.zds.data.api

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.jj.common.exception.JiangErrorCode
import com.jj.common.exception.JiangException
import com.jj.common.result.JiangResult
import com.jj.network.call.JiangApiCaller
import com.jj.zds.data.dto.common.ZdsBaseResult
import java.lang.reflect.Type
import okhttp3.ResponseBody

object ZdsRawApiCaller {

    suspend inline fun <reified T> safeBaseCall(
        emptyMessage: String = "Response data is empty.",
        noinline call: suspend () -> ResponseBody,
    ): JiangResult<T> {
        return safeBaseCall(
            dataType = object : TypeToken<T>() {}.type,
            emptyMessage = emptyMessage,
            call = call,
        )
    }

    suspend fun <T> safeBaseCall(
        dataType: Type,
        emptyMessage: String = "Response data is empty.",
        call: suspend () -> ResponseBody,
    ): JiangResult<T> {
        return when (val result = JiangApiCaller.safeApiCall(call)) {
            is JiangResult.Success -> parseBaseResponse(
                responseText = result.data.string(),
                dataType = dataType,
                emptyMessage = emptyMessage,
            )

            is JiangResult.Error -> result
            JiangResult.Loading -> JiangResult.Loading
            JiangResult.Empty -> JiangResult.Empty
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> parseBaseResponse(
        responseText: String,
        dataType: Type,
        emptyMessage: String,
    ): JiangResult<T> {
        val json = runCatching { JsonParser.parseString(responseText) }.getOrElse { throwable ->
            return JiangResult.Error(
                JiangException(
                    code = JiangErrorCode.UNKNOWN,
                    message = "Invalid JSON response: ${responseText.toBriefText()}",
                    throwable = throwable,
                ),
            )
        }
        val wrapper = json.asZdsWrapperOrNull()
        if (wrapper != null && !wrapper.isSuccess()) {
            return JiangResult.Error(
                JiangException(
                    code = wrapper.code,
                    message = wrapper.message ?: "Request failed.",
                ),
            )
        }
        if (dataType == Unit::class.java) {
            return JiangResult.Success(Unit as T)
        }

        val dataJson = wrapper?.data ?: json
        if (dataJson == null || dataJson.isJsonNull) {
            return JiangResult.Error(
                JiangException(
                    code = JiangErrorCode.UNKNOWN,
                    message = emptyMessage,
                ),
            )
        }
        return runCatching {
            JiangResult.Success(gson.fromJson<T>(dataJson, dataType))
        }.getOrElse { throwable ->
            JiangResult.Error(
                JiangException(
                    code = JiangErrorCode.UNKNOWN,
                    message = buildParseErrorMessage(dataType, dataJson, throwable),
                    throwable = throwable,
                ),
            )
        }
    }

    private fun JsonElement.asZdsWrapperOrNull(): RawBaseResponse? {
        val jsonObject = this as? JsonObject ?: return null
        if (!jsonObject.has("code")) {
            return null
        }
        return RawBaseResponse(
            code = jsonObject.get("code")?.toIntOrNull() ?: JiangErrorCode.UNKNOWN,
            message = jsonObject.get("data")?.findMessageOrNull()
                ?: jsonObject.get("message")?.toTextOrNull()
                ?: jsonObject.get("msg")?.toTextOrNull(),
            data = jsonObject.get("data"),
        )
    }

    private fun JsonElement.findMessageOrNull(): String? {
        val jsonObject = this as? JsonObject ?: return null
        return jsonObject.get("message")?.toTextOrNull()
            ?: jsonObject.get("msg")?.toTextOrNull()
            ?: jsonObject.get("error")?.toTextOrNull()
            ?: jsonObject.get("reason")?.toTextOrNull()
    }

    private fun JsonElement.toIntOrNull(): Int? {
        return runCatching {
            if (isJsonPrimitive) {
                asJsonPrimitive.asInt
            } else {
                null
            }
        }.getOrNull()
    }

    private fun JsonElement.toTextOrNull(): String? {
        return runCatching {
            if (isJsonNull) {
                null
            } else {
                asString.takeIf { it.isNotBlank() && it != "null" }
            }
        }.getOrNull()
    }

    private fun RawBaseResponse.isSuccess(): Boolean {
        return code == ZdsBaseResult.CODE_SUCCESS
    }

    private fun buildParseErrorMessage(
        dataType: Type,
        dataJson: JsonElement,
        throwable: Throwable,
    ): String {
        val detail = (throwable as? JsonSyntaxException)?.cause?.message
            ?: throwable.message
            ?: "unknown parse error"
        return "Response data parse failed. Expected $dataType, " +
            "actual data is ${dataJson.toBriefText()}. Detail: $detail"
    }

    private fun JsonElement.toBriefText(maxLength: Int = 300): String {
        return toString().toBriefText(maxLength)
    }

    private fun String.toBriefText(maxLength: Int = 300): String {
        val text = trim()
        return if (text.length <= maxLength) {
            text
        } else {
            text.take(maxLength) + "..."
        }
    }

    private data class RawBaseResponse(
        val code: Int,
        val message: String?,
        val data: JsonElement?,
    )

    private val gson = Gson()
}
