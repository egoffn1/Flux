# Flux Music Player

[![CI](https://github.com/egoffn1/Flux/actions/workflows/ci.yml/badge.svg)](https://github.com/egoffn1/Flux/actions/workflows/ci.yml)
[![Release](https://github.com/egoffn1/Flux/actions/workflows/release.yml/badge.svg)](https://github.com/egoffn1/Flux/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-26%2B-blue)](https://developer.android.com/about/versions/14)

Flux — минималистичный локальный музыкальный плеер в стиле Material You для Android. Приложение полностью офлайн, не содержит рекламы и не собирает данные о пользователях.

## Особенности

- **Material You дизайн** — интерфейс динамически подстраивается под цвета обоев устройства
- **Полностью офлайн** — никаких сетевых запросов, ваша музыка остается локальной
- **Без рекламы и сбора данных** — приватность превыше всего
- **Поддержка Bluetooth и Android Auto** — наслаждайтесь музыкой в автомобиле и наушниках
- **Умная очередь** — shuffle и repeat режимы
- **Избранное** — добавляйте любимые треки
- **Плейлисты** — создавайте и управляйте своими плейлистами
- **Поиск** — быстрый поиск по трекам, альбомам и исполнителям

## Технологический стек

- **Язык**: 100% Kotlin
- **UI**: Jetpack Compose + Material 3
- **Архитектура**: Clean Architecture + MVVM
- **Мультимедиа**: Media3 (ExoPlayer + MediaSessionService)
- **Локальное хранилище**: Room + DataStore
- **Инъекция зависимостей**: Dagger Hilt
- **Загрузка изображений**: Coil
- **Адаптивные цвета**: Palette API

## Требования

- Android 6.0 (API 26) и выше
- Android Studio Arctic Fox или новее

## Сборка

### Предварительные требования

1. Установите Android Studio
2. Установите JDK 17
3. Клонируйте репозиторий

### Локальная сборка

```bash
# Клонирование репозитория
git clone https://github.com/egoffn1/Flux.git
cd Flux

# Сборка debug APK
./gradlew assembleDebug

# Сборка release APK (требуется настройка подписи)
./gradlew assembleRelease
```

### Настройка подписи для release сборки

Создайте файл `local.properties` в корне проекта:

```properties
sdk.dir=/path/to/android/sdk
keystore.file=path/to/your/keystore.jks
```

Или используйте переменные окружения:

```bash
export KEYSTORE_FILE=/path/to/keystore.jks
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=your_alias
export KEY_PASSWORD=your_key_password
```

## CI/CD

Проект использует GitHub Actions для автоматизации:

### CI (Continuous Integration)

- Запускается при push в main и создании Pull Request
- Выполняет: lint, unit тесты, сборку debug APK
- Загружает APK как артефакт

### CD (Continuous Deployment)

- Запускается при создании тега с префиксом `v*` (например, `v1.0.0`)
- Собирает signed AAB и APK
- Создает GitHub Release с артефактами

### Настройка секретов для release сборки

В настройках репозитория добавьте следующие secrets:

- `KEYSTORE_BASE64` — Keystore в base64 encoding
- `KEYSTORE_PASSWORD` — Пароль от keystore
- `KEY_ALIAS` — Alias ключа
- `KEY_PASSWORD` — Пароль ключа

Генерация base64 keystore:
```bash
base64 -w 0 keystore.jks
```

## Архитектура

```
app/src/main/java/com/fluxmusic/player/
├── data/                    # Data layer
│   ├── local/              # Room database, DAOs, entities
│   ├── repository/         # Repository implementations
│   └── scanner/            # MediaStore scanner
├── domain/                  # Domain layer
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecases/           # Use cases
├── ui/                      # Presentation layer
│   ├── components/         # Reusable Compose components
│   ├── navigation/         # Navigation setup
│   ├── screens/           # Screen composables + ViewModels
│   └── theme/             # Material 3 theming
├── playback/               # Music playback
│   ├── MusicService       # MediaSessionService
│   ├── QueueManager       # Queue management
│   └── MediaSessionConnection
└── di/                      # Hilt modules
```

## Лицензия

MIT License — подробности в файле [LICENSE](LICENSE)

## Contributing

1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/amazing-feature`)
3. Commit ваши изменения (`git commit -m 'Add some amazing feature'`)
4. Push в branch (`git push origin feature/amazing-feature`)
5. Откройте Pull Request