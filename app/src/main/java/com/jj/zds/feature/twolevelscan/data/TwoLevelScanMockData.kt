package com.jj.zds.feature.twolevelscan.data

import com.jj.zds.data.dto.twolevelscan.TwoLevelParentGroup

object TwoLevelScanMockData {

    fun createGroups(): List<TwoLevelParentGroup> {
        return listOf(
            TwoLevelParentGroup(
                id = "group-a",
                categoryCode = "A",
                title = "A 类物料",
                description = "伺服驱动 / 电源模块",
            ),
            TwoLevelParentGroup(
                id = "group-b",
                categoryCode = "B",
                title = "B 类物料",
                description = "控制板 / 通讯组件",
            ),
            TwoLevelParentGroup(
                id = "group-c",
                categoryCode = "C",
                title = "C 类物料",
                description = "屏幕模组 / 显示组件",
            ),
        )
    }
}
