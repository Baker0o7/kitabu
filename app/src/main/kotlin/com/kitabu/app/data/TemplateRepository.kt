package com.kitabu.app.data

import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val dao: TemplateDao) {
    val allTemplates: Flow<List<Template>> = dao.getAllTemplates()
    suspend fun getById(id: Int): Template? = dao.getTemplateById(id)
    suspend fun insert(template: Template): Long = dao.insertTemplate(template)
    suspend fun update(template: Template) = dao.updateTemplate(template)
    suspend fun delete(template: Template) = dao.deleteTemplate(template)
}
