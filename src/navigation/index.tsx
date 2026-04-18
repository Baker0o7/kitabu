import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { HomeScreen } from '../screens/HomeScreen';
import { EditorScreen } from '../screens/EditorScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { OnboardingScreen } from '../screens/OnboardingScreen';
import { TagsScreen } from '../screens/TagsScreen';
import { TemplatesScreen } from '../screens/TemplatesScreen';
import { BuildReleaseScreen } from '../screens/BuildReleaseScreen';
import useStore from '../store/useStore';

export type RootStackParamList = {
  Onboarding: undefined;
  Home: undefined;
  Editor: { noteId?: string; templateId?: string };
  Settings: undefined;
  Tags: undefined;
  Templates: undefined;
  BuildRelease: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export const Navigation: React.FC = () => {
  const { settings, isInitialized } = useStore();

  if (!isInitialized) {
    return null;
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
          animation: 'slide_from_right',
        }}
      >
        {!settings.hasCompletedOnboarding ? (
          <Stack.Screen name="Onboarding" component={OnboardingScreen} />
        ) : (
          <>
            <Stack.Screen name="Home" component={HomeScreen} />
            <Stack.Screen 
              name="Editor" 
              component={EditorScreen}
              options={{
                animation: 'slide_from_bottom',
              }}
            />
            <Stack.Screen name="Settings" component={SettingsScreen} />
            <Stack.Screen name="Tags" component={TagsScreen} />
            <Stack.Screen name="Templates" component={TemplatesScreen} />
            <Stack.Screen name="BuildRelease" component={BuildReleaseScreen} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};
