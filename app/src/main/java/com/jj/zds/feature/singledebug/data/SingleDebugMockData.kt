package com.jj.zds.feature.singledebug.data

import com.jj.zds.data.dto.singledebug.DebugCheckItem
import com.jj.zds.data.dto.singledebug.PhotoCheckItem
import com.jj.zds.data.dto.singledebug.ScopeCheckItem
import com.jj.zds.data.dto.singledebug.SwitchCheckItem
import com.jj.zds.data.dto.singledebug.TextCheckItem
import com.jj.zds.data.dto.singledebug.ValueCheckItem

object SingleDebugMockData {

    fun createChecks(): List<DebugCheckItem> {
        return listOf(
            SwitchCheckItem("power", "上电状态、指示灯、急停回路正常"),
            ValueCheckItem("current", "空载电流", referenceValue = "1.20", unit = "A"),
            PhotoCheckItem("nameplate", "工装接线和铭牌照片"),
            TextCheckItem("remark", "填写现场调试说明"),
            ScopeCheckItem("scope", "确认示波器协议波形"),
            SwitchCheckItem("label", "标签核对，SN 与物料型号一致"),
            ValueCheckItem("voltage", "工作电压", referenceValue = "24.00", unit = "V"),
            PhotoCheckItem("appearance", "外观和接线复核照片"),
        )
    }
}
