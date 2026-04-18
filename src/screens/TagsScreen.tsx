import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Alert,
  TextInput,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import useStore from '../store/useStore';
import { Colors, TagColors } from '../theme/colors';
import { Tag } from '../types';
import { RootStackParamList } from '../navigation';

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const TagsScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { tags, createTag, deleteTag, notes } = useStore();
  const [isCreating, setIsCreating] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [selectedColor, setSelectedColor] = useState(TagColors[0]);

  const getTagNoteCount = (tagId: string) => {
    return notes.filter(n => n.tagIds.includes(tagId)).length;
  };

  const handleCreateTag = async () => {
    if (!newTagName.trim()) return;
    await createTag(newTagName.trim(), selectedColor);
    setNewTagName('');
    setIsCreating(false);
  };

  const handleDeleteTag = (tag: Tag) => {
    const noteCount = getTagNoteCount(tag.id);
    Alert.alert(
      'Delete Tag?',
      noteCount > 0 
        ? `This tag is used in ${noteCount} note${noteCount !== 1 ? 's' : ''}. The tag will be removed from all notes.`
        : 'Are you sure you want to delete this tag?',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Delete', style: 'destructive', onPress: () => deleteTag(tag.id) },
      ]
    );
  };

  const handleTagPress = (tag: Tag) => {
    navigation.navigate('Home');
    // Set selected tag would be handled by store
  };

  const renderTag = ({ item }: { item: Tag }) => (
    <TouchableOpacity 
      style={styles.tagItem}
      onPress={() => handleTagPress(item)}
      onLongPress={() => handleDeleteTag(item)}
    >
      <View style={[styles.tagColor, { backgroundColor: item.color }]} />
      <View style={styles.tagInfo}>
        <Text style={styles.tagName}>#{item.name}</Text>
        <Text style={styles.tagCount}>{getTagNoteCount(item.id)} notes</Text>
      </View>
      <Ionicons name="chevron-forward" size={20} color={Colors.textMuted} />
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={Colors.text} />
        </TouchableOpacity>
        <Text style={styles.title}>Tags</Text>
        <TouchableOpacity style={styles.addButton} onPress={() => setIsCreating(true)}>
          <Ionicons name="add" size={24} color={Colors.text} />
        </TouchableOpacity>
      </View>

      {/* Create Tag Form */}
      {isCreating && (
        <View style={styles.createForm}>
          <TextInput
            style={styles.input}
            placeholder="Tag name..."
            placeholderTextColor={Colors.textMuted}
            value={newTagName}
            onChangeText={setNewTagName}
            autoFocus
          />
          <View style={styles.colorRow}>
            {TagColors.map(color => (
              <TouchableOpacity
                key={color}
                style={[
                  styles.colorDot,
                  { backgroundColor: color },
                  selectedColor === color && styles.colorDotSelected,
                ]}
                onPress={() => setSelectedColor(color)}
              />
            ))}
          </View>
          <View style={styles.formActions}>
            <TouchableOpacity style={styles.cancelButton} onPress={() => setIsCreating(false)}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.createButton} onPress={handleCreateTag}>
              <Text style={styles.createText}>Create</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}

      {/* Tags List */}
      <FlatList
        data={tags}
        renderItem={renderTag}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyIcon}>🏷️</Text>
            <Text style={styles.emptyTitle}>No tags yet</Text>
            <Text style={styles.emptySubtitle}>Create tags to organize your notes</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  backButton: {
    padding: 8,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: Colors.text,
  },
  addButton: {
    padding: 8,
    backgroundColor: Colors.accent,
    borderRadius: 10,
  },
  createForm: {
    backgroundColor: Colors.surface,
    margin: 16,
    padding: 16,
    borderRadius: 16,
  },
  input: {
    backgroundColor: Colors.surfaceLight,
    borderRadius: 12,
    padding: 14,
    color: Colors.text,
    fontSize: 16,
    marginBottom: 12,
  },
  colorRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  colorDot: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  colorDotSelected: {
    borderWidth: 3,
    borderColor: Colors.text,
  },
  formActions: {
    flexDirection: 'row',
    gap: 12,
  },
  cancelButton: {
    flex: 1,
    padding: 14,
    backgroundColor: Colors.surfaceLight,
    borderRadius: 12,
    alignItems: 'center',
  },
  cancelText: {
    color: Colors.text,
    fontSize: 15,
    fontWeight: '500',
  },
  createButton: {
    flex: 1,
    padding: 14,
    backgroundColor: Colors.accent,
    borderRadius: 12,
    alignItems: 'center',
  },
  createText: {
    color: Colors.text,
    fontSize: 15,
    fontWeight: '600',
  },
  listContent: {
    padding: 16,
  },
  tagItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    padding: 16,
    borderRadius: 14,
    marginBottom: 10,
  },
  tagColor: {
    width: 16,
    height: 16,
    borderRadius: 8,
    marginRight: 12,
  },
  tagInfo: {
    flex: 1,
  },
  tagName: {
    color: Colors.text,
    fontSize: 16,
    fontWeight: '500',
  },
  tagCount: {
    color: Colors.textMuted,
    fontSize: 13,
    marginTop: 2,
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: 60,
  },
  emptyIcon: {
    fontSize: 48,
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: Colors.text,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: Colors.textSecondary,
  },
});
