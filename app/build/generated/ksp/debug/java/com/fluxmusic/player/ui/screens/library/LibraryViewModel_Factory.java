package com.fluxmusic.player.ui.screens.library;

import com.fluxmusic.player.domain.repository.FavoritesRepository;
import com.fluxmusic.player.domain.repository.MusicRepository;
import com.fluxmusic.player.domain.usecases.GetAlbumsUseCase;
import com.fluxmusic.player.domain.usecases.GetAllTracksUseCase;
import com.fluxmusic.player.domain.usecases.GetArtistsUseCase;
import com.fluxmusic.player.playback.MediaSessionConnection;
import com.fluxmusic.player.playback.QueueManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<GetAllTracksUseCase> getAllTracksUseCaseProvider;

  private final Provider<GetAlbumsUseCase> getAlbumsUseCaseProvider;

  private final Provider<GetArtistsUseCase> getArtistsUseCaseProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<QueueManager> queueManagerProvider;

  private final Provider<MediaSessionConnection> mediaSessionConnectionProvider;

  public LibraryViewModel_Factory(Provider<GetAllTracksUseCase> getAllTracksUseCaseProvider,
      Provider<GetAlbumsUseCase> getAlbumsUseCaseProvider,
      Provider<GetArtistsUseCase> getArtistsUseCaseProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<QueueManager> queueManagerProvider,
      Provider<MediaSessionConnection> mediaSessionConnectionProvider) {
    this.getAllTracksUseCaseProvider = getAllTracksUseCaseProvider;
    this.getAlbumsUseCaseProvider = getAlbumsUseCaseProvider;
    this.getArtistsUseCaseProvider = getArtistsUseCaseProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.queueManagerProvider = queueManagerProvider;
    this.mediaSessionConnectionProvider = mediaSessionConnectionProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(getAllTracksUseCaseProvider.get(), getAlbumsUseCaseProvider.get(), getArtistsUseCaseProvider.get(), musicRepositoryProvider.get(), favoritesRepositoryProvider.get(), queueManagerProvider.get(), mediaSessionConnectionProvider.get());
  }

  public static LibraryViewModel_Factory create(
      Provider<GetAllTracksUseCase> getAllTracksUseCaseProvider,
      Provider<GetAlbumsUseCase> getAlbumsUseCaseProvider,
      Provider<GetArtistsUseCase> getArtistsUseCaseProvider,
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<QueueManager> queueManagerProvider,
      Provider<MediaSessionConnection> mediaSessionConnectionProvider) {
    return new LibraryViewModel_Factory(getAllTracksUseCaseProvider, getAlbumsUseCaseProvider, getArtistsUseCaseProvider, musicRepositoryProvider, favoritesRepositoryProvider, queueManagerProvider, mediaSessionConnectionProvider);
  }

  public static LibraryViewModel newInstance(GetAllTracksUseCase getAllTracksUseCase,
      GetAlbumsUseCase getAlbumsUseCase, GetArtistsUseCase getArtistsUseCase,
      MusicRepository musicRepository, FavoritesRepository favoritesRepository,
      QueueManager queueManager, MediaSessionConnection mediaSessionConnection) {
    return new LibraryViewModel(getAllTracksUseCase, getAlbumsUseCase, getArtistsUseCase, musicRepository, favoritesRepository, queueManager, mediaSessionConnection);
  }
}
