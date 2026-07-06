package com.jj.zds.feature.pda

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdaMainFragmentLifecycleContractTest {

    @Test
    fun `tab switching keeps registered fragment instances attached`() {
        val source = projectFile(
            "src/main/java/com/jj/zds/feature/pda/PdaMainActivity.kt",
        ).readText()

        assertFalse(source.contains(".replace(R.id.fragmentContainer"))
        assertTrue(source.contains("findFragmentByTag"))
        assertTrue(source.contains("transaction.hide(fragment)"))
        assertTrue(source.contains("transaction.show(targetFragment)"))
        assertTrue(source.contains("transaction.add(R.id.fragmentContainer"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(
            File(relativePath),
            File("app", relativePath),
        ).first { it.exists() }
    }
}
