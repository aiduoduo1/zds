package com.jj.zds.feature.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoUploadModeTest {

    @Test
    fun defaultPhotoUploadModeIsCompressed() {
        assertEquals(PhotoUploadMode.COMPRESSED, PhotoUploadMode.default())
    }
}
