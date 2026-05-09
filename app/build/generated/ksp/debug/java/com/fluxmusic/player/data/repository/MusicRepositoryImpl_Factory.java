package com.fluxmusic.player.data.repository;

import com.fluxmusic.player.data.local.dao.TrackDao;
import com.fluxmusic.player.data.scanner.LocalTrackScanner;
import com.fluxmusic.player.data.scanner.MediaScanner;
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
public final class MusicRepositoryImpl_Factory implements Factory<MusicRepositoryImpl> {
  private final Provider<TrackDao> trackDaoProvider;

  private final Provider<MediaScanner> mediaScannerProvider;

  private final Provider<LocalTrackScanner> localTrackScannerProvider;

  public MusicRepositoryImpl_Factory(Provider<TrackDao> trackDaoProvider,
      Provider<MediaScanner> mediaScannerProvider,
      Provider<LocalTrackScanner> localTrackScannerProvider) {
    this.trackDaoProvider = trackDaoProvider;
    this.mediaScannerProvider = mediaScannerProvider;
    this.localTrackScannerProvider = localTrackScannerProvider;
  }

  @Override
  public MusicRepositoryImpl get() {
    return newInstance(trackDaoProvider.get(), mediaScannerProvider.get(), localTrackScannerProvider.get());
  }

  public static MusicRepositoryImpl_Factory create(Provider<TrackDao> trackDaoProvider,
      Provider<MediaScanner> mediaScannerProvider,
      Provider<LocalTrackScanner> localTrackScannerProvider) {
    return new MusicRepositoryImpl_Factory(trackDaoProvider, mediaScannerProvider, localTrackScannerProvider);
  }

  public static MusicRepositoryImpl newInstance(TrackDao trackDao, MediaScanner mediaScanner,
      LocalTrackScanner localTrackScanner) {
    return new MusicRepositoryImpl(trackDao, mediaScanner, localTrackScanner);
  }
}
