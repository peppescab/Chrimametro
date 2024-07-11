/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.di

import android.content.Context
import ch.zu.chrimametro.SharedPreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSharedPreferenceManager(@ApplicationContext context: Context): SharedPreferenceManager {
        return SharedPreferenceManager(context)
    }
}
