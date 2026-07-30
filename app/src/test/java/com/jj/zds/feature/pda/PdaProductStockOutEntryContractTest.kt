package com.jj.zds.feature.pda

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PdaProductStockOutEntryContractTest {

    @Test
    fun `product stock out card opens the delivery task list activity`() {
        val fragment = projectFile("src/main/java/com/jj/zds/feature/pda/PdaHomeFragment.kt").readText()
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        assertTrue(fragment.contains("binding.homeActions.btnProductOut.setOnClickListener"))
        assertTrue(fragment.contains("ProductStockOutTaskListActivity::class.java"))
        assertTrue(manifest.contains(".feature.productstockout.ProductStockOutTaskListActivity"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(File(relativePath), File("app", relativePath)).first { it.exists() }
    }
}
