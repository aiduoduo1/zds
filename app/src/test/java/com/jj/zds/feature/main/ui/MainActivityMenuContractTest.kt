package com.jj.zds.feature.main.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityMenuContractTest {

    @Test
    fun `toolbar exposes aging management menu and opens aging page`() {
        val source = projectFile("src/main/java/com/jj/zds/feature/main/ui/MainActivity.kt").readText()

        assertTrue(source.contains("JiangToolbarMenuItem(MENU_AGING_MANAGEMENT, \"老化管理\")"))
        assertTrue(source.contains("MENU_AGING_MANAGEMENT -> startActivity(Intent(this, AgingActivity::class.java))"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(File(relativePath), File("app", relativePath)).first { it.exists() }
    }
}
