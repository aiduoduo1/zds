package com.jj.zds.feature.login.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginLayoutContractTest {

    @Test
    fun `login stays a single screen with only the required controls`() {
        val layout = projectFile("src/main/res/layout/activity_login.xml").readText()
        val strings = projectFile("src/main/res/values/strings.xml").readText()

        assertFalse(layout.contains("ScrollView"))
        assertFalse(layout.contains("loginContentGuide"))
        assertTrue(layout.contains("android:id=\"@+id/layoutBrand\"\n        android:layout_width=\"match_parent\""))
        assertTrue(layout.contains("android:id=\"@+id/layoutLoginCard\"\n        android:layout_width=\"match_parent\""))
        assertTrue(layout.contains("@+id/inputAccount"))
        assertTrue(layout.contains("@+id/inputPassword"))
        assertTrue(layout.contains("@+id/btnLogin"))
        assertTrue(strings.contains("<string name=\"login_title\">DZS</string>"))
        assertTrue(strings.contains("<string name=\"login_subtitle\">智能制造·高效协同</string>"))
        assertFalse(layout.contains("记住账号"))
        assertFalse(layout.contains("设置"))
        assertFalse(layout.contains("版本"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(
            File(relativePath),
            File("app", relativePath),
        ).first { it.exists() }
    }
}
