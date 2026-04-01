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
**Location:** 
**Meeting Link:** 

---

### 📋 Agenda
1. 
2. 
3. 

### 💬 Discussion Points
- 

### ✅ Action Items
- [ ] 
- [ ] 
- [ ] 

### 📅 Next Meeting
**Date:** 
**Topic:** 

### 📝 Notes
""".trimIndent()),

        Template(id = -2, name = "Daily Journal", icon = "📔", isBuiltIn = true, content = """
## {{date}}

### 🌅 Morning Intentions
Today I want to...
- [ ] 

### 📝 Notes & Thoughts


### ✅ Tasks Completed
- [ ] 
- [ ] 
- [ ] 

### 🙏 Gratitude
I'm grateful for...

### 🌙 Evening Reflection
**Highlights of today:**
**What I learned:**
**Tomorrow I will focus on:**
""".trimIndent()),

        Template(id = -3, name = "Book Summary", icon = "📚", isBuiltIn = true, content = """
## Book Summary
**Title:** 
**Author:** 
**Genre:** 
**Rating:** ⭐⭐⭐⭐⭐
**Date Finished:** {{date}}

---

### 📖 One-Sentence Summary


### 🔑 Key Ideas
1. 
2. 
3. 
4. 

### 💬 Favourite Quotes
> 

> 

### 🧠 My Takeaways
- 
- 

### 📚 Related Books
- 
- 

### ✅ Action Items
- [ ] 
""".trimIndent()),

        Template(id = -4, name = "Project Plan", icon = "🚀", isBuiltIn = true, content = """
## Project: 
**Status:** 🟡 In Progress
**Start Date:** {{date}}
**Deadline:** 
**Owner:** 

---

### 🎯 Goal & Vision


### 📐 Scope & Requirements
- 
- 
- 

### 📅 Milestones
| # | Milestone | Due Date | Status |
|---|-----------|----------|--------|
| 1 |           |          | ⬜     |
| 2 |           |          | ⬜     |
| 3 |           |          | ⬜     |

### 📋 Tasks
- [ ] 
- [ ] 
- [ ] 

### ⚠️ Risks & Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
|      |        |            |

### 📝 Meeting Notes & Decisions


### 🔗 Resources & Links
- 
""".trimIndent()),

        Template(id = -5, name = "Quick Note", icon = "⚡", isBuiltIn = true, content = ""),

        Template(id = -6, name = "Weekly Review", icon = "📆", isBuiltIn = true, content = """
## Weekly Review — Week of {{date}}

### 📊 This Week's Wins
- 
- 
- 

### 📝 What Worked Well


### 🔧 What Could Be Improved


### 📅 Key Learnings


### 🎯 Goals for Next Week
1. 
2. 
3. 

### 📋 Pending Items
- [ ] 
- [ ] 

### 💡 Ideas & Brainstorms
- 
""".trimIndent()),

        Template(id = -7, name = "Habit Tracker", icon = "✅", isBuiltIn = true, content = """
## Habit Tracker — {{date}}

### Daily Habits
| Habit | Mon | Tue | Wed | Thu | Fri | Sat | Sun |
|-------|-----|-----|-----|-----|-----|-----|-----|
| 🏃 Exercise | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 📖 Read 30min | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 💧 Water 8cups | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 😴 Sleep 8hrs | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 🧘 Meditate | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

### Weekly Streaks
- Exercise: 0/7 days
- Reading: 0/7 days

### Notes


""".trimIndent()),

        Template(id = -8, name = "Reading List", icon = "📖", isBuiltIn = true, content = """
## Reading List

### 📚 Currently Reading
- **Title:** 
  *Author:* | *Started:* | *Progress:* ⬜⬜⬜⬜⬜⬜⬜⬜⬜⬜

### 📋 Up Next
1. 
2. 
3. 

### ✅ Completed This Year
| Title | Author | Rating | Date |
|-------|--------|--------|------|
|       |        | ⭐⭐⭐⭐⭐ |      |

### 💭 Reading Notes


""".trimIndent())
    )
}
