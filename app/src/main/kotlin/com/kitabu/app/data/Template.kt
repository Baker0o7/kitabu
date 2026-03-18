package com.kitabu.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val content: String,
    val icon: String = "📄",
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object BuiltInTemplates {
    val all = listOf(
        Template(id = -1, name = "Meeting Notes", icon = "📋", isBuiltIn = true, content = """
## Meeting Notes
**Date:** {{date}}
**Attendees:** 

---

### Agenda
- 

### Discussion

### Action Items
- [ ] 
- [ ] 

### Next Meeting

""".trimIndent()),

        Template(id = -2, name = "Daily Journal", icon = "📔", isBuiltIn = true, content = """
## {{date}}

### 🌅 Morning Intentions
Today I want to...

### 📝 Notes & Thoughts

### ✅ Tasks
- [ ] 
- [ ] 
- [ ] 

### 🌙 Evening Reflection
Today I'm grateful for...
""".trimIndent()),

        Template(id = -3, name = "Book Summary", icon = "📚", isBuiltIn = true, content = """
## Book Summary
**Title:** 
**Author:** 
**Rating:** ⭐⭐⭐⭐⭐

---

### Key Ideas

### Favourite Quotes

> 

### My Takeaways

### Action Items
- [ ] 
""".trimIndent()),

        Template(id = -4, name = "Project Plan", icon = "🚀", isBuiltIn = true, content = """
## Project: 
**Status:** 🟡 In Progress
**Deadline:** 

---

### Goal

### Scope

### Milestones
| Milestone | Due | Status |
|-----------|-----|--------|
|           |     | ⬜     |
|           |     | ⬜     |

### Tasks
- [ ] 
- [ ] 

### Notes

""".trimIndent()),

        Template(id = -5, name = "Quick Note", icon = "⚡", isBuiltIn = true, content = "")
    )
}
