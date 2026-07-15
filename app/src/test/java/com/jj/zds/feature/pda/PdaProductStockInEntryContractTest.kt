package com.jj.zds.feature.pda

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PdaProductStockInEntryContractTest {

    @Test
    fun `product stock in card opens the stock in activity`() {
        val fragment = projectFile("src/main/java/com/jj/zds/feature/pda/PdaHomeFragment.kt").readText()
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        assertTrue(fragment.contains("binding.homeActions.btnProductIn.setOnClickListener"))
        assertTrue(fragment.contains("ProductStockInActivity::class.java"))
        assertTrue(manifest.contains(".feature.productstockin.ProductStockInActivity"))
    }

    private fun projectFile(relativePath: String): File {
        return sequenceOf(File(relativePath), File("app", relativePath)).first { it.exists() }
    }
}
