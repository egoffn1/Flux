<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Flux Logo" width="100" />
  <h1>Flux Music Player</h1>
  <p><em>Музыка течёт. Цвета дышат.</em></p>
  
  [![Platform](https://img.shields.io/badge/Platform-Android-brightgreen?style=flat-square)](https://developer.android.com)
  [![Minimum SDK](https://img.shields.io/badge/Min%20SDK-26-orange?style=flat-square)](https://developer.android.com/tools/releases/platforms)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blueviolet?style=flat-square)](https://kotlinlang.org)
  [![Compose](https://img.shields.io/badge/Jetpack%20Compose-latest-blue?style=flat-square)](https://developer.android.com/jetpack/compose)
  [![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey?style=flat-square)](LICENSE)
</div>

---

## 📀 О проекте

**Flux** — это минималистичный офлайн-музыкальный плеер, вдохновлённый эстетикой Google.  
Здесь нет рекламы, сбора данных и лишних элементов. Только ваша музыка и интерфейс, который подстраивается под **вас**.

Flux полностью переосмысливает локальное прослушивание:  
🎨 Динамическая тема Material You окрашивает плеер в цвета ваших обоев  
🌀 Анимации создают ощущение живого звукового потока  
🎧 Современный аудиодвижок на базе Media3 обеспечивает идеальное воспроизведение и управление

Приложение задумывалось как open-source альтернатива коммерческим плеерам, с упором на чистоту кода, архитектуру и дизайн.

---

## ✨ Возможности

- **🎵 Полноценное локальное прослушивание**  
  Сканирование всех аудиофайлов на устройстве через MediaStore. Поддержка ALAC, FLAC, MP3, AAC и др.

- **🔥 Material You (Monet)**  
  Цветовая схема автоматически меняется вместе с обоями (Android 12+). Идеально сочетается со светлой и тёмной темами.

- **🎨 «Сейчас играет» как произведение искусства**  
  Размытый фон по цветам обложки альбома (Palette API), плавающая анимация винила, мягкие переходы между треками.

- **📱 Адаптивный интерфейс**  
  Мини-плеер снизу → полноэкранный режим с жестом swipe. Плавные shared-анимации Jetpack Compose.

- **📂 Управление библиотекой**  
  Вкладки: Треки, Альбомы, Исполнители, Плейлисты. Мгновенный поиск, сортировки.

- **💾 Плейлисты и Избранное**  
  Создавайте/редактируйте списки воспроизведения, перетаскивайте треки (drag-and-drop). Любимые песни всегда под рукой.

- **🔄 Умная очередь**  
  Контекстная очередь (игра из альбома, исполнителя), режимы Shuffle и Repeat (один/все/выкл).

- **🎧 Полноценное фоновое управление**  
  MediaSessionService для уведомлений, поддержки Bluetooth/Android Auto/Wear OS, гарнитур.

- **📦 Офлайн и без слежки**  
  Никакого интернета, аналитики или рекламы. Вся библиотека и предпочтения хранятся локально (Room, DataStore).

---

## 🛠️ Технологический стек

| Компонент          | Решение                         |
|-------------------|----------------------------------|
| Язык              | 100% Kotlin                     |
| UI                | Jetpack Compose (Material 3)    |
| Архитектура       | Clean Architecture + MVVM        |
| Воспроизведение   | Media3 (ExoPlayer) + MediaSession |
| База данных        | Room (треки, плейлисты)         |
| Локальные настройки| DataStore                       |
| DI                | Dagger Hilt                     |
| Асинхронность     | Kotlin Coroutines + Flow        |
| Анимации          | SharedTransitionLayout, Compose Animation |
| Обложки/Кеш       | Coil + Palette API              |
| Сканирование      | WorkManager (периодическая синхронизация MediaStore) |

Весь код строго разделён на слои **data**, **domain**, **ui**. Пакетная структура оптимизирована под масштабирование.

---

## 📸 Скриншоты

> _Добавь сюда реальные скриншоты, когда сделаешь первые билды. Рекомендуемые ракурсы:_
> - Главный экран библиотеки (светлая тема, динамические цвета)
> - Экран «Сейчас играет» с размытым фоном и винил-анимацией
> - Мини-плеер внизу экрана
> - Управление плейлистами и drag‑and‑drop
> - Уведомление медиаплеера

<div align="center">
  <img src="screenshots/library.png" width="24%" alt="Библиотека" />
  <img src="screenshots/nowplaying.png" width="24%" alt="Плеер" />
  <img src="screenshots/miniplayer.png" width="24%" alt="Мини-плеер" />
  <img src="screenshots/playlists.png" width="24%" alt="Плейлисты" />
</div>

---

## 🚀 Как собрать и запустить

1. **Клонируйте репозиторий**
   ```bash
   git clone https://github.com/your-username/Flux-Music-Player.git
   cd Flux-Music-Player
   ```

2. **Откройте в Android Studio Hedgehog (2023.1.1) или новее**

3. **Синхронизируйте Gradle** и дайте время на загрузку зависимостей.

4. **Запустите на устройстве/эмуляторе**
   - Минимальный SDK: 26 (Android 8.0)
   - Рекомендуемый: Android 12+ для полного эффекта Material You

5. **Сборка APK / App Bundle**
   ```bash
   ./gradlew assembleRelease
   ```
   Или через Android Studio: `Build` → `Generate Signed Bundle / APK`.

> **Примечание:** Для корректной работы необходимо предоставить разрешение на чтение аудио (`READ_MEDIA_AUDIO` для Android 13+).

---

## 🤝 Хочу участвовать в разработке

Flux — полностью открытый проект. Я буду рад вашим идеям и пулл‑реквестам!

1. Делайте форк репозитория.
2. Создайте новую ветку: `feature/ваша-фича` или `fix/название-бага`.
3. Внесите изменения и отправьте PR.

Пожалуйста, придерживайтесь общей архитектуры проекта и стиля кода. Желательно обсудить крупные изменения в [Issues](https://github.com/your-username/Flux-Music-Player/issues) заранее.

---

## 📄 Лицензия

```
Copyright 2025 Flux Music Player

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## ⭐ Поддержи проект

Если тебе нравится Flux, поставь звёздочку на GitHub ⭐ — это лучшая мотивация для дальнейшего развития!

Можно также рассказать о плеере в соцсетях или поделиться скриншотом в теме «Material You».

---

<div align="center">
  <sub>Создано с любовью к музыке и Material Design.</sub>
</div>
