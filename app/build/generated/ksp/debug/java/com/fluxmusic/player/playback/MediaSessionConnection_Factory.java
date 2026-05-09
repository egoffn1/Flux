package com.fluxmusic.player.playback;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class MediaSessionConnection_Factory implements Factory<MediaSessionConnection> {
  private final Provider<Context> contextProvider;

  public MediaSessionConnection_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MediaSessionConnection get() {
    return newInstance(contextProvider.get());
  }

  public static MediaSessionConnection_Factory create(Provider<Context> contextProvider) {
    return new MediaSessionConnection_Factory(contextProvider);
  }

  public static MediaSessionConnection newInstance(Context context) {
    return new MediaSessionConnection(context);
  }
}
