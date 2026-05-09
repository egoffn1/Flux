package com.fluxmusic.player.data.repository;

import com.fluxmusic.player.data.local.dao.FavoriteDao;
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
public final class FavoritesRepositoryImpl_Factory implements Factory<FavoritesRepositoryImpl> {
  private final Provider<FavoriteDao> favoriteDaoProvider;

  public FavoritesRepositoryImpl_Factory(Provider<FavoriteDao> favoriteDaoProvider) {
    this.favoriteDaoProvider = favoriteDaoProvider;
  }

  @Override
  public FavoritesRepositoryImpl get() {
    return newInstance(favoriteDaoProvider.get());
  }

  public static FavoritesRepositoryImpl_Factory create(Provider<FavoriteDao> favoriteDaoProvider) {
    return new FavoritesRepositoryImpl_Factory(favoriteDaoProvider);
  }

  public static FavoritesRepositoryImpl newInstance(FavoriteDao favoriteDao) {
    return new FavoritesRepositoryImpl(favoriteDao);
  }
}
