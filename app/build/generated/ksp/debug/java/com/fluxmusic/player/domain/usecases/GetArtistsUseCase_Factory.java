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
public final class GetArtistsUseCase_Factory implements Factory<GetArtistsUseCase> {
  private final Provider<MusicRepository> repositoryProvider;

  public GetArtistsUseCase_Factory(Provider<MusicRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetArtistsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetArtistsUseCase_Factory create(Provider<MusicRepository> repositoryProvider) {
    return new GetArtistsUseCase_Factory(repositoryProvider);
  }

  public static GetArtistsUseCase newInstance(MusicRepository repository) {
    return new GetArtistsUseCase(repository);
  }
}
