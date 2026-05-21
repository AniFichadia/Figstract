package com.anifichadia.figstract.ios.importer.asset.model.importing

object HeicSupport {
    val isAvailable: Boolean by lazy {
        try {
            val result = ProcessBuilder("magick", "--version")
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .readText()
            result.contains("ImageMagick", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun requireAvailable() {
        check(isAvailable) {
            "HEIC output requires ImageMagick. Refer to readme for installation instructions."
        }
    }
}
