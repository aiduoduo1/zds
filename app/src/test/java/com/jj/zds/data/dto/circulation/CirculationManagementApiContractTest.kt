package com.jj.zds.data.dto.circulation

import com.jj.zds.data.api.ZdsApi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import retrofit2.http.POST

class CirculationManagementApiContractTest {

    @Test
    fun `second scan requests keep circulation id`() {
        val combine = CirculationCombineFromCheckRequest("QR-2", "MAIN-1")
        val product = CirculationProductCheckRequest("MAIN-1", "PRO-1")

        assertEquals("MAIN-1", combine.mainCirculationId)
        assertEquals("MAIN-1", product.circulationId)
    }

    @Test
    fun `add and remove reason paths match contract`() {
        assertEquals(
            "/pro/matCirculation/addReason",
            postPath("getCirculationAddReasons"),
        )
        assertEquals(
            "/pro/matCirculation/removeReason",
            postPath("getCirculationRemoveReasons"),
        )
    }

    @Test
    fun `add and remove product checks return object`() {
        val addType = apiResultDataType("checkCirculationAddProduct")
        val removeType = apiResultDataType("checkCirculationRemoveProduct")

        assertEquals(CirculationProductCheckDto::class.java, addType)
        assertEquals(CirculationProductCheckDto::class.java, removeType)
    }

    private fun postPath(name: String): String? {
        return ZdsApi::class.java.methods
            .first { it.name == name }
            .getAnnotation(POST::class.java)
            ?.value
    }

    private fun apiResultDataType(name: String): Type {
        val continuationType = ZdsApi::class.java.methods
            .first { it.name == name }
            .genericParameterTypes
            .last() as ParameterizedType
        val resultType = continuationType.actualTypeArguments.first().unwrapWildcard() as ParameterizedType
        return resultType.actualTypeArguments.first()
    }

    private fun Type.unwrapWildcard(): Type {
        return if (this is WildcardType) {
            lowerBounds.firstOrNull() ?: upperBounds.first()
        } else {
            this
        }
    }
}
