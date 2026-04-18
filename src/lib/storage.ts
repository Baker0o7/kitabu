import AsyncStorage from '@react-native-async-storage/async-storage';
import { Note, Folder, Tag, Template, NoteVersion, UserSettings, BuiltInTemplates } from '../types';

const KEYS = {
  notes: 'kitabu_notes',
  folders: 'kitabu_folders',
  tags: 'kitabu_tags',
  templates: 'kitabu_templates',
  versions: 'kitabu_versions',
  settings: 'kitabu_settings',
};

// Notes
export async function getNotes(): Promise<Note[]> {
  const data = await AsyncStorage.getItem(KEYS.notes);
  return data ? JSON.parse(data) : [];
}

export async function saveNotes(notes: Note[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.notes, JSON.stringify(notes));
}

export async function getNoteById(id: string): Promise<Note | null> {
  const notes = await getNotes();
  return notes.find(n => n.id === id) || null;
}

// Folders
export async function getFolders(): Promise<Folder[]> {
  const data = await AsyncStorage.getItem(KEYS.folders);
  return data ? JSON.parse(data) : [];
}

export async function saveFolders(folders: Folder[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.folders, JSON.stringify(folders));
}

// Tags
export async function getTags(): Promise<Tag[]> {
  const data = await AsyncStorage.getItem(KEYS.tags);
  return data ? JSON.parse(data) : [];
}

export async function saveTags(tags: Tag[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.tags, JSON.stringify(tags));
}

// Templates
export async function getTemplates(): Promise<Template[]> {
  const data = await AsyncStorage.getItem(KEYS.templates);
  const userTemplates = data ? JSON.parse(data) : [];
  return [...BuiltInTemplates, ...userTemplates];
}

export async function saveUserTemplates(templates: Template[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.templates, JSON.stringify(templates));
}

// Versions
export async function getVersions(): Promise<NoteVersion[]> {
  const data = await AsyncStorage.getItem(KEYS.versions);
  return data ? JSON.parse(data) : [];
}

export async function saveVersions(versions: NoteVersion[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.versions, JSON.stringify(versions));
}

export async function addVersion(noteId: string, title: string, content: string): Promise<void> {
  const versions = await getVersions();
  const newVersion: NoteVersion = {
    id: `version_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    noteId,
    title,
    content,
    createdAt: Date.now(),
  };
  // Keep only last 20 versions per note
  const noteVersions = versions.filter(v => v.noteId === noteId);
  if (noteVersions.length >= 20) {
    const toDelete = noteVersions[0];
    const filtered = versions.filter(v => v.id !== toDelete.id);
    await saveVersions([...filtered, newVersion]);
  } else {
    await saveVersions([...versions, newVersion]);
  }
}

// Settings
export async function getSettings(): Promise<UserSettings> {
  const data = await AsyncStorage.getItem(KEYS.settings);
  const defaults: UserSettings = {
    theme: 'dark',
    accentColor: '#6C63FF',
    defaultView: 'grid',
    sortOrder: 'newest',
    enableBiometric: false,
    dailyReminderTime: null,
    hasCompletedOnboarding: false,
  };
  return data ? { ...defaults, ...JSON.parse(data) } : defaults;
}

export async function saveSettings(settings: UserSettings): Promise<void> {
  await AsyncStorage.setItem(KEYS.settings, JSON.stringify(settings));
}

// Export/Import
export async function exportAllData(): Promise<string> {
  const [notes, folders, tags, templates, settings] = await Promise.all([
    getNotes(),
    getFolders(),
    getTags(),
    AsyncStorage.getItem(KEYS.templates),
    getSettings(),
  ]);
  
  return JSON.stringify({
    notes,
    folders,
    tags,
    userTemplates: templates ? JSON.parse(templates) : [],
    settings,
    exportedAt: new Date().toISOString(),
  }, null, 2);
}

export async function importAllData(jsonString: string): Promise<{ success: boolean; message: string }> {
  try {
    const data = JSON.parse(jsonString);
    if (data.notes) await saveNotes(data.notes);
    if (data.folders) await saveFolders(data.folders);
    if (data.tags) await saveTags(data.tags);
    if (data.userTemplates) await saveUserTemplates(data.userTemplates);
    if (data.settings) await saveSettings({ ...await getSettings(), ...data.settings });
    return { success: true, message: `Imported ${data.notes?.length || 0} notes successfully` };
  } catch (e) {
    return { success: false, message: 'Invalid backup file' };
  }
}

// Initialize with sample data if empty
export async function initializeStorage(): Promise<void> {
  const notes = await getNotes();
  if (notes.length === 0) {
    // Create welcome note
    const welcomeNote: Note = {
      id: `note_${Date.now()}`,
      title: 'Welcome to Kitabu 📚',
      content: `# Welcome to Kitabu!

Kitabu is your personal space for thoughts, ideas, and notes.

## Features

- 📝 **Rich Markdown Editor** - Write with style
- 🏷️ **Tags & Folders** - Organize your notes
- 📋 **Templates** - Quick start your writing
- ⭐ **Favorites** - Pin important notes
- 🔒 **Biometric Lock** - Keep notes private
- 📤 **Export/Import** - Backup your data

## Quick Tips

- Use **Markdown** formatting
- Create [[Wiki Links]] between notes
- Set reminders for important notes
- Use templates for consistent formatting

Happy writing! ✨`,
      color: '#1E1E2E',
      folderId: null,
      isPinned: true,
      isLocked: false,
      isArchived: false,
      isDaily: false,
      dailyDate: null,
      templateId: null,
      reminderTime: null,
      isTrashed: false,
      trashedAt: null,
      isFavorite: true,
      tagIds: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    await saveNotes([welcomeNote]);
  }
}
