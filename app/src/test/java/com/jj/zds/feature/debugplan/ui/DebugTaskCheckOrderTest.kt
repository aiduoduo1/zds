package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugCheckFirstLevelDto
import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugCheckItemDto
import com.jj.zds.data.dto.debugplan.DebugCheckSecondLevelDto
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTaskCheckOrderTest {

    @Test
    fun `flatten keeps api array order instead of sort values`() {
        val group = DebugCheckGroupDto(
            checkList = listOf(
                firstLevel("first-api", sort = 20, itemId = "item-first", itemSort = 20),
                firstLevel("second-api", sort = 10, itemId = "item-second", itemSort = 10),
            ),
        )

        val paths = DebugTaskCheckOrder.flatten(group)

        assertEquals(listOf("item-first", "item-second"), paths.map { it.item.id })
        assertEquals(listOf("1.1.1", "2.1.1"), paths.map { it.typeLabel })
    }

    private fun firstLevel(
        name: String,
        sort: Int,
        itemId: String,
        itemSort: Int,
    ): DebugCheckFirstLevelDto {
        return DebugCheckFirstLevelDto(
            name = name,
            sort = sort,
            children = listOf(
                DebugCheckSecondLevelDto(
                    id = "$itemId-second",
                    name = "$name-child",
                    sort = sort,
                    children = listOf(
                        DebugCheckItemDto(
                            id = itemId,
                            sort = itemSort,
                        ),
                    ),
                ),
            ),
        )
    }
}
