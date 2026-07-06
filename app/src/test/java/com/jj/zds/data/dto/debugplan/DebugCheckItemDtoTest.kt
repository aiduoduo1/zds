package com.jj.zds.data.dto.debugplan

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugCheckItemDtoTest {

    @Test
    fun `keeps decimal range values returned by scan valid`() {
        val item = DebugCheckItemDto(
            minValue = BigDecimal("10.5"),
            maxValue = BigDecimal("11.99"),
        )

        assertEquals(BigDecimal("10.5"), item.minValue)
        assertEquals(BigDecimal("11.99"), item.maxValue)
    }
}
