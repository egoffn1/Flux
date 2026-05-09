package com.fluxmusic.player.ui.screens.search;

import com.fluxmusic.player.domain.repository.FavoritesRepository;
import com.fluxmusic.player.domain.usecases.SearchTracksUseCase;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<SearchTracksUseCase> searchTracksUseCaseProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<QueueManager> queueManagerProvider;

  private final Provider<MediaSessionConnection> mediaSessionConnectionProvider;

  public SearchViewModel_Factory(Provider<SearchTracksUseCase> searchTracksUseCaseProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<QueueManager> queueManagerProvider,
      Provider<MediaSessionConnection> mediaSessionConnectionProvider) {
    this.searchTracksUseCaseProvider = searchTracksUseCaseProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.queueManagerProvider = queueManagerProvider;
    this.mediaSessionConnectionProvider = mediaSessionConnectionProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(searchTracksUseCaseProvider.get(), favoritesRepositoryProvider.get(), queueManagerProvider.get(), mediaSessionConnectionProvider.get());
  }

  public static SearchViewModel_Factory create(
      Provider<SearchTracksUseCase> searchTracksUseCaseProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<QueueManager> queueManagerProvider,
      Provider<MediaSessionConnection> mediaSessionConnectionProvider) {
    return new SearchViewModel_Factory(searchTracksUseCaseProvider, favoritesRepositoryProvider, queueManagerProvider, mediaSessionConnectionProvider);
  }

  public static SearchViewModel newInstance(SearchTracksUseCase searchTracksUseCase,
      FavoritesRepository favoritesRepository, QueueManager queueManager,
      MediaSessionConnection mediaSessionConnection) {
    return new SearchViewModel(searchTracksUseCase, favoritesRepository, queueManager, mediaSessionConnection);
  }
}
