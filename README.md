# 📓 Kitabu v2.0

A full-featured, dark-themed **second brain** notes app for Android — built in Kotlin with Room, MVVM, Markwon, and Claude AI.

---

## ✨ Feature Set

### Core
| Feature | Details |
|---|---|
| Rich Markdown editing | Bold, italic, strikethrough, code, H1-H3, lists, task lists, tables, horizontal rules |
| Live preview toggle | One tap switches between edit and rendered Markdown |
| Wikilink `[[note title]]` | Cross-link notes; backlink count shown in editor |
| 6 note colour themes | Per-note background colour picker |
| Staggered grid list | Pinterest-style card layout with tag chips |
| Pin & lock notes | Pinned notes float to top; locked notes require biometric auth |

### Organisation
| Feature | Details |
|---|---|
| Nested tags | `#work/meetings`, `#personal/health` — tag autocomplete in editor |
| Filter by tag | Tap any tag in the drawer to filter the note list |
| Sort | Last edited · Date created · Title A–Z |
| Full-text search | Debounced real-time search across title + body |
| Daily notes | Auto-creates a dated note per day; separate list in drawer |
| Templates | 5 built-in (Meeting, Journal, Book Summary, Project Plan, Quick) + custom |
| Version history | Auto-snapshot on save, browse & restore up to 30 versions per note |

### AI (requires Anthropic API key)
| Feature | Details |
|---|---|
| AI Assistant | Full chat interface with note context injected as system prompt |
| Quick actions | Summarise · Expand · Rewrite · Flashcards · Suggest tags · Outline |
| Voice-to-text | Tap 🎙 in the Markdown toolbar to dictate |

### Visual
| Feature | Details |
|---|---|
| Knowledge Graph | Force-directed graph of all notes; edges from `[[wikilinks]]`; tap node to open; pinch to zoom; pan |
| Relative dates | "Just now / 5m ago / Yesterday / Mar 3" |

---

## 🏗 Architecture

```
kitabu/
└── app/src/main/
    ├── kotlin/com/kitabu/app/
    │   ├── data/
    │   │   ├── Note.kt, Tag.kt, NoteTag.kt        ← Entities
    │   │   ├── NoteWithTags.kt                    ← @Relation result
    │   │   ├── NoteVersion.kt, Template.kt        ← History & templates
    │   │   ├── NoteDao, TagDao, NoteVersionDao,
    │   │   │   TemplateDao                        ← Room DAOs
    │   │   ├── KitabuDatabase.kt                  ← DB + migration v1→v2
    │   │   └── NoteRepository, TagRepository,
    │   │       TemplateRepository                 ← Single sources of truth
    │   ├── ui/
    │   │   ├── notes/   MainActivity, NoteAdapter, NoteViewModel
    │   │   ├── editor/  EditorActivity (Markdown toolbar, voice, autosave)
    │   │   ├── ai/      AiAssistantActivity (Claude API)
    │   │   ├── graph/   GraphActivity + GraphView (custom canvas)
    │   │   ├── history/ VersionHistoryActivity
    │   │   ├── tags/    TagManagerActivity
    │   │   ├── templates/ TemplatesActivity
    │   │   └── settings/ SettingsActivity
    │   └── util/
    │       ├── MarkdownHelper.kt   ← Markwon + wikilink utils
    │       ├── BiometricHelper.kt  ← Biometric/PIN auth
    │       ├── Extensions.kt       ← View, date, string extensions
    │       ├── SortOrder.kt
    │       └── TextStats.kt
    └── res/
        ├── layout/   14 layouts
        ├── drawable/ 23 vector icons + backgrounds
        ├── menu/     3 menus (drawer, main, editor)
        ├── font/     Inter Regular / Medium / SemiBold
        └── values/   colors, strings, themes, dimens
```

**Stack:** MVVM · Room 2.6.1 · Kotlin Coroutines + Flow · Markwon 4.6.2 · OkHttp 4.12 · Biometric 1.1 · Material 3

---

## 🚀 Setup

### 1. Clone & open in Android Studio Hedgehog+

### 2. Add Inter fonts
Download from [fonts.google.com/specimen/Inter](https://fonts.google.com/specimen/Inter) → put in `app/src/main/res/font/`:
```
inter_regular.ttf   (weight 400)
inter_medium.ttf    (weight 500)
inter_semibold.ttf  (weight 600)
```

### 3. Enable AI features (optional)
Open the app → ☰ drawer → Settings → paste your **Anthropic API key**.  
Get one at [console.anthropic.com](https://console.anthropic.com).

### 4. Build
```bash
./gradlew assembleDebug
```
Min SDK: **24** · Target SDK: **34** · Java: **17**

---

## 🎨 Note Colours
`Default #1E1E2E` · `Rose #2D1B2E` · `Ocean #0D2137` · `Forest #0D2818` · `Amber #2D1F00` · `Lavender #1A1A2E`
