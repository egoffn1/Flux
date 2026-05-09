package com.fluxmusic.player.data.scanner;

import android.content.ContentResolver;
import com.fluxmusic.player.data.local.dao.TrackDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class MediaScanner_Factory implements Factory<MediaScanner> {
  private final Provider<ContentResolver> contentResolverProvider;

  private final Provider<TrackDao> trackDaoProvider;

  public MediaScanner_Factory(Provider<ContentResolver> contentResolverProvider,
      Provider<TrackDao> trackDaoProvider) {
    this.contentResolverProvider = contentResolverProvider;
    this.trackDaoProvider = trackDaoProvider;
  }

  @Override
  public MediaScanner get() {
    return newInstance(contentResolverProvider.get(), trackDaoProvider.get());
  }

  public static MediaScanner_Factory create(Provider<ContentResolver> contentResolverProvider,
      Provider<TrackDao> trackDaoProvider) {
    return new MediaScanner_Factory(contentResolverProvider, trackDaoProvider);
  }

  public static MediaScanner newInstance(ContentResolver contentResolver, TrackDao trackDao) {
    return new MediaScanner(contentResolver, trackDao);
  }
}
