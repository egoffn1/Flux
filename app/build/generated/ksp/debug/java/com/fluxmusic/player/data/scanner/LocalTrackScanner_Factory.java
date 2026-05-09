package com.fluxmusic.player.data.scanner;

import android.content.Context;
import com.fluxmusic.player.data.local.dao.TrackDao;
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
public final class LocalTrackScanner_Factory implements Factory<LocalTrackScanner> {
  private final Provider<Context> contextProvider;

  private final Provider<TrackDao> trackDaoProvider;

  public LocalTrackScanner_Factory(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider) {
    this.contextProvider = contextProvider;
    this.trackDaoProvider = trackDaoProvider;
  }

  @Override
  public LocalTrackScanner get() {
    return newInstance(contextProvider.get(), trackDaoProvider.get());
  }

  public static LocalTrackScanner_Factory create(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider) {
    return new LocalTrackScanner_Factory(contextProvider, trackDaoProvider);
  }

  public static LocalTrackScanner newInstance(Context context, TrackDao trackDao) {
    return new LocalTrackScanner(context, trackDao);
  }
}
