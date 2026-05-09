package com.fluxmusic.player;

import com.fluxmusic.player.domain.repository.FavoritesRepository;
import com.fluxmusic.player.playback.MediaSessionConnection;
import com.fluxmusic.player.playback.QueueManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<MediaSessionConnection> mediaSessionConnectionProvider;

  private final Provider<QueueManager> queueManagerProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  public MainActivity_MembersInjector(
      Provider<MediaSessionConnection> mediaSessionConnectionProvider,
      Provider<QueueManager> queueManagerProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider) {
    this.mediaSessionConnectionProvider = mediaSessionConnectionProvider;
    this.queueManagerProvider = queueManagerProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<MediaSessionConnection> mediaSessionConnectionProvider,
      Provider<QueueManager> queueManagerProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider) {
    return new MainActivity_MembersInjector(mediaSessionConnectionProvider, queueManagerProvider, favoritesRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectMediaSessionConnection(instance, mediaSessionConnectionProvider.get());
    injectQueueManager(instance, queueManagerProvider.get());
    injectFavoritesRepository(instance, favoritesRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.fluxmusic.player.MainActivity.mediaSessionConnection")
  public static void injectMediaSessionConnection(MainActivity instance,
      MediaSessionConnection mediaSessionConnection) {
    instance.mediaSessionConnection = mediaSessionConnection;
  }

  @InjectedFieldSignature("com.fluxmusic.player.MainActivity.queueManager")
  public static void injectQueueManager(MainActivity instance, QueueManager queueManager) {
    instance.queueManager = queueManager;
  }

  @InjectedFieldSignature("com.fluxmusic.player.MainActivity.favoritesRepository")
  public static void injectFavoritesRepository(MainActivity instance,
      FavoritesRepository favoritesRepository) {
    instance.favoritesRepository = favoritesRepository;
  }
}
