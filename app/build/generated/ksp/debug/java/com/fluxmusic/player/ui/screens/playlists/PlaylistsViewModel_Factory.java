package com.fluxmusic.player.ui.screens.playlists;

import com.fluxmusic.player.domain.repository.PlaylistRepository;
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
public final class PlaylistsViewModel_Factory implements Factory<PlaylistsViewModel> {
  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  public PlaylistsViewModel_Factory(Provider<PlaylistRepository> playlistRepositoryProvider) {
    this.playlistRepositoryProvider = playlistRepositoryProvider;
  }

  @Override
  public PlaylistsViewModel get() {
    return newInstance(playlistRepositoryProvider.get());
  }

  public static PlaylistsViewModel_Factory create(
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    return new PlaylistsViewModel_Factory(playlistRepositoryProvider);
  }

  public static PlaylistsViewModel newInstance(PlaylistRepository playlistRepository) {
    return new PlaylistsViewModel(playlistRepository);
  }
}
