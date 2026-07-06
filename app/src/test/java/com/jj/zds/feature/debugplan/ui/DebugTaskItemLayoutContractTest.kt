package com.jj.zds.feature.debugplan.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugTaskItemLayoutContractTest {

    @Test
    fun `item titles use extra small text`() {
        listOf("switch", "value", "text", "photo").forEach { type ->
            val layout = projectFile("src/main/res/layout/item_single_debug_check_$type.xml").readText()
            val title = layout.substringAfter("android:id=\"@+id/tvTitle\"").substringBefore("/>")

            assertTrue(title.contains("android:textSize=\"@dimen/j_text_size_xs\""))
        }
    }

    @Test
    fun `photo and confirm content uses primary blue`() {
        listOf("switch", "photo").forEach { type ->
            val layout = projectFile("src/main/res/layout/item_single_debug_check_$type.xml").readText()
            val content = layout.substringAfter("android:id=\"@+id/tvContent\"").substringBefore("/>")

            assertTrue(content.contains("android:textColor=\"@color/j_color_primary\""))
        }
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(File(relativePath), File("app", relativePath)).first { it.exists() }
    }
}
