import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Dimensions } from 'react-native';
import { Swipeable } from 'react-native-gesture-handler';
import { Ionicons } from '@expo/vector-icons';
import { Note, Tag } from '../types';
import { Colors } from '../theme/colors';

interface NoteCardProps {
  note: Note;
  tags: Tag[];
  onPress: () => void;
  onLongPress?: () => void;
  onDelete?: () => void;
  viewMode?: 'grid' | 'compact' | 'list';
  isTrash?: boolean;
}

const { width } = Dimensions.get('window');

export const NoteCard: React.FC<NoteCardProps> = ({
  note,
  tags,
  onPress,
  onLongPress,
  onDelete,
  viewMode = 'grid',
  isTrash = false,
}) => {
  const noteTags = tags.filter(t => note.tagIds.includes(t.id));
  const previewText = note.content
    .replace(/[#*\[\]`\-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 150);

  const renderRightActions = () => (
    <TouchableOpacity 
      style={[
        styles.deleteAction, 
        { backgroundColor: isTrash ? Colors.error : Colors.accent },
        viewMode === 'compact' && styles.deleteActionCompact
      ]}
      onPress={onDelete}
    >
      <Ionicons name={isTrash ? "trash" : "archive"} size={24} color={Colors.text} />
      <Text style={styles.deleteText}>{isTrash ? 'Delete' : 'Archive'}</Text>
    </TouchableOpacity>
  );

  if (viewMode === 'compact') {
    return (
      <Swipeable renderRightActions={renderRightActions}>
        <TouchableOpacity
          style={[styles.compactCard, { backgroundColor: note.color }]}
          onPress={onPress}
          onLongPress={onLongPress}
          activeOpacity={0.8}
        >
          <View style={styles.compactContent}>
            <View style={styles.compactHeader}>
              <Text style={styles.compactTitle} numberOfLines={1}>
                {note.title || 'Untitled'}
              </Text>
              <View style={styles.compactIcons}>
                {note.isPinned && <Ionicons name="pin" size={14} color={Colors.accent} style={styles.iconMargin} />}
                {note.isFavorite && <Ionicons name="star" size={14} color={Colors.warning} />}
              </View>
            </View>
            <Text style={styles.compactPreview} numberOfLines={1}>
              {previewText || 'No content'}
            </Text>
          </View>
        </TouchableOpacity>
      </Swipeable>
    );
  }

  if (viewMode === 'list') {
    return (
      <Swipeable renderRightActions={renderRightActions}>
        <TouchableOpacity
          style={[styles.listCard, { backgroundColor: note.color }]}
          onPress={onPress}
          onLongPress={onLongPress}
          activeOpacity={0.8}
        >
          <View style={styles.listContent}>
            <View style={styles.listHeader}>
              <Text style={styles.listTitle} numberOfLines={1}>
                {note.title || 'Untitled'}
              </Text>
              <View style={styles.listIcons}>
                {note.isPinned && <Ionicons name="pin" size={16} color={Colors.accent} style={styles.iconMargin} />}
                {note.isFavorite && <Ionicons name="star" size={16} color={Colors.warning} />}
              </View>
            </View>
            <Text style={styles.listPreview} numberOfLines={2}>
              {previewText || 'No content'}
            </Text>
            {noteTags.length > 0 && (
              <View style={styles.tagContainer}>
                {noteTags.slice(0, 3).map(tag => (
                  <View key={tag.id} style={[styles.tagChip, { backgroundColor: tag.color }]}>
                    <Text style={styles.tagText}>#{tag.name}</Text>
                  </View>
                ))}
                {noteTags.length > 3 && (
                  <Text style={styles.moreTags}>+{noteTags.length - 3}</Text>
                )}
              </View>
            )}
          </View>
        </TouchableOpacity>
      </Swipeable>
    );
  }

  // Grid view (default)
  return (
    <Swipeable renderRightActions={renderRightActions}>
      <TouchableOpacity
        style={[styles.card, { backgroundColor: note.color }]}
        onPress={onPress}
        onLongPress={onLongPress}
        activeOpacity={0.8}
      >
        <View style={styles.cardContent}>
          <View style={styles.header}>
            <Text style={styles.title} numberOfLines={2}>
              {note.title || 'Untitled'}
            </Text>
            <View style={styles.icons}>
              {note.isPinned && (
                <Ionicons name="pin" size={16} color={Colors.accent} style={styles.iconMargin} />
              )}
              {note.isFavorite && (
                <Ionicons name="star" size={16} color={Colors.warning} />
              )}
            </View>
          </View>
          
          <Text style={styles.preview} numberOfLines={4}>
            {previewText || 'No content'}
          </Text>
          
          {noteTags.length > 0 && (
            <View style={styles.tagContainer}>
              {noteTags.slice(0, 2).map(tag => (
                <View key={tag.id} style={[styles.tagChip, { backgroundColor: tag.color + '40' }]}>
                  <Text style={[styles.tagText, { color: tag.color }]}>#{tag.name}</Text>
                </View>
              ))}
            </View>
          )}
        </View>
      </TouchableOpacity>
    </Swipeable>
  );
};

const styles = StyleSheet.create({
  card: {
    flex: 1,
    margin: 6,
    borderRadius: 16,
    minHeight: 160,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 5,
  },
  cardContent: {
    padding: 16,
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  title: {
    fontSize: 16,
    fontWeight: '600',
    color: Colors.text,
    flex: 1,
    marginRight: 8,
  },
  icons: {
    flexDirection: 'row',
  },
  iconMargin: {
    marginRight: 6,
  },
  preview: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
  },
  tagContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 12,
    gap: 6,
  },
  tagChip: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  tagText: {
    fontSize: 11,
    fontWeight: '500',
  },
  moreTags: {
    fontSize: 11,
    color: Colors.textMuted,
    marginLeft: 4,
  },
  deleteAction: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 80,
    marginVertical: 6,
    marginRight: 6,
    borderRadius: 16,
  },
  deleteActionCompact: {
    marginVertical: 2,
  },
  deleteText: {
    color: Colors.text,
    fontSize: 12,
    marginTop: 4,
  },
  
  // Compact styles
  compactCard: {
    marginHorizontal: 12,
    marginVertical: 4,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 4,
    elevation: 3,
  },
  compactContent: {
    padding: 12,
  },
  compactHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  compactTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.text,
    flex: 1,
  },
  compactIcons: {
    flexDirection: 'row',
    marginLeft: 8,
  },
  compactPreview: {
    fontSize: 13,
    color: Colors.textSecondary,
    marginTop: 4,
  },
  
  // List styles
  listCard: {
    marginHorizontal: 12,
    marginVertical: 6,
    borderRadius: 14,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.15,
    shadowRadius: 6,
    elevation: 4,
  },
  listContent: {
    padding: 16,
  },
  listHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  listTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: Colors.text,
    flex: 1,
  },
  listIcons: {
    flexDirection: 'row',
    marginLeft: 8,
  },
  listPreview: {
    fontSize: 14,
    color: Colors.textSecondary,
    lineHeight: 20,
  },
});
