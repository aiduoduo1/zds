package com.jj.zds.data.dto.productionplan

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionSwitchCheckProgressTest {

    @Test
    fun equipmentProgressUsesCheckedEquipListAndRequiredCount() {
        val items = listOf(
            ProductionSwitchEquipmentDto(
                equipNum = 2,
                status = 0,
                equips = listOf(ProductionSwitchCheckedEquipmentDto(equCode = "EQ-1")),
            ),
            ProductionSwitchEquipmentDto(
                equipNum = 1,
                status = 1,
                equips = emptyList(),
            ),
        )

        val progress = items.toEquipmentCheckProgress()

        assertEquals(1, progress.done)
        assertEquals(3, progress.total)
        assertEquals(ProductionSwitchCheckState.DOING, progress.state)
    }

    @Test
    fun personProgressUsesCheckedUserListInsteadOfStatus() {
        val items = listOf(
            ProductionSwitchUserDto(
                status = 0,
                users = listOf(ProductionSwitchCheckedUserDto(account = "u001")),
            ),
            ProductionSwitchUserDto(
                status = 1,
                users = emptyList(),
            ),
        )

        val progress = items.toPersonCheckProgress()

        assertEquals(1, progress.done)
        assertEquals(2, progress.total)
        assertEquals(ProductionSwitchCheckState.DOING, progress.state)
    }

    @Test
    fun materialAndSoftProgressStillUseStatus() {
        val materials = listOf(
            ProductionSwitchCategoryDto(status = 1),
            ProductionSwitchCategoryDto(status = 0),
        )
        val softs = listOf(
            ProductionSwitchSoftDto(status = 1),
        )

        assertEquals(1, materials.toStatusCheckProgress().done)
        assertEquals(2, materials.toStatusCheckProgress().total)
        assertEquals(ProductionSwitchCheckState.DOING, materials.toStatusCheckProgress().state)
        assertEquals(ProductionSwitchCheckState.DONE, softs.toStatusCheckProgress().state)
    }

    @Test
    fun materialSubmitHiddenOnlyWhenNonEmptyListIsComplete() {
        assertEquals(
            true,
            listOf(
                ProductionSwitchCategoryDto(status = 1),
                ProductionSwitchCategoryDto(status = 1),
            ).isMaterialCheckComplete(),
        )
        assertEquals(
            false,
            listOf(
                ProductionSwitchCategoryDto(status = 1),
                ProductionSwitchCategoryDto(status = 0),
            ).isMaterialCheckComplete(),
        )
        assertEquals(false, emptyList<ProductionSwitchCategoryDto>().isMaterialCheckComplete())
    }

    @Test
    fun softSubmitHiddenOnlyWhenNonEmptyListIsComplete() {
        assertEquals(
            true,
            listOf(
                ProductionSwitchSoftDto(status = 1),
                ProductionSwitchSoftDto(status = 1),
            ).isSoftCheckComplete(),
        )
        assertEquals(
            false,
            listOf(
                ProductionSwitchSoftDto(status = 1),
                ProductionSwitchSoftDto(status = 0),
            ).isSoftCheckComplete(),
        )
        assertEquals(false, emptyList<ProductionSwitchSoftDto>().isSoftCheckComplete())
    }

    @Test
    fun switchCanEndOnlyWhenEveryExistingCheckItemIsDone() {
        val doneDetail = ProductionSwitchDetailDto(
            planId = "PLAN-01",
            equList = listOf(
                ProductionSwitchEquipmentDto(
                    equipNum = 1,
                    equips = listOf(ProductionSwitchCheckedEquipmentDto(equId = "EQU-01")),
                ),
            ),
            userList = listOf(
                ProductionSwitchUserDto(
                    users = listOf(ProductionSwitchCheckedUserDto(userId = "USER-01")),
                ),
            ),
            catList = listOf(ProductionSwitchCategoryDto(status = 1)),
            softList = listOf(ProductionSwitchSoftDto(status = 1)),
        )
        val doingDetail = doneDetail.copy(
            catList = listOf(ProductionSwitchCategoryDto(status = 0)),
        )
        val emptyDetail = ProductionSwitchDetailDto(planId = "PLAN-01")

        assertEquals(true, doneDetail.isAllCheckItemsDone())
        assertEquals(false, doingDetail.isAllCheckItemsDone())
        assertEquals(false, emptyDetail.isAllCheckItemsDone())
    }
}
