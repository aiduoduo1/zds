package com.jj.zds.feature.aging

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AgingModeContractTest {

    @Test
    fun `aging page has start and end modes that clear state when switched`() {
        val layout = projectFile("src/main/res/layout/activity_aging.xml").readText()
        val activity = projectFile("src/main/java/com/jj/zds/feature/aging/AgingActivity.kt").readText()

        assertTrue(layout.contains("android:id=\"@+id/btnModeStart\""))
        assertTrue(layout.contains("android:id=\"@+id/btnModeEnd\""))
        assertTrue(layout.contains("android:id=\"@+id/layoutStartMode\""))
        assertTrue(layout.contains("android:id=\"@+id/layoutEndMode\""))
        assertTrue(layout.contains("android:background=\"@drawable/aging_bg_mode_selected\""))
        assertTrue(layout.contains("app:jInputClearEnabled=\"true\""))
        assertTrue(!layout.contains("android:id=\"@+id/btnClearProducts\""))
        assertTrue(!layout.contains("android:text=\"本次待开始\""))
        assertTrue(activity.contains("viewModel.clearAll()"))
        assertTrue(activity.contains("binding.inputEndAgingScan.clearText().clearState()"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(File(relativePath), File("app", relativePath)).first { it.exists() }
    }
}
