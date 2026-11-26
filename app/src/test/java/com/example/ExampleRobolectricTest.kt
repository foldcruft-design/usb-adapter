package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FileManagerRepository
import com.example.model.FileCategory
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.util.Formatters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches OTG Drive`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OTG Drive", appName)
    }

    @Test
    fun `local workspace file operations create list rename copy delete`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FileManagerRepository(context)
        val root = repo.getLocalWorkspaceRoot()
        root.listFiles()?.forEach { it.deleteRecursively() }

        repo.createDirectory(null, null, root, "USB_Backup").getOrThrow()
        repo.createTextFile(null, null, root, "readme.txt", "Привет OTG!").getOrThrow()

        val initial = repo.listLocalDirectory(root, false, SortField.NAME, SortOrder.ASCENDING)
        assertEquals(2, initial.size)
        assertTrue(initial[0].isDirectory)
        assertEquals("USB_Backup", initial[0].name)
        assertEquals("readme.txt", initial[1].name)

        val textFile = initial[1]
        assertEquals("Привет OTG!", repo.readTextContent(textFile).getOrThrow())

        val subDir = File(root, "USB_Backup")
        repo.copyOrMoveNodes(
            sources = listOf(textFile),
            sourceTreeUri = null,
            targetTreeUri = null,
            targetParentDocId = null,
            targetLocalDir = subDir,
            isMove = false,
            onProgress = {}
        ).getOrThrow()

        val subList = repo.listLocalDirectory(subDir, false, SortField.NAME, SortOrder.ASCENDING)
        assertEquals(1, subList.size)
        assertEquals("readme.txt", subList[0].name)

        repo.renameNode(null, subList[0], "notes.md").getOrThrow()
        val renamedList = repo.listLocalDirectory(subDir, false, SortField.NAME, SortOrder.ASCENDING)
        assertEquals("notes.md", renamedList[0].name)

        repo.deleteNodes(initial).getOrThrow()
        val afterDelete = repo.listLocalDirectory(root, false, SortField.NAME, SortOrder.ASCENDING)
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun `formatters classify files and format usb ids`() {
        assertEquals("VID:0781  PID:5581", Formatters.formatUsbIds(0x0781, 0x5581))
        assertEquals(FileCategory.IMAGE, Formatters.classifyFile("photo.jpg", "image/jpeg", false))
        assertEquals(FileCategory.CODE_TEXT, Formatters.classifyFile("config.json", "application/json", false))
        assertEquals(FileCategory.FOLDER, Formatters.classifyFile("DCIM", "", true))
    }
}
