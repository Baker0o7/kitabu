package com.kitabu.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kitabu.app.data.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

/**
 * Handles exporting and importing notes in various formats:
 * - JSON backup (all data including tags, versions, templates)
 * - Markdown zip (one .md file per note)
 */
object ExportImportHelper {

    // ── JSON Export (moved from NoteRepository for cleaner separation) ──

    suspend fun exportAllAsJson(context: Context): Uri {
        val db = KitabuDatabase.getDatabase(context)
        val notes = db.noteDao().getAllNotesRaw()
        val tags = db.tagDao().getAllTagsRaw()
        val noteTags = db.noteDao().getAllNoteTagsRaw()
        val versions = db.noteDao().getAllVersionsRaw()
        val templates = db.templateDao().getAllTemplatesRaw()

        val json = JSONObject()
        json.put("app", "Kitabu")
        json.put("version", 5)
        json.put("exportedAt", System.currentTimeMillis())

        json.put("notes", JSONArray().apply {
            notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id); put("title", n.title); put("content", n.content)
                    put("color", n.color); put("isPinned", n.isPinned); put("isLocked", n.isLocked)
                    put("isArchived", n.isArchived); put("isDaily", n.isDaily); put("dailyDate", n.dailyDate)
                    put("templateId", n.templateId); put("reminderTime", n.reminderTime)
                    put("isTrashed", n.isTrashed); put("trashedAt", n.trashedAt)
                    put("isFavorite", n.isFavorite)
                    put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
                })
            }
        })

        json.put("tags", JSONArray().apply {
            tags.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("name", t.name); put("color", t.color); put("createdAt", t.createdAt)
                })
            }
        })

        json.put("noteTags", JSONArray().apply {
            noteTags.forEach { nt ->
                put(JSONObject().apply { put("noteId", nt.noteId); put("tagId", nt.tagId) })
            }
        })

        json.put("versions", JSONArray().apply {
            versions.forEach { v ->
                put(JSONObject().apply {
                    put("id", v.id); put("noteId", v.noteId); put("title", v.title)
                    put("content", v.content); put("savedAt", v.savedAt)
                })
            }
        })

        json.put("templates", JSONArray().apply {
            templates.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("name", t.name); put("content", t.content)
                    put("icon", t.icon); put("isBuiltIn", t.isBuiltIn); put("createdAt", t.createdAt)
                })
            }
        })

        val fileName = "kitabu_backup_${System.currentTimeMillis()}.json"
        return saveToDownloads(context, fileName, "application/json", json.toString(2).toByteArray())
    }

    // ── Markdown Zip Export ──

    suspend fun exportAsMarkdownZip(context: Context): Uri {
        val db = KitabuDatabase.getDatabase(context)
        val notes = db.noteDao().getAllNotesRaw().filter { !it.isTrashed }

        val fileName = "kitabu_notes_${System.currentTimeMillis()}.zip"
        val byteArray = ByteArrayOutputStream()
        ZipOutputStream(byteArray).use { zos ->
            notes.forEach { note ->
                val safeName = note.title.ifBlank { "Untitled-${note.id}" }
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val entry = ZipEntry("$safeName.md")
                zos.putNextEntry(entry)
                val content = if (note.title.isNotBlank()) "# ${note.title}\n\n${note.content}" else note.content
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        return saveToDownloads(context, fileName, "application/zip", byteArray.toByteArray())
    }

    /**
     * Save a byte array to the Downloads folder using MediaStore (Android 10+)
     * or direct file access (Android 9 and below).
     */
    private fun saveToDownloads(context: Context, fileName: String, mimeType: String, data: ByteArray): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for scoped storage
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to create MediaStore entry")
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                ?: throw IOException("Failed to open output stream")
            // Mark as complete
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            uri
        } else {
            // Legacy approach for Android 9 and below
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { it.write(data) }
            Uri.fromFile(file)
        }
    }

    // ── JSON Import ──

    suspend fun importFromJson(context: Context, uri: Uri): ImportResult {
        val db = KitabuDatabase.getDatabase(context)
        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: return ImportResult(0, 0, 0, error = "Could not read file")

        return try {
            val json = JSONObject(content)
            val app = json.optString("app", "")
            if (app != "Kitabu") return ImportResult(0, 0, 0, error = "Not a valid Kitabu backup file")

            var notesImported = 0
            var tagsImported = 0
            var versionsImported = 0

            // Import tags first (needed for note_tags foreign keys)
            val tagIdMap = mutableMapOf<Int, Int>() // old id -> new id
            val tagsArray = json.optJSONArray("tags")
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    val tagJson = tagsArray.getJSONObject(i)
                    val tagName = tagJson.getString("name").lowercase().trim()
                    // Check if tag already exists to avoid duplicates
                    val existing = db.tagDao().findTagByName(tagName)
                    val newId = if (existing != null) {
                        existing.id
                    } else {
                        val tag = Tag(
                            name = tagName,
                            color = tagJson.getInt("color"),
                            createdAt = tagJson.getLong("createdAt")
                        )
                        db.tagDao().insertTag(tag).toInt()
                    }
                    tagIdMap[tagJson.getInt("id")] = newId
                    tagsImported++
                }
            }

            // Import notes
            val noteIdMap = mutableMapOf<Int, Int>()
            val notesArray = json.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val n = notesArray.getJSONObject(i)
                    val note = Note(
                        title = n.getString("title"),
                        content = n.getString("content"),
                        color = n.optInt("color", NoteColor.DEFAULT),
                        isPinned = n.optBoolean("isPinned", false),
                        isLocked = n.optBoolean("isLocked", false),
                        isArchived = n.optBoolean("isArchived", false),
                        isDaily = n.optBoolean("isDaily", false),
                        dailyDate = n.optString("dailyDate", null).ifBlank { null },
                        templateId = null,
                        reminderTime = if (n.has("reminderTime") && !n.isNull("reminderTime")) n.getLong("reminderTime") else null,
                        isTrashed = n.optBoolean("isTrashed", false),
                        trashedAt = if (n.has("trashedAt") && !n.isNull("trashedAt")) n.getLong("trashedAt") else null,
                        isFavorite = n.optBoolean("isFavorite", false),
                        createdAt = n.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = n.optLong("updatedAt", System.currentTimeMillis())
                    )
                    val newId = db.noteDao().insertNote(note)
                    noteIdMap[n.getInt("id")] = newId.toInt()
                    notesImported++
                }
            }

            // Import note_tags with remapped IDs
            val noteTagsArray = json.optJSONArray("noteTags")
            if (noteTagsArray != null) {
                for (i in 0 until noteTagsArray.length()) {
                    val nt = noteTagsArray.getJSONObject(i)
                    val newNoteId = noteIdMap[nt.getInt("noteId")] ?: continue
                    val newTagId = tagIdMap[nt.getInt("tagId")] ?: continue
                    db.noteDao().insertNoteTag(NoteTag(newNoteId, newTagId))
                }
            }

            // Import versions with remapped note IDs
            val versionsArray = json.optJSONArray("versions")
            if (versionsArray != null) {
                for (i in 0 until versionsArray.length()) {
                    val v = versionsArray.getJSONObject(i)
                    val newNoteId = noteIdMap[v.getInt("noteId")] ?: continue
                    val version = NoteVersion(
                        noteId = newNoteId,
                        title = v.getString("title"),
                        content = v.getString("content"),
                        savedAt = v.getLong("savedAt")
                    )
                    db.noteVersionDao().insertVersion(version)
                    versionsImported++
                }
            }

            // Import templates
            val templatesArray = json.optJSONArray("templates")
            if (templatesArray != null) {
                for (i in 0 until templatesArray.length()) {
                    val t = templatesArray.getJSONObject(i)
                    if (t.optBoolean("isBuiltIn", false)) continue // Skip built-in templates
                    val template = Template(
                        name = t.getString("name"),
                        content = t.getString("content"),
                        icon = t.optString("icon", "📄"),
                        isBuiltIn = false,
                        createdAt = t.getLong("createdAt")
                    )
                    db.templateDao().insertTemplate(template)
                }
            }

            ImportResult(notesImported, tagsImported, versionsImported)
        } catch (e: Exception) {
            ImportResult(0, 0, 0, error = "Import failed: ${e.message}")
        }
    }

    // ── Markdown Zip Import ──

    suspend fun importFromMarkdownZip(context: Context, uri: Uri): ImportResult {
        val db = KitabuDatabase.getDatabase(context)
        var notesImported = 0

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val name = entry!!.name
                        if (!name.endsWith(".md")) continue

                        val content = zis.readBytes().toString(Charsets.UTF_8)
                        // Extract title from first H1 or filename
                        val title = if (content.startsWith("# ")) {
                            content.lines().first().removePrefix("# ").trim()
                        } else {
                            name.removeSuffix(".md").replace("_", " ").replace("-", " ")
                        }
                        val body = if (content.startsWith("# ")) {
                            content.lines().drop(1).joinToString("\n").trim()
                        } else {
                            content.trim()
                        }

                        val note = Note(title = title, content = body)
                        db.noteDao().insertNote(note)
                        notesImported++
                    }
                }
            }
            ImportResult(notesImported, 0, 0)
        } catch (e: Exception) {
            ImportResult(0, 0, 0, error = "Import failed: ${e.message}")
        }
    }

    data class ImportResult(
        val notesImported: Int,
        val tagsImported: Int,
        val versionsImported: Int,
        val error: String? = null
    ) {
        val success get() = error == null
        val summary get() = if (success) "Imported $notesImported notes, $tagsImported tags, $versionsImported versions"
                             else error ?: "Unknown error"
    }
}
