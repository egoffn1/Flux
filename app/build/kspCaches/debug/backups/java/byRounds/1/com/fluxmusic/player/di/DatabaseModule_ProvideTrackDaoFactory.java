package com.fluxmusic.player.di;

import com.fluxmusic.player.data.local.FluxDatabase;
import com.fluxmusic.player.data.local.dao.TrackDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideTrackDaoFactory implements Factory<TrackDao> {
  private final Provider<FluxDatabase> databaseProvider;

  public DatabaseModule_ProvideTrackDaoFactory(Provider<FluxDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TrackDao get() {
    return provideTrackDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTrackDaoFactory create(
      Provider<FluxDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTrackDaoFactory(databaseProvider);
  }

  public static TrackDao provideTrackDao(FluxDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTrackDao(database));
  }
}
