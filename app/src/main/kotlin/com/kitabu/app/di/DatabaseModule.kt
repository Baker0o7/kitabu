package com.kitabu.app.di

import android.content.Context
import com.kitabu.app.data.KitabuDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KitabuDatabase {
        return KitabuDatabase.getDatabase(context)
    }
}
