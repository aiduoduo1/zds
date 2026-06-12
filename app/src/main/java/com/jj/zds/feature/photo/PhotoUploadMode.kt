package com.jj.zds.feature.photo

enum class PhotoUploadMode(val label: String) {
    COMPRESSED("压缩图"),
    ORIGINAL("原图");

    companion object {
        fun default(): PhotoUploadMode = COMPRESSED
    }
}
