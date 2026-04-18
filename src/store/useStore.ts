import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { 
  Note, Folder, Tag, Template, NoteVersion, 
  UserSettings, ViewMode, SortOrder, NoteColors, NoteColor 
} from '../types';
import * as storage from '../lib/storage';

interface AppState {
  // Data
  notes: Note[];
  folders: Folder[];
  tags: Tag[];
  templates: Template[];
  versions: NoteVersion[];
  settings: UserSettings;
  
  // UI State
  searchQuery: string;
  selectedTagId: string | null;
  selectedFolderId: string | null;
  viewMode: ViewMode;
  showingArchived: boolean;
  showingTrashed: boolean;
  showingFavorites: boolean;
  showingDaily: boolean;
  
  // Loading
  isLoading: boolean;
  isInitialized: boolean;
}

interface AppActions {
  // Initialization
  initialize: () => Promise<void>;
  
  // Notes
  createNote: (note?: Partial<Note>) => Promise<Note>;
  updateNote: (id: string, updates: Partial<Note>) => Promise<void>;
  deleteNote: (id: string) => Promise<void>;
  trashNote: (id: string) => Promise<void>;
  restoreNote: (id: string) => Promise<void>;
  togglePin: (id: string) => Promise<void>;
  toggleFavorite: (id: string) => Promise<void>;
  toggleArchive: (id: string) => Promise<void>;
  toggleLock: (id: string) => Promise<void>;
  setNoteColor: (id: string, color: NoteColor) => Promise<void>;
  setNoteTags: (id: string, tagIds: string[]) => Promise<void>;
  setNoteReminder: (id: string, time: number | null) => Promise<void>;
  saveNoteVersion: (id: string) => Promise<void>;
  restoreVersion: (versionId: string) => Promise<void>;
  getOrCreateDailyNote: () => Promise<Note>;
  
  // Folders
  createFolder: (folder: Partial<Folder>) => Promise<Folder>;
  updateFolder: (id: string, updates: Partial<Folder>) => Promise<void>;
  deleteFolder: (id: string) => Promise<void>;
  
  // Tags
  createTag: (name: string, color?: string) => Promise<Tag>;
  updateTag: (id: string, updates: Partial<Tag>) => Promise<void>;
  deleteTag: (id: string) => Promise<void>;
  getOrCreateTag: (name: string) => Promise<Tag>;
  
  // Templates
  createTemplate: (template: Partial<Template>) => Promise<Template>;
  deleteTemplate: (id: string) => Promise<void>;
  applyTemplate: (templateId: string) => Promise<string>;
  
  // UI
  setSearchQuery: (query: string) => void;
  setSelectedTag: (id: string | null) => void;
  setSelectedFolder: (id: string | null) => void;
  setViewMode: (mode: ViewMode) => void;
  setShowingArchived: (show: boolean) => void;
  setShowingTrashed: (show: boolean) => void;
  setShowingFavorites: (show: boolean) => void;
  setShowingDaily: (show: boolean) => void;
  
  // Settings
  updateSettings: (updates: Partial<UserSettings>) => Promise<void>;
  completeOnboarding: () => Promise<void>;
  
  // Data
  exportData: () => Promise<string>;
  importData: (json: string) => Promise<{ success: boolean; message: string }>;
  
  // Computed (getters)
  getFilteredNotes: () => Note[];
  getNoteById: (id: string) => Note | undefined;
  getVersionsForNote: (noteId: string) => NoteVersion[];
  getBacklinks: (title: string, excludeId: string) => Note[];
}

const useStore = create<AppState & AppActions>()(
  persist(
    (set, get) => ({
      // Initial State
      notes: [],
      folders: [],
      tags: [],
      templates: [],
      versions: [],
      settings: {
        theme: 'dark',
        accentColor: '#6C63FF',
        defaultView: 'grid',
        sortOrder: 'newest',
        enableBiometric: false,
        dailyReminderTime: null,
        hasCompletedOnboarding: false,
      },
      searchQuery: '',
      selectedTagId: null,
      selectedFolderId: null,
      viewMode: 'grid',
      showingArchived: false,
      showingTrashed: false,
      showingFavorites: false,
      showingDaily: false,
      isLoading: false,
      isInitialized: false,

      // Initialization
      initialize: async () => {
        set({ isLoading: true });
        await storage.initializeStorage();
        const [notes, folders, tags, templates, versions, settings] = await Promise.all([
          storage.getNotes(),
          storage.getFolders(),
          storage.getTags(),
          storage.getTemplates(),
          storage.getVersions(),
          storage.getSettings(),
        ]);
        set({ 
          notes, folders, tags, templates, versions, settings,
          viewMode: settings.defaultView,
          isLoading: false,
          isInitialized: true,
        });
      },

      // Notes
      createNote: async (noteData = {}) => {
        const newNote: Note = {
          id: `note_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          title: '',
          content: '',
          color: NoteColors[0],
          folderId: null,
          isPinned: false,
          isLocked: false,
          isArchived: false,
          isDaily: false,
          dailyDate: null,
          templateId: null,
          reminderTime: null,
          isTrashed: false,
          trashedAt: null,
          isFavorite: false,
          tagIds: [],
          createdAt: Date.now(),
          updatedAt: Date.now(),
          ...noteData,
        };
        const notes = [...get().notes, newNote];
        await storage.saveNotes(notes);
        set({ notes });
        return newNote;
      },

      updateNote: async (id, updates) => {
        const notes = get().notes.map(n => 
          n.id === id ? { ...n, ...updates, updatedAt: Date.now() } : n
        );
        await storage.saveNotes(notes);
        set({ notes });
      },

      deleteNote: async (id) => {
        const notes = get().notes.filter(n => n.id !== id);
        await storage.saveNotes(notes);
        set({ notes });
      },

      trashNote: async (id) => {
        const notes = get().notes.map(n => 
          n.id === id ? { ...n, isTrashed: true, trashedAt: Date.now() } : n
        );
        await storage.saveNotes(notes);
        set({ notes });
      },

      restoreNote: async (id) => {
        const notes = get().notes.map(n => 
          n.id === id ? { ...n, isTrashed: false, trashedAt: null } : n
        );
        await storage.saveNotes(notes);
        set({ notes });
      },

      togglePin: async (id) => {
        const note = get().notes.find(n => n.id === id);
        if (note) {
          await get().updateNote(id, { isPinned: !note.isPinned });
        }
      },

      toggleFavorite: async (id) => {
        const note = get().notes.find(n => n.id === id);
        if (note) {
          await get().updateNote(id, { isFavorite: !note.isFavorite });
        }
      },

      toggleArchive: async (id) => {
        const note = get().notes.find(n => n.id === id);
        if (note) {
          await get().updateNote(id, { isArchived: !note.isArchived });
        }
      },

      toggleLock: async (id) => {
        const note = get().notes.find(n => n.id === id);
        if (note) {
          await get().updateNote(id, { isLocked: !note.isLocked });
        }
      },

      setNoteColor: async (id, color) => {
        await get().updateNote(id, { color });
      },

      setNoteTags: async (id, tagIds) => {
        await get().updateNote(id, { tagIds });
      },

      setNoteReminder: async (id, reminderTime) => {
        await get().updateNote(id, { reminderTime });
      },

      saveNoteVersion: async (id) => {
        const note = get().notes.find(n => n.id === id);
        if (note) {
          await storage.addVersion(id, note.title, note.content);
          const versions = await storage.getVersions();
          set({ versions });
        }
      },

      restoreVersion: async (versionId) => {
        const version = get().versions.find(v => v.id === versionId);
        if (version) {
          await get().updateNote(version.noteId, { 
            title: version.title, 
            content: version.content 
          });
        }
      },

      getOrCreateDailyNote: async () => {
        const today = new Date().toISOString().split('T')[0];
        const existing = get().notes.find(n => n.dailyDate === today);
        if (existing) return existing;
        
        return await get().createNote({
          title: `Daily Note - ${new Date().toLocaleDateString()}`,
          isDaily: true,
          dailyDate: today,
        });
      },

      // Folders
      createFolder: async (folderData) => {
        const newFolder: Folder = {
          id: `folder_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          name: 'New Folder',
          parentFolderId: null,
          color: '#6C63FF',
          icon: '📁',
          isArchived: false,
          isFavorite: false,
          createdAt: Date.now(),
          updatedAt: Date.now(),
          ...folderData,
        };
        const folders = [...get().folders, newFolder];
        await storage.saveFolders(folders);
        set({ folders });
        return newFolder;
      },

      updateFolder: async (id, updates) => {
        const folders = get().folders.map(f => 
          f.id === id ? { ...f, ...updates, updatedAt: Date.now() } : f
        );
        await storage.saveFolders(folders);
        set({ folders });
      },

      deleteFolder: async (id) => {
        const folders = get().folders.filter(f => f.id !== id);
        // Move notes to root
        const notes = get().notes.map(n => 
          n.folderId === id ? { ...n, folderId: null } : n
        );
        await storage.saveFolders(folders);
        await storage.saveNotes(notes);
        set({ folders, notes });
      },

      // Tags
      createTag: async (name, color = '#6C63FF') => {
        const newTag: Tag = {
          id: `tag_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          name: name.toLowerCase().replace(/\s+/g, '-'),
          color,
          createdAt: Date.now(),
        };
        const tags = [...get().tags, newTag];
        await storage.saveTags(tags);
        set({ tags });
        return newTag;
      },

      updateTag: async (id, updates) => {
        const tags = get().tags.map(t => 
          t.id === id ? { ...t, ...updates } : t
        );
        await storage.saveTags(tags);
        set({ tags });
      },

      deleteTag: async (id) => {
        const tags = get().tags.filter(t => t.id !== id);
        // Remove tag from all notes
        const notes = get().notes.map(n => ({
          ...n,
          tagIds: n.tagIds.filter(tid => tid !== id)
        }));
        await storage.saveTags(tags);
        await storage.saveNotes(notes);
        set({ tags, notes });
      },

      getOrCreateTag: async (name) => {
        const normalized = name.toLowerCase().replace(/\s+/g, '-');
        const existing = get().tags.find(t => t.name === normalized);
        if (existing) return existing;
        return await get().createTag(name);
      },

      // Templates
      createTemplate: async (templateData) => {
        const newTemplate: Template = {
          id: `template_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          name: 'New Template',
          content: '',
          icon: '📄',
          isBuiltIn: false,
          createdAt: Date.now(),
          ...templateData,
        };
        const userTemplates = get().templates.filter(t => !t.isBuiltIn);
        const templates = [...userTemplates, newTemplate];
        await storage.saveUserTemplates(templates);
        set({ templates: [...get().templates.filter(t => t.isBuiltIn), ...templates] });
        return newTemplate;
      },

      deleteTemplate: async (id) => {
        const templates = get().templates.filter(t => t.id !== id);
        const userTemplates = templates.filter(t => !t.isBuiltIn);
        await storage.saveUserTemplates(userTemplates);
        set({ templates });
      },

      applyTemplate: async (templateId) => {
        const template = get().templates.find(t => t.id === templateId);
        if (!template) return '';
        
        return template.content
          .replace(/\{\{date\}\}/g, new Date().toLocaleDateString())
          .replace(/\{\{time\}\}/g, new Date().toLocaleTimeString());
      },

      // UI
      setSearchQuery: (query) => set({ searchQuery: query }),
      setSelectedTag: (id) => set({ selectedTagId: id }),
      setSelectedFolder: (id) => set({ selectedFolderId: id }),
      setViewMode: (mode) => set({ viewMode: mode }),
      setShowingArchived: (show) => set({ showingArchived: show }),
      setShowingTrashed: (show) => set({ showingTrashed: show }),
      setShowingFavorites: (show) => set({ showingFavorites: show }),
      setShowingDaily: (show) => set({ showingDaily: show }),

      // Settings
      updateSettings: async (updates) => {
        const settings = { ...get().settings, ...updates };
        await storage.saveSettings(settings);
        set({ settings });
      },

      completeOnboarding: async () => {
        await get().updateSettings({ hasCompletedOnboarding: true });
      },

      // Data
      exportData: async () => {
        return await storage.exportAllData();
      },

      importData: async (json) => {
        const result = await storage.importAllData(json);
        if (result.success) {
          await get().initialize();
        }
        return result;
      },

      // Computed getters
      getFilteredNotes: () => {
        const state = get();
        let filtered = state.notes;

        // Filter by trashed
        if (state.showingTrashed) {
          filtered = filtered.filter(n => n.isTrashed);
        } else {
          filtered = filtered.filter(n => !n.isTrashed);
        }

        // Filter by archived
        if (state.showingArchived) {
          filtered = filtered.filter(n => n.isArchived);
        } else {
          filtered = filtered.filter(n => !n.isArchived);
        }

        // Filter by favorites
        if (state.showingFavorites) {
          filtered = filtered.filter(n => n.isFavorite);
        }

        // Filter by daily
        if (state.showingDaily) {
          filtered = filtered.filter(n => n.isDaily);
        }

        // Filter by folder
        if (state.selectedFolderId) {
          filtered = filtered.filter(n => n.folderId === state.selectedFolderId);
        }

        // Filter by tag
        if (state.selectedTagId) {
          filtered = filtered.filter(n => n.tagIds.includes(state.selectedTagId!));
        }

        // Filter by search query
        if (state.searchQuery) {
          const query = state.searchQuery.toLowerCase();
          filtered = filtered.filter(n => 
            n.title.toLowerCase().includes(query) ||
            n.content.toLowerCase().includes(query)
          );
        }

        // Sort
        const sortOrder = state.settings.sortOrder;
        filtered = [...filtered].sort((a, b) => {
          if (a.isPinned && !b.isPinned) return -1;
          if (!a.isPinned && b.isPinned) return 1;
          
          switch (sortOrder) {
            case 'newest': return b.createdAt - a.createdAt;
            case 'oldest': return a.createdAt - b.createdAt;
            case 'updated': return b.updatedAt - a.updatedAt;
            case 'alphabetical': return a.title.localeCompare(b.title);
            default: return 0;
          }
        });

        return filtered;
      },

      getNoteById: (id) => {
        return get().notes.find(n => n.id === id);
      },

      getVersionsForNote: (noteId) => {
        return get().versions
          .filter(v => v.noteId === noteId)
          .sort((a, b) => b.createdAt - a.createdAt);
      },

      getBacklinks: (title, excludeId) => {
        const pattern = new RegExp(`\\[\\[${title.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\]\\]`, 'i');
        return get().notes.filter(n => 
          n.id !== excludeId && 
          !n.isTrashed && 
          pattern.test(n.content)
        );
      },
    }),
    {
      name: 'kitabu-storage',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        settings: state.settings,
        viewMode: state.viewMode,
      }),
    }
  )
);

export default useStore;
