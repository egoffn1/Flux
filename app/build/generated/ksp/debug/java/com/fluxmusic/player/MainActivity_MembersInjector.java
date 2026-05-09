package com.fluxmusic.player;

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

  public MainActivity_MembersInjector(
      Provider<MediaSessionConnection> mediaSessionConnectionProvider,
      Provider<QueueManager> queueManagerProvider) {
    this.mediaSessionConnectionProvider = mediaSessionConnectionProvider;
    this.queueManagerProvider = queueManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<MediaSessionConnection> mediaSessionConnectionProvider,
      Provider<QueueManager> queueManagerProvider) {
    return new MainActivity_MembersInjector(mediaSessionConnectionProvider, queueManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectMediaSessionConnection(instance, mediaSessionConnectionProvider.get());
    injectQueueManager(instance, queueManagerProvider.get());
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
}
