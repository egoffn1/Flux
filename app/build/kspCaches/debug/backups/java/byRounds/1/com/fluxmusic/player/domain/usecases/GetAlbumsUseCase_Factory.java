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
public final class GetAlbumsUseCase_Factory implements Factory<GetAlbumsUseCase> {
  private final Provider<MusicRepository> repositoryProvider;

  public GetAlbumsUseCase_Factory(Provider<MusicRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetAlbumsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetAlbumsUseCase_Factory create(Provider<MusicRepository> repositoryProvider) {
    return new GetAlbumsUseCase_Factory(repositoryProvider);
  }

  public static GetAlbumsUseCase newInstance(MusicRepository repository) {
    return new GetAlbumsUseCase(repository);
  }
}
