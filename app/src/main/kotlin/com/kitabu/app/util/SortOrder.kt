package com.kitabu.app.util

enum class SortOrder(val label: String) {
    UPDATED_DESC("Last edited"),
    CREATED_DESC("Date created"),
    TITLE_ASC("Title A–Z"),
    TITLE_DESC("Title Z–A"),
    WORD_COUNT("Word count")
}
