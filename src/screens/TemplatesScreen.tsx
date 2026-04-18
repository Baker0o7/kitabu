import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import useStore from '../store/useStore';
import { Colors } from '../theme/colors';
import { Template } from '../types';
import { RootStackParamList } from '../navigation';

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const TemplatesScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { templates } = useStore();

  const handleTemplatePress = (template: Template) => {
    navigation.navigate('Editor', { templateId: template.id });
  };

  const renderTemplate = ({ item }: { item: Template }) => (
    <TouchableOpacity 
      style={styles.templateItem}
      onPress={() => handleTemplatePress(item)}
    >
      <Text style={styles.templateIcon}>{item.icon}</Text>
      <View style={styles.templateInfo}>
        <Text style={styles.templateName}>{item.name}</Text>
        {item.isBuiltIn && (
          <Text style={styles.templateBadge}>Built-in</Text>
        )}
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
        <Text style={styles.title}>Templates</Text>
        <View style={{ width: 40 }} />
      </View>

      <Text style={styles.subtitle}>Choose a template to start writing</Text>

      {/* Templates List */}
      <FlatList
        data={templates}
        renderItem={renderTemplate}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
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
  subtitle: {
    fontSize: 14,
    color: Colors.textSecondary,
    paddingHorizontal: 24,
    marginBottom: 16,
  },
  listContent: {
    padding: 16,
  },
  templateItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    padding: 16,
    borderRadius: 16,
    marginBottom: 10,
  },
  templateIcon: {
    fontSize: 32,
    marginRight: 16,
  },
  templateInfo: {
    flex: 1,
  },
  templateName: {
    color: Colors.text,
    fontSize: 16,
    fontWeight: '500',
  },
  templateBadge: {
    color: Colors.accent,
    fontSize: 12,
    marginTop: 4,
  },
});
