package com.fluxmusic.player.domain.usecases;

import com.fluxmusic.player.domain.repository.MusicRepository;
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
public final class SearchTracksUseCase_Factory implements Factory<SearchTracksUseCase> {
  private final Provider<MusicRepository> repositoryProvider;

  public SearchTracksUseCase_Factory(Provider<MusicRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SearchTracksUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SearchTracksUseCase_Factory create(Provider<MusicRepository> repositoryProvider) {
    return new SearchTracksUseCase_Factory(repositoryProvider);
  }

  public static SearchTracksUseCase newInstance(MusicRepository repository) {
    return new SearchTracksUseCase(repository);
  }
}
