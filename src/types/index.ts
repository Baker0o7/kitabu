export interface Note {
  id: string;
  title: string;
  content: string;
  color: NoteColor;
  folderId: string | null;
  isPinned: boolean;
  isLocked: boolean;
  isArchived: boolean;
  isDaily: boolean;
  dailyDate: string | null;
  templateId: string | null;
  reminderTime: number | null;
  isTrashed: boolean;
  trashedAt: number | null;
  isFavorite: boolean;
  tagIds: string[];
  createdAt: number;
  updatedAt: number;
}

export type NoteColor = 
  | '#1E1E2E' // DEFAULT
  | '#2D1B2E' // ROSE
  | '#0D2137' // OCEAN
  | '#0D2818' // FOREST
  | '#2D1F00' // AMBER
  | '#1A1A2E' // LAVENDER
  | '#0D2225' // TEAL
  | '#1A1A1A'; // CHARCOAL

export const NoteColors: NoteColor[] = [
  '#1E1E2E', '#2D1B2E', '#0D2137', '#0D2818',
  '#2D1F00', '#1A1A2E', '#0D2225', '#1A1A1A'
];

export interface Folder {
  id: string;
  name: string;
  parentFolderId: string | null;
  color: string;
  icon: string;
  isArchived: boolean;
  isFavorite: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Tag {
  id: string;
  name: string;
  color: string;
  createdAt: number;
}

export interface Template {
  id: string;
  name: string;
  content: string;
  icon: string;
  isBuiltIn: boolean;
  createdAt: number;
}

export interface NoteVersion {
  id: string;
  noteId: string;
  title: string;
  content: string;
  createdAt: number;
}

export type ViewMode = 'grid' | 'compact' | 'list';
export type SortOrder = 'newest' | 'oldest' | 'alphabetical' | 'updated';

export interface UserSettings {
  theme: 'dark' | 'light' | 'system';
  accentColor: string;
  defaultView: ViewMode;
  sortOrder: SortOrder;
  enableBiometric: boolean;
  dailyReminderTime: string | null;
  hasCompletedOnboarding: boolean;
}

export const BuiltInTemplates: Template[] = [
  {
    id: 'template-meeting',
    name: 'Meeting Notes',
    icon: '📋',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## Meeting Notes
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
`
  },
  {
    id: 'template-daily',
    name: 'Daily Journal',
    icon: '📔',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## {{date}}

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
`
  },
  {
    id: 'template-book',
    name: 'Book Summary',
    icon: '📚',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## Book Summary
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
`
  },
  {
    id: 'template-project',
    name: 'Project Plan',
    icon: '🚀',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## Project: 
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
`
  },
  {
    id: 'template-quick',
    name: 'Quick Note',
    icon: '⚡',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: ''
  },
  {
    id: 'template-weekly',
    name: 'Weekly Review',
    icon: '📆',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## Weekly Review — Week of {{date}}

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
`
  },
  {
    id: 'template-habit',
    name: 'Habit Tracker',
    icon: '✅',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## Habit Tracker — {{date}}

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

`
  },
  {
    id: 'template-reading',
    name: 'Reading List',
    icon: '📖',
    isBuiltIn: true,
    createdAt: Date.now(),
    content: `## Reading List

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

`
  }
];
