package com.fluxmusic.player.playback;

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
public final class MusicService_MembersInjector implements MembersInjector<MusicService> {
  private final Provider<QueueManager> queueManagerProvider;

  public MusicService_MembersInjector(Provider<QueueManager> queueManagerProvider) {
    this.queueManagerProvider = queueManagerProvider;
  }

  public static MembersInjector<MusicService> create(Provider<QueueManager> queueManagerProvider) {
    return new MusicService_MembersInjector(queueManagerProvider);
  }

  @Override
  public void injectMembers(MusicService instance) {
    injectQueueManager(instance, queueManagerProvider.get());
  }

  @InjectedFieldSignature("com.fluxmusic.player.playback.MusicService.queueManager")
  public static void injectQueueManager(MusicService instance, QueueManager queueManager) {
    instance.queueManager = queueManager;
  }
}
