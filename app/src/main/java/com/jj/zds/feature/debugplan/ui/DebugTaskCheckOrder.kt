package com.jj.zds.feature.debugplan.ui

import com.jj.zds.data.dto.debugplan.DebugCheckGroupDto
import com.jj.zds.data.dto.debugplan.DebugCheckItemDto

internal data class DebugTaskCheckPath(
    val item: DebugCheckItemDto,
    val typeLabel: String,
    val parentKey: String,
)

internal object DebugTaskCheckOrder {

    fun flatten(group: DebugCheckGroupDto): List<DebugTaskCheckPath> {
        return group.checkList.flatMapIndexed { firstIndex, first ->
            first.children.flatMapIndexed { secondIndex, second ->
                second.children.mapIndexed { itemIndex, item ->
                    DebugTaskCheckPath(
                        item = item,
                        typeLabel = "${firstIndex + 1}.${secondIndex + 1}.${itemIndex + 1}",
                        parentKey = "${first.name.orEmpty()}-${second.id.orEmpty()}-$itemIndex",
                    )
                }
            }
        }
    }
}
