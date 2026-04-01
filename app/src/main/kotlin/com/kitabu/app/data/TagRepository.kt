package com.kitabu.app.data

import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao) {
    val allTags: Flow<List<Tag>> = dao.getAllTags()
    suspend fun insert(tag: Tag): Long = dao.insertTag(tag)
    suspend fun update(tag: Tag) = dao.updateTag(tag)
    suspend fun delete(tag: Tag) = dao.deleteTag(tag)
    suspend fun deleteTagById(id: Int) = dao.deleteTagById(id)
    suspend fun find(name: String): Tag? = dao.findTagByName(name)
    suspend fun getNoteCount(tagId: Int): Int = dao.getNoteCountForTag(tagId)
}
