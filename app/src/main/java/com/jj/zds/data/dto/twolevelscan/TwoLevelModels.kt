package com.jj.zds.data.dto.twolevelscan

data class TwoLevelParentGroup(
    val id: String,
    val categoryCode: String,
    val title: String,
    val description: String,
    val expanded: Boolean = true,
    val children: List<TwoLevelChildItem> = emptyList(),
)

data class TwoLevelChildItem(
    val id: String,
    val code: String,
    val name: String,
    val remark: String,
    val refreshCount: Int = 0,
)

sealed interface TwoLevelRow {
    val rowId: String
}

data class TwoLevelParentRow(
    val group: TwoLevelParentGroup,
) : TwoLevelRow {
    override val rowId: String = "parent:${group.id}"
}

data class TwoLevelChildRow(
    val parentId: String,
    val child: TwoLevelChildItem,
) : TwoLevelRow {
    override val rowId: String = "child:$parentId:${child.id}"
}
