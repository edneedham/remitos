package com.remitos.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CameraFileHelperTest {
    @Test
    fun createImageFile_usesBaseDirAndJpgExtension() {
        val baseDir = File("build/tmp/testImages")
        val file = createImageFile(baseDir)

        assertEquals(baseDir, file.parentFile)
        assertTrue(file.name.startsWith("remito_"))
        assertTrue(file.name.endsWith(".jpg"))
    }
}
