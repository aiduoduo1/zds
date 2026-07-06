package com.jj.zds.data.dto.productionplan

import com.jj.zds.data.api.ZdsApi
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.POST

class ProductionCirculationQueryDtosTest {

    @Test
    fun `circulation query request keeps backend field name`() {
        val request = ProductionCirculationQueryRequest(circulationId = "circulation-1")

        assertEquals("circulation-1", request.circulationId)
    }

    @Test
    fun `material detail keeps response fields`() {
        val item = ProductionCirculationMaterialDto(
            id = "1",
            circulationId = "2",
            materialId = "3",
            materialCode = "PRO001",
            okType = 1,
            createTime = "2026-07-03 10:00:00",
        )

        assertEquals("PRO001", item.materialCode)
        assertEquals(1, item.okType)
    }

    @Test
    fun `print log keeps response fields`() {
        val item = ProductionCirculationPrintLogDto(
            id = "1",
            circulationId = "2",
            printTime = "2026-07-03 10:00:00",
            createByName = "管理员",
        )

        assertEquals("管理员", item.createByName)
    }

    @Test
    fun `api paths match openapi contract`() {
        val materialMethod = ZdsApi::class.java.getMethod(
            "getProductionCirculationMaterials",
            ProductionCirculationQueryRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        val printLogMethod = ZdsApi::class.java.getMethod(
            "getProductionCirculationPrintLogs",
            ProductionCirculationQueryRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )

        assertEquals(
            "/pro/matCirculationToMat/matInfo",
            materialMethod.getAnnotation(POST::class.java)?.value,
        )
        assertEquals(
            "/pro/matCirculationPrintLog/printLog",
            printLogMethod.getAnnotation(POST::class.java)?.value,
        )
    }
}
