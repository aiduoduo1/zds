package com.jj.zds.data.dto.productionplan

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionSwitchOperationDtosTest {

    @Test
    fun softCheckRequestCarriesSwitchId() {
        val request = ProductionSoftCheckRequest(lineSwiId = "258046612688011264")

        assertEquals("258046612688011264", request.lineSwiId)
    }

    @Test
    fun planListRequestCarriesLineId() {
        val request = ProductionPlanListRequest(
            keyword = "WO20260622001",
            lineId = "LINE-01",
        )

        assertEquals("WO20260622001", request.keyword)
        assertEquals("LINE-01", request.lineId)
    }

    @Test
    fun materialCheckRequestCarriesSwitchId() {
        val request = ProductionMaterialCheckRequest(lineSwiId = "SWI-01")

        assertEquals("SWI-01", request.lineSwiId)
    }

    @Test
    fun materialListResolvesFirstAvailableSwitchId() {
        val materials = listOf(
            ProductionSwitchCategoryDto(lineSwiId = ""),
            ProductionSwitchCategoryDto(lineSwiId = "SWI-01"),
        )

        assertEquals("SWI-01", materials.firstLineSwiId())
    }

    @Test
    fun personCheckRequestCarriesCheckedUsers() {
        val request = ProductionPersonCheckRequest(
            lineSwiId = "SWI-01",
            tecDtlId = "TEC-01",
            staId = null,
            skiId = "SKI-01",
            users = listOf(
                ProductionPersonCheckedRequest(
                    id = "CHECK-01",
                    userId = "USER-01",
                    activeAt = "2026-06-22 10:00:00",
                ),
            ),
        )

        assertEquals("SWI-01", request.lineSwiId)
        assertEquals("USER-01", request.users.first().userId)
    }

    @Test
    fun personCheckDtoBuildsRequestWithAllCheckedUsers() {
        val item = ProductionSwitchUserDto(
            lineSwiId = "SWI-01",
            tecDtlId = "TEC-01",
            staId = "STA-01",
            skiId = "SKI-01",
            users = listOf(
                ProductionSwitchCheckedUserDto(
                    id = "CHECK-01",
                    userId = "USER-01",
                    activeAt = "2026-06-22 10:00:00",
                ),
                ProductionSwitchCheckedUserDto(
                    id = "CHECK-02",
                    userId = "USER-02",
                    activeAt = "2026-06-22 10:05:00",
                ),
            ),
        )

        val request = item.toPersonCheckRequest()

        assertEquals("SWI-01", request.lineSwiId)
        assertEquals("TEC-01", request.tecDtlId)
        assertEquals("STA-01", request.staId)
        assertEquals("SKI-01", request.skiId)
        assertEquals(2, request.users.size)
        assertEquals("USER-02", request.users[1].userId)
    }

    @Test
    fun equipmentCheckRequestCarriesCheckedEquipment() {
        val request = ProductionEquipmentCheckRequest(
            lineSwiId = "SWI-01",
            tecDtlId = "TEC-01",
            staId = null,
            equId = "EQU-01",
            equCatId = "EQU-CAT-01",
            equips = listOf(
                ProductionEquipmentCheckedRequest(
                    id = "CHECK-01",
                    equId = "EQU-02",
                    activeAt = "2026-06-22 10:00:00",
                ),
            ),
        )

        assertEquals("EQU-CAT-01", request.equCatId)
        assertEquals("EQU-02", request.equips.first().equId)
    }

    @Test
    fun equipmentCheckDtoBuildsRequestWithAllCheckedEquipment() {
        val item = ProductionSwitchEquipmentDto(
            lineSwiId = "SWI-01",
            tecDtlId = "TEC-01",
            staId = "STA-01",
            equId = "EQU-01",
            equCatId = "EQU-CAT-01",
            equips = listOf(
                ProductionSwitchCheckedEquipmentDto(
                    id = "CHECK-01",
                    equId = "EQU-02",
                    activeAt = "2026-06-22 10:00:00",
                ),
                ProductionSwitchCheckedEquipmentDto(
                    id = "CHECK-02",
                    equId = "EQU-03",
                    activeAt = "2026-06-22 10:05:00",
                ),
            ),
        )

        val request = item.toEquipmentCheckRequest()

        assertEquals("SWI-01", request.lineSwiId)
        assertEquals("TEC-01", request.tecDtlId)
        assertEquals("STA-01", request.staId)
        assertEquals("EQU-01", request.equId)
        assertEquals("EQU-CAT-01", request.equCatId)
        assertEquals(2, request.equips.size)
        assertEquals("EQU-03", request.equips[1].equId)
    }

    @Test
    fun endSwitchRequestCarriesPlanId() {
        val request = ProductionEndSwitchRequest(planId = "PLAN-01")

        assertEquals("PLAN-01", request.planId)
    }
}
