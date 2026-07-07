package com.jj.zds.feature.pda

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdaHomeCardStyleContractTest {

    @Test
    fun `home cards use the same flat outlined treatment as debug plan cards`() {
        val home = projectFile("src/main/res/layout/fragment_pda_home.xml").readText()
        val actions = projectFile("src/main/res/layout/include_pda_home_actions.xml").readText()

        assertFalse(home.contains("android:elevation=\"4dp\""))
        assertFalse(home.contains("android:elevation=\"3dp\""))
        assertFalse(actions.contains("app:cardElevation=\"3dp\""))
        assertTrue(actions.contains("app:strokeColor=\"@color/j_color_line_light\""))
        assertTrue(actions.contains("android:id=\"@+id/btnAging\""))
        assertTrue(actions.contains("android:text=\"老化管理\""))

        val fragment = projectFile("src/main/java/com/jj/zds/feature/pda/PdaHomeFragment.kt").readText()
        assertTrue(fragment.contains("binding.homeActions.btnAging.setOnClickListener"))
        assertTrue(fragment.contains("AgingActivity::class.java"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(File(relativePath), File("app", relativePath)).first { it.exists() }
    }
}
