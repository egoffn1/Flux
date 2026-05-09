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
public final class GetAllTracksUseCase_Factory implements Factory<GetAllTracksUseCase> {
  private final Provider<MusicRepository> repositoryProvider;

  public GetAllTracksUseCase_Factory(Provider<MusicRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetAllTracksUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetAllTracksUseCase_Factory create(Provider<MusicRepository> repositoryProvider) {
    return new GetAllTracksUseCase_Factory(repositoryProvider);
  }

  public static GetAllTracksUseCase newInstance(MusicRepository repository) {
    return new GetAllTracksUseCase(repository);
  }
}
