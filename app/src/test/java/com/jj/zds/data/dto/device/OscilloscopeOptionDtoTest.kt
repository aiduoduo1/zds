package com.jj.zds.data.dto.device

import org.junit.Assert.assertEquals
import org.junit.Test

class OscilloscopeOptionDtoTest {
    @Test
    fun displayLabelUsesFriendlyNamesAndAddress() {
        val option = OscilloscopeOptionDto(
            id = 1,
            deviceName = "调试台1示波器",
            host = "192.168.3.114",
            computerName = "一号工控机",
        )

        assertEquals("调试台1示波器\n一号工控机  ·  192.168.3.114", option.displayLabel())
    }
}
