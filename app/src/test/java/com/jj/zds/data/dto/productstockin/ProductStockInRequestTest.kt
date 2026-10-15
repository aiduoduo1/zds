package com.jj.zds.data.dto.productstockin

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductStockInRequestTest {

    @Test
    fun `request keeps the scanned flow ticket or product code in qrCode`() {
        assertEquals("FC20260729000002", ProductStockInRequest("FC20260729000002").qrCode)
    }

    @Test
    fun `response exposes records from the 已入库 field`() {
        val records = listOf(ProductStockInRecordDto(materialCode = "PRO20260803000003"))

        assertEquals(records, ProductStockInResponse(已入库 = records).已入库)
    }

    @Test
    fun `response parses backend 已入库 field`() {
        val response = Gson().fromJson(
            """{"在库":[{"materialCode":"PRO-IN-STOCK"}],"已出库":[{"materialCode":"PRO-OUT"}],"老化NG":[{"materialCode":"PRO-NG"}],"已入库":[{"proMatId":"274994954437320705","materialCode":"PRO20260803000003","partName":"Type-C接口"}],"批次未达标":[{"materialCode":"PRO-BATCH-NOT-QUALIFIED"}],"未老化":[{"materialCode":"PRO-NOT-AGING"}]}""",
            ProductStockInResponse::class.java,
        )

        assertEquals("PRO20260803000003", response.已入库.single().materialCode)
        assertEquals("274994954437320705", response.已入库.single().proMatId)
        assertEquals("PRO-OUT", response.已出库.single().materialCode)
        assertEquals("PRO-NG", response.老化NG.single().materialCode)
        assertEquals("PRO-IN-STOCK", response.在库.single().materialCode)
        assertEquals("PRO-BATCH-NOT-QUALIFIED", response.批次未达标.single().materialCode)
        assertEquals("PRO-NOT-AGING", response.未老化.single().materialCode)
    }
}
