package com.jj.zds.data.dto.circulation

import com.jj.zds.data.api.ZdsApi
import org.junit.Assert.assertEquals
import org.junit.Test
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

    private fun postPath(name: String): String? {
        return ZdsApi::class.java.methods
            .first { it.name == name }
            .getAnnotation(POST::class.java)
            ?.value
    }
}
