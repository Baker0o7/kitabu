import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Switch,
  Alert,
  Modal,
  Share,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import useStore from '../store/useStore';
import { Colors, TagColors } from '../theme/colors';
import { RootStackParamList } from '../navigation';

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const SettingsScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { settings, updateSettings, exportData, importData, tags, folders } = useStore();
  
  const [showAccentPicker, setShowAccentPicker] = useState(false);
  const [showThemePicker, setShowThemePicker] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  const handleExport = async () => {
    setIsExporting(true);
    try {
      const data = await exportData();
      await Share.share({
        message: data,
        title: 'Kitabu Backup',
      });
    } catch (error) {
      Alert.alert('Error', 'Failed to export data');
    }
    setIsExporting(false);
  };

  const handleImport = async () => {
    Alert.alert(
      'Import Data',
      'This will replace all your current data. Are you sure?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Import',
          onPress: async () => {
            setIsImporting(true);
            // In real app, would use DocumentPicker
            // For now, show placeholder
            Alert.alert('Import', 'Please paste your backup JSON');
            setIsImporting(false);
          },
        },
      ]
    );
  };

  const handleClearData = () => {
    Alert.alert(
      'Clear All Data',
      'This will permanently delete all notes, folders, and tags. This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete Everything',
          style: 'destructive',
          onPress: () => {
            Alert.alert('Coming Soon', 'Data clearing will be implemented');
          },
        },
      ]
    );
  };

  interface SettingItem {
    icon: string;
    label: string;
    value?: React.ReactNode;
    onPress?: () => void;
    loading?: boolean;
    danger?: boolean;
  }

  const settingsSections: { title: string; items: SettingItem[] }[] = [
    {
      title: 'Appearance',
      items: [
        {
          icon: 'color-palette',
          label: 'Accent Color',
          value: (
            <View style={[styles.colorPreview, { backgroundColor: settings.accentColor }]} />
          ),
          onPress: () => setShowAccentPicker(true),
        },
        {
          icon: 'moon',
          label: 'Theme',
          value: (
            <Text style={styles.settingValue}>
              {settings.theme === 'dark' ? 'Dark' : settings.theme === 'light' ? 'Light' : 'System'}
            </Text>
          ),
          onPress: () => setShowThemePicker(true),
        },
      ],
    },
    {
      title: 'Notes',
      items: [
        {
          icon: 'grid',
          label: 'Default View',
          value: (
            <Text style={styles.settingValue}>
              {settings.defaultView === 'grid' ? 'Grid' : settings.defaultView === 'list' ? 'List' : 'Compact'}
            </Text>
          ),
          onPress: () => {
            const views: ('grid' | 'list' | 'compact')[] = ['grid', 'list', 'compact'];
            const currentIndex = views.indexOf(settings.defaultView);
            updateSettings({ defaultView: views[(currentIndex + 1) % views.length] });
          },
        },
        {
          icon: 'finger-print',
          label: 'Biometric Lock',
          value: (
            <Switch
              value={settings.enableBiometric}
              onValueChange={(v) => updateSettings({ enableBiometric: v })}
              trackColor={{ false: Colors.surfaceLight, true: Colors.accent }}
            />
          ),
        },
      ],
    },
    {
      title: 'Data',
      items: [
        {
          icon: 'download',
          label: 'Export Backup',
          onPress: handleExport,
          loading: isExporting,
          danger: false,
        },
        {
          icon: 'upload',
          label: 'Import Backup',
          onPress: handleImport,
          loading: isImporting,
          danger: false,
        },
        {
          icon: 'trash',
          label: 'Clear All Data',
          danger: true,
          onPress: handleClearData,
          loading: false,
        },
      ],
    },
    {
      title: 'Developer',
      items: [
        {
          icon: 'cube',
          label: 'Build Release APK',
          onPress: () => navigation.navigate('BuildRelease'),
          loading: false,
          danger: false,
        },
      ],
    },
    {
      title: 'About',
      items: [
        {
          icon: 'information-circle',
          label: 'Version',
          value: <Text style={styles.settingValue}>1.0.0</Text>,
        },
        {
          icon: 'heart',
          label: 'Made with ❤️ by Kitabu',
        },
      ],
    },
  ];

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={Colors.text} />
        </TouchableOpacity>
        <Text style={styles.title}>Settings</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView style={styles.content}>
        {/* Stats */}
        <View style={styles.statsCard}>
          <View style={styles.statItem}>
            <Text style={styles.statNumber}>0</Text>
            <Text style={styles.statLabel}>Notes</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statNumber}>{folders.length}</Text>
            <Text style={styles.statLabel}>Folders</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statNumber}>{tags.length}</Text>
            <Text style={styles.statLabel}>Tags</Text>
          </View>
        </View>

        {/* Settings Sections */}
        {settingsSections.map((section, sectionIndex) => (
          <View key={section.title} style={styles.section}>
            <Text style={styles.sectionTitle}>{section.title}</Text>
            <View style={styles.sectionContent}>
              {section.items.map((item, itemIndex) => (
                <TouchableOpacity
                  key={item.label}
                  style={[
                    styles.settingItem,
                    itemIndex === section.items.length - 1 && styles.settingItemLast,
                  ]}
                  onPress={item.onPress}
                  disabled={!item.onPress || item.loading}
                >
                  <View style={styles.settingLeft}>
                    <View style={[styles.iconContainer, item.danger && styles.iconContainerDanger]}>
                      <Ionicons 
                        name={item.icon as any} 
                        size={20} 
                        color={item.danger ? Colors.error : Colors.text} 
                      />
                    </View>
                    <Text style={[styles.settingLabel, item.danger && styles.settingLabelDanger]}>
                      {item.label}
                    </Text>
                  </View>
                  <View style={styles.settingRight}>
                    {item.loading ? (
                      <ActivityIndicator size="small" color={Colors.accent} />
                    ) : (
                      item.value
                    )}
                    {item.onPress && !item.value && !item.loading && (
                      <Ionicons name="chevron-forward" size={20} color={Colors.textMuted} />
                    )}
                  </View>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        ))}
        
        <View style={{ height: 40 }} />
      </ScrollView>

      {/* Accent Color Picker */}
      <Modal
        visible={showAccentPicker}
        transparent
        animationType="slide"
        onRequestClose={() => setShowAccentPicker(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Accent Color</Text>
              <TouchableOpacity onPress={() => setShowAccentPicker(false)}>
                <Ionicons name="close" size={24} color={Colors.text} />
              </TouchableOpacity>
            </View>
            <View style={styles.colorGrid}>
              {TagColors.map((color) => (
                <TouchableOpacity
                  key={color}
                  style={[
                    styles.colorButton,
                    { backgroundColor: color },
                    settings.accentColor === color && styles.colorButtonSelected,
                  ]}
                  onPress={() => { updateSettings({ accentColor: color }); setShowAccentPicker(false); }}
                />
              ))}
            </View>
          </View>
        </View>
      </Modal>

      {/* Theme Picker */}
      <Modal
        visible={showThemePicker}
        transparent
        animationType="slide"
        onRequestClose={() => setShowThemePicker(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Theme</Text>
              <TouchableOpacity onPress={() => setShowThemePicker(false)}>
                <Ionicons name="close" size={24} color={Colors.text} />
              </TouchableOpacity>
            </View>
            {['dark', 'light', 'system'].map((theme) => (
              <TouchableOpacity
                key={theme}
                style={[styles.themeOption, settings.theme === theme && styles.themeOptionSelected]}
                onPress={() => { updateSettings({ theme: theme as any }); setShowThemePicker(false); }}
              >
                <Ionicons 
                  name={theme === 'dark' ? 'moon' : theme === 'light' ? 'sunny' : 'phone-portrait'} 
                  size={24} 
                  color={Colors.text} 
                />
                <Text style={styles.themeOptionText}>
                  {theme.charAt(0).toUpperCase() + theme.slice(1)}
                </Text>
                {settings.theme === theme && (
                  <Ionicons name="checkmark" size={24} color={Colors.accent} />
                )}
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </Modal>
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
  content: {
    flex: 1,
  },
  statsCard: {
    flexDirection: 'row',
    backgroundColor: Colors.surface,
    margin: 16,
    padding: 20,
    borderRadius: 16,
    justifyContent: 'space-around',
  },
  statItem: {
    alignItems: 'center',
  },
  statNumber: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.accent,
  },
  statLabel: {
    fontSize: 14,
    color: Colors.textSecondary,
    marginTop: 4,
  },
  statDivider: {
    width: 1,
    backgroundColor: Colors.surfaceLight,
  },
  section: {
    marginBottom: 24,
    paddingHorizontal: 16,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 8,
    marginLeft: 12,
  },
  sectionContent: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    overflow: 'hidden',
  },
  settingItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 14,
    borderBottomWidth: 1,
    borderBottomColor: Colors.surfaceLight,
  },
  settingItemLast: {
    borderBottomWidth: 0,
  },
  settingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconContainer: {
    width: 32,
    height: 32,
    borderRadius: 8,
    backgroundColor: Colors.surfaceLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  iconContainerDanger: {
    backgroundColor: Colors.error + '20',
  },
  settingLabel: {
    fontSize: 16,
    color: Colors.text,
  },
  settingLabelDanger: {
    color: Colors.error,
  },
  settingRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  settingValue: {
    fontSize: 15,
    color: Colors.textMuted,
  },
  colorPreview: {
    width: 24,
    height: 24,
    borderRadius: 12,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  modalTitle: {
    color: Colors.text,
    fontSize: 18,
    fontWeight: '600',
  },
  colorGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    justifyContent: 'center',
  },
  colorButton: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },
  colorButtonSelected: {
    borderWidth: 3,
    borderColor: Colors.text,
  },
  themeOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 12,
    marginBottom: 8,
  },
  themeOptionSelected: {
    backgroundColor: Colors.accent + '20',
  },
  themeOptionText: {
    color: Colors.text,
    fontSize: 16,
    marginLeft: 12,
    flex: 1,
  },
});
