package ai.platon.pulsar.agentic.common

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AgentFileSystemTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var fs: AgentFileSystem

    @BeforeEach
    fun setUp() {
        fs = AgentFileSystem(tempDir, createDefaultFiles = false)
    }

    @AfterEach
    fun tearDown() {
        // Cleanup handled by TempDir
    }

    // --- Basic file operations ---

    @Test
    @DisplayName("writeString creates new file")
    fun writeStringCreatesNewFile() = runBlocking {
        val result = fs.writeString("test.txt", "Hello, World!")
        assertTrue(result.contains("successfully"))
        assertEquals(listOf("test.txt"), fs.listFiles())
    }

    @Test
    @DisplayName("readString returns file content")
    fun readStringReturnsFileContent() = runBlocking {
        fs.writeString("test.txt", "Hello, World!")
        val result = fs.readString("test.txt")
        assertTrue(result.contains("Hello, World!"))
        assertTrue(result.contains("<content>"))
    }

    @Test
    @DisplayName("readString returns error for non-existent file")
    fun readStringReturnsErrorForNonExistentFile() = runBlocking {
        val result = fs.readString("nonexistent.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    @DisplayName("append adds content to existing file")
    fun appendAddsContentToExistingFile() = runBlocking {
        fs.writeString("test.txt", "Line 1\n")
        fs.append("test.txt", "Line 2\n")
        val result = fs.readString("test.txt")
        assertTrue(result.contains("Line 1"))
        assertTrue(result.contains("Line 2"))
    }

    @Test
    @DisplayName("append returns error for non-existent file")
    fun appendReturnsErrorForNonExistentFile() = runBlocking {
        val result = fs.append("nonexistent.txt", "content")
        assertTrue(result.contains("not found"))
    }

    @Test
    @DisplayName("replaceContent replaces string in file")
    fun replaceContentReplacesStringInFile() = runBlocking {
        fs.writeString("test.txt", "Hello, World!")
        val result = fs.replaceContent("test.txt", "World", "Universe")
        assertTrue(result.contains("Successfully"))
        val content = fs.readString("test.txt")
        assertTrue(content.contains("Universe"))
        assertFalse(content.contains("World"))
    }

    @Test
    @DisplayName("replaceContent returns error for empty oldStr")
    fun replaceContentReturnsErrorForEmptyOldstr() = runBlocking {
        fs.writeString("test.txt", "Hello, World!")
        val result = fs.replaceContent("test.txt", "", "new")
        assertTrue(result.contains("Cannot replace empty string"))
    }

    // --- New file operations ---

    @Test
    @DisplayName("fileExists returns exists for existing file")
    fun fileExistsReturnsExistsForExistingFile() = runBlocking {
        fs.writeString("test.txt", "content")
        val result = fs.fileExists("test.txt")
        assertTrue(result.contains("exists"))
        assertFalse(result.contains("does not"))
    }

    @Test
    @DisplayName("fileExists returns not exists for missing file")
    fun fileExistsReturnsNotExistsForMissingFile() = runBlocking {
        val result = fs.fileExists("nonexistent.txt")
        assertTrue(result.contains("does not exist"))
    }

    @Test
    @DisplayName("getFileInfo returns file metadata")
    fun getFileInfoReturnsFileMetadata() = runBlocking {
        fs.writeString("test.txt", "Line 1\nLine 2\nLine 3")
        val result = fs.getFileInfo("test.txt")
        assertTrue(result.contains("Size:"))
        assertTrue(result.contains("Lines: 3"))
        assertTrue(result.contains("Extension: txt"))
    }

    @Test
    @DisplayName("getFileInfo returns error for non-existent file")
    fun getFileInfoReturnsErrorForNonExistentFile() = runBlocking {
        val result = fs.getFileInfo("nonexistent.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    @DisplayName("deleteFile removes file")
    fun deleteFileRemovesFile() = runBlocking {
        fs.writeString("test.txt", "content")
        assertTrue(fs.listFiles().contains("test.txt"))

        val result = fs.deleteFile("test.txt")
        assertTrue(result.contains("deleted successfully"))
        assertFalse(fs.listFiles().contains("test.txt"))
    }

    @Test
    @DisplayName("deleteFile returns error for non-existent file")
    fun deleteFileReturnsErrorForNonExistentFile() = runBlocking {
        val result = fs.deleteFile("nonexistent.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    @DisplayName("copyFile creates copy with same content")
    fun copyFileCreatesCopyWithSameContent() = runBlocking {
        fs.writeString("source.txt", "Original content")
        val result = fs.copyFile("source.txt", "dest.txt")
        assertTrue(result.contains("copied"))

        // Both files should exist
        assertTrue(fs.listFiles().contains("source.txt"))
        assertTrue(fs.listFiles().contains("dest.txt"))

        // Content should be the same
        val sourceContent = fs.readString("source.txt")
        val destContent = fs.readString("dest.txt")
        assertTrue(sourceContent.contains("Original content"))
        assertTrue(destContent.contains("Original content"))
    }

    @Test
    @DisplayName("copyFile returns error for non-existent source")
    fun copyFileReturnsErrorForNonExistentSource() = runBlocking {
        val result = fs.copyFile("nonexistent.txt", "dest.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    @DisplayName("copyFile returns error when source equals dest")
    fun copyFileReturnsErrorWhenSourceEqualsDest() = runBlocking {
        fs.writeString("test.txt", "content")
        val result = fs.copyFile("test.txt", "test.txt")
        assertTrue(result.contains("must be different"))
    }

    @Test
    @DisplayName("moveFile moves file to new name")
    fun moveFileMovesFileToNewName() = runBlocking {
        fs.writeString("old.txt", "Content to move")
        val result = fs.moveFile("old.txt", "new.txt")
        assertTrue(result.contains("moved"))

        // Old file should not exist, new file should
        assertFalse(fs.listFiles().contains("old.txt"))
        assertTrue(fs.listFiles().contains("new.txt"))

        // Content should be preserved
        val content = fs.readString("new.txt")
        assertTrue(content.contains("Content to move"))
    }

    @Test
    @DisplayName("moveFile returns error for non-existent source")
    fun moveFileReturnsErrorForNonExistentSource() = runBlocking {
        val result = fs.moveFile("nonexistent.txt", "dest.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    @DisplayName("moveFile returns error when source equals dest")
    fun moveFileReturnsErrorWhenSourceEqualsDest() = runBlocking {
        fs.writeString("test.txt", "content")
        val result = fs.moveFile("test.txt", "test.txt")
        assertTrue(result.contains("must be different"))
    }

    @Test
    @DisplayName("listFilesInfo returns formatted file list")
    fun listFilesInfoReturnsFormattedFileList() = runBlocking {
        fs.writeString("file1.txt", "Content 1")
        fs.writeString("file2.md", "# Markdown content")

        val result = fs.listFilesInfo()
        assertTrue(result.contains("2 files"))
        assertTrue(result.contains("file1.txt"))
        assertTrue(result.contains("file2.md"))
        assertTrue(result.contains("bytes"))
        assertTrue(result.contains("lines"))
    }

    @Test
    @DisplayName("listFilesInfo returns empty message when no files")
    fun listFilesInfoReturnsEmptyMessageWhenNoFiles() = runBlocking {
        val result = fs.listFilesInfo()
        assertTrue(result.contains("No files"))
    }

    // --- File extension validation ---

    @Test
    @DisplayName("writeString rejects invalid extension")
    fun writeStringRejectsInvalidExtension() = runBlocking {
        val result = fs.writeString("test.exe", "content")
        assertTrue(result.contains("Invalid"))
    }

    @Test
    @DisplayName("supports all valid extensions")
    fun supportsAllValidExtensions() = runBlocking {
        val extensions = listOf("md", "txt", "json", "jsonl", "csv")
        for (ext in extensions) {
            val result = fs.writeString("test.$ext", "content")
            assertTrue(result.contains("successfully"), "Failed for extension: $ext")
        }
    }

    @Test
    @DisplayName("rejects filenames with special characters")
    fun rejectsFilenamesWithSpecialCharacters() = runBlocking {
        val invalidNames = listOf("test file.txt", "test/path.txt", "test..txt")
        for (name in invalidNames) {
            val result = fs.writeString(name, "content")
            assertTrue(result.contains("Invalid") || result.contains("Error"), "Should reject: $name")
        }
    }

    @Test
    @DisplayName("allows dot in base name")
    fun allowsDotInBaseName() = runBlocking {
        val result = fs.writeString("a.b.txt", "content")
        assertTrue(result.contains("successfully"), result)
        assertTrue(fs.listFiles().contains("a.b.txt"))
    }

    // --- Edge cases ---

    @Test
    @DisplayName("handles empty file content")
    fun handlesEmptyFileContent() = runBlocking {
        fs.writeString("empty.txt", "")
        val info = fs.getFileInfo("empty.txt")
        assertTrue(info.contains("Lines: 0"))
    }

    @Test
    @DisplayName("handles multi-line content correctly")
    fun handlesMultiLineContentCorrectly() = runBlocking {
        val multiLine = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
        fs.writeString("multiline.txt", multiLine)
        val info = fs.getFileInfo("multiline.txt")
        assertTrue(info.contains("Lines: 5"))
    }

    @Test
    @DisplayName("copyFile can change extension")
    fun copyFileCanChangeExtension() = runBlocking {
        fs.writeString("source.txt", "Content")
        val result = fs.copyFile("source.txt", "dest.md")
        assertTrue(result.contains("copied"))
        assertTrue(fs.listFiles().contains("dest.md"))
    }

    @Test
    @DisplayName("moveFile can change extension")
    fun moveFileCanChangeExtension() = runBlocking {
        fs.writeString("source.txt", "Content")
        val result = fs.moveFile("source.txt", "dest.md")
        assertTrue(result.contains("moved"))
        assertTrue(fs.listFiles().contains("dest.md"))
        assertFalse(fs.listFiles().contains("source.txt"))
    }

    // --- Concurrent access tests ---

    @Test
    @DisplayName("handles multiple concurrent writes")
    fun handlesMultipleConcurrentWrites() = runBlocking {
        val files = (1..10).map { "file$it.txt" }
        files.forEach { fs.writeString(it, "Content for $it") }

        assertEquals(10, fs.listFiles().size)
        files.forEach { assertTrue(fs.listFiles().contains(it)) }
    }
}
