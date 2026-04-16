package com.kitabu.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.List

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllFolders(): List<Folder>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: Int): Folder?

    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL AND isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getRootFolders(): List<Folder>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId AND isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getChildFolders(parentId: Int?): List<Folder>

    @Query("SELECT * FROM folders WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getFavoriteFolders(): List<Folder>

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId")
    suspend fun getNoteCountInFolder(folderId: Int?): Int

    @Query("""
        SELECT f.*, COUNT(n.id) as noteCount
        FROM folders f
        LEFT JOIN notes n ON f.id = n.folderId
        WHERE f.isArchived = 0
        GROUP BY f.id
        ORDER BY f.name COLLATE NOCASE ASC
    """)
    suspend fun getFoldersWithNoteCount(): List<FolderWithCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder): Int

    @Delete
    suspend fun deleteFolder(folder: Folder): Int

    @Query("DELETE FROM folders WHERE id = :folderId")
    fun deleteFolderById(folderId: Int)

    // For nested folder hierarchies
    @Query("""
        WITH RECURSIVE folder_tree AS (
            SELECT id, name, parentFolderId, 0 as level
            FROM folders
            WHERE id = :folderId
            UNION ALL
            SELECT f.id, f.name, f.parentFolderId, ft.level + 1
            FROM folders f
            INNER JOIN folder_tree ft ON f.parentFolderId = ft.id
        )
        SELECT * FROM folder_tree ORDER BY level
    """)
    suspend fun getFolderHierarchy(folderId: Int): List<Folder>

    @Query("SELECT * FROM folders WHERE isArchived = 1 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getArchivedFolders(): List<Folder>
}

data class FolderWithCount(
    val id: Int,
    val name: String,
    val parentFolderId: Int?,
    val color: Int,
    val icon: String,
    val isArchived: Boolean,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val noteCount: Int
)
