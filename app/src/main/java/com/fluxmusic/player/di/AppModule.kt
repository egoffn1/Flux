package com.fluxmusic.player.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.fluxmusic.player.data.local.FluxDatabase
import com.fluxmusic.player.data.local.MIGRATION_1_2
import com.fluxmusic.player.data.local.MIGRATION_2_3
import com.fluxmusic.player.data.local.MIGRATION_3_4
import com.fluxmusic.player.data.local.MIGRATION_4_5
import com.fluxmusic.player.data.local.dao.FavoriteDao
import com.fluxmusic.player.data.local.dao.PlaylistDao
import com.fluxmusic.player.data.local.dao.TrackDao
import com.fluxmusic.player.data.repository.FavoritesRepositoryImpl
import com.fluxmusic.player.data.repository.MusicRepositoryImpl
import com.fluxmusic.player.data.repository.PlaylistRepositoryImpl
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.domain.repository.PlaylistRepository
import dagger.Binds
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
    fun provideDatabase(@ApplicationContext context: Context): FluxDatabase {
        return Room.databaseBuilder(
            context,
            FluxDatabase::class.java,
            "flux_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: FluxDatabase): TrackDao = database.trackDao()

    @Provides
    @Singleton
    fun providePlaylistDao(database: FluxDatabase): PlaylistDao = database.playlistDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: FluxDatabase): FavoriteDao = database.favoriteDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}