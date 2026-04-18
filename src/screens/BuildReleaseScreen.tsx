import React from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  ScrollView, 
  TouchableOpacity,
  Linking,
  Clipboard,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { Colors } from '../theme/colors';
import { BuildOption } from '../components/BuildOption';

export const BuildReleaseScreen: React.FC = () => {
  const navigation = useNavigation();

  const copyToClipboard = (text: string) => {
    Clipboard.setString(text);
    Alert.alert('Copied', 'Command copied to clipboard');
  };

  const openGitHub = () => {
    Linking.openURL('https://github.com/Baker0o7/KITABU');
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={Colors.text} />
        </TouchableOpacity>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>Release APK</Text>
          <Text style={styles.headerSubtitle}>Build Options</Text>
        </View>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        {/* Intro */}
        <View style={styles.introSection}>
          <View style={styles.iconContainer}>
            <Ionicons name="cube-outline" size={32} color={Colors.accent} />
          </View>
          <Text style={styles.introTitle}>Build Your App</Text>
          <Text style={styles.introText}>
            Choose the best method to build your Android APK. We recommend Option 1 for the simplest experience.
          </Text>
        </View>

        {/* Option 1 - Featured */}
        <BuildOption
          number="01"
          title="EAS Cloud Build"
          subtitle="Expo Application Services"
          description="The fastest way to build your APK. No local setup required. Cloud-based builds with automatic signing."
          features={[
            'No Android Studio needed',
            'Automatic code signing',
            'Build takes ~10 minutes',
            'Free tier: 30 builds/month',
            'Download link via email',
          ]}
          actionLabel="Start Cloud Build"
          onAction={() => copyToClipboard('npx eas build --platform android --profile preview')}
          isRecommended={true}
        />

        {/* Quick Commands */}
        <View style={styles.commandsSection}>
          <Text style={styles.sectionTitle}>Quick Commands</Text>
          
          <View style={styles.commandCard}>
            <View style={styles.commandHeader}>
              <Text style={styles.commandLabel}>Preview APK</Text>
              <TouchableOpacity onPress={() => copyToClipboard('npx eas build --platform android --profile preview')}>
                <Ionicons name="copy-outline" size={20} color={Colors.textMuted} />
              </TouchableOpacity>
            </View>
            <Text style={styles.commandText}>
              npx eas build --platform android --profile preview
            </Text>
          </View>

          <View style={styles.commandCard}>
            <View style={styles.commandHeader}>
              <Text style={styles.commandLabel}>Production APK</Text>
              <TouchableOpacity onPress={() => copyToClipboard('npx eas build --platform android --profile production')}>
                <Ionicons name="copy-outline" size={20} color={Colors.textMuted} />
              </TouchableOpacity>
            </View>
            <Text style={styles.commandText}>
              npx eas build --platform android --profile production
            </Text>
          </View>
        </View>

        {/* Steps */}
        <View style={styles.stepsSection}>
          <Text style={styles.sectionTitle}>Build Steps</Text>
          
          {[
            { step: '1', text: 'Login to Expo: npx eas login' },
            { step: '2', text: 'Run build command' },
            { step: '3', text: 'Wait for email with download link' },
            { step: '4', text: 'Install APK on your device' },
          ].map((item, index) => (
            <View key={index} style={styles.stepItem}>
              <View style={styles.stepNumber}>
                <Text style={styles.stepNumberText}>{item.step}</Text>
              </View>
              <Text style={styles.stepText}>{item.text}</Text>
            </View>
          ))}
        </View>

        {/* GitHub Link */}
        <TouchableOpacity style={styles.githubCard} onPress={openGitHub}>
          <View style={styles.githubIcon}>
            <Ionicons name="logo-github" size={28} color={Colors.text} />
          </View>
          <View style={styles.githubContent}>
            <Text style={styles.githubTitle}>View on GitHub</Text>
            <Text style={styles.githubUrl}>github.com/Baker0o7/KITABU</Text>
          </View>
          <Ionicons name="open-outline" size={20} color={Colors.textMuted} />
        </TouchableOpacity>

        <View style={{ height: 40 }} />
      </ScrollView>
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
  headerContent: {
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 17,
    fontWeight: '600',
    color: Colors.text,
  },
  headerSubtitle: {
    fontSize: 13,
    color: Colors.textMuted,
    marginTop: 2,
  },
  scrollView: {
    flex: 1,
  },
  introSection: {
    alignItems: 'center',
    paddingVertical: 32,
    paddingHorizontal: 40,
  },
  iconContainer: {
    width: 72,
    height: 72,
    borderRadius: 24,
    backgroundColor: Colors.accent + '12',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
  },
  introTitle: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 12,
    letterSpacing: -0.5,
  },
  introText: {
    fontSize: 15,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
  },
  commandsSection: {
    paddingHorizontal: 20,
    marginTop: 8,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textMuted,
    letterSpacing: 0.5,
    textTransform: 'uppercase',
    marginBottom: 16,
    marginLeft: 4,
  },
  commandCard: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.surfaceLight,
  },
  commandHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  commandLabel: {
    fontSize: 13,
    fontWeight: '500',
    color: Colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  commandText: {
    fontSize: 14,
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    color: Colors.text,
    backgroundColor: Colors.surfaceLight,
    padding: 12,
    borderRadius: 8,
  },
  stepsSection: {
    paddingHorizontal: 20,
    marginTop: 24,
  },
  stepItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  stepNumber: {
    width: 32,
    height: 32,
    borderRadius: 10,
    backgroundColor: Colors.accent + '15',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  stepNumberText: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.accent,
  },
  stepText: {
    fontSize: 15,
    color: Colors.text,
    fontWeight: '500',
  },
  githubCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    marginHorizontal: 20,
    marginTop: 24,
    padding: 18,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: Colors.surfaceLight,
  },
  githubIcon: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: Colors.surfaceLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  githubContent: {
    flex: 1,
  },
  githubTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: Colors.text,
    marginBottom: 4,
  },
  githubUrl: {
    fontSize: 13,
    color: Colors.textMuted,
  },
});
