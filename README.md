# LadyBugOS Note-Taking App

A modern, feature-rich note-taking application for Android, built with Kotlin and Jetpack Compose.

## Features

- **Home Screen**: View, search, and filter all your notes.
- **Note Management**: Create, edit, and organize notes. Supports standard notes and checklists.
- **Organization**:
    - **Archive**: Keep your main notes list clean by archiving old notes.
    - **Trash**: Deleted notes are moved to the trash for easy recovery.
    - **Labels**: Organize notes with custom labels.
    - **Reminders**: Set reminders for your notes.
- **Customization**: Personalize notes with different colors.
- **Backup & Restore**: Simple backup and restore functionality for your data.
- **Settings**: Configure application settings and view open-source licenses.
- **Widgets**: Access your notes from the home screen with app widgets.

## Technology Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**:
    - [Jetpack Compose](https://developer.android.com/jetpack/compose) for building the UI.
    - [Material 3](https://m3.material.io/) for modern UI components.
    - [Glance](https://developer.android.com/jetpack/androidx/releases/glance) for creating home screen widgets.
    - [Coil](https://coil-kt.github.io/coil/) for image loading.
- **Architecture**:
    - [Model-View-ViewModel (MVVM)](https://developer.android.com/jetpack/guide)
    - [Koin](https://insert-koin.io/) for dependency injection.
    - [Jetpack Navigation](https://developer.android.com/guide/navigation) for navigating between screens.
- **Asynchronous Programming**:
    - [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and Flow for managing background threads.
- **Data Persistence**:
    - [Room](https://developer.android.com/training/data-storage/room) for the local database.
    - [Retrofit](https://square.github.io/retrofit/) for handling network requests.
    - [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for storing key-value data.
- **Background Processing**:
    - [Jetpack WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for deferrable background tasks.
- **Build System**:
    - [Gradle](https://gradle.org/) with Kotlin DSL.

## Project Structure

The project follows a multi-module architecture to separate concerns and improve maintainability.

- `app`: The main application module that integrates all other modules.
- `core`: Contains shared code and utilities used across multiple modules.
    - `common`: Common utilities and extensions.
    - `data`: Repositories and data sources.
    - `database`: Room database definitions and DAOs.
    - `datastore`: DataStore preferences.
    - `design-system`: Shared Compose components and themes.
    - `domain`: Use cases and business logic.
    - `model`: Data models used throughout the app.
- `feature`: Contains individual feature modules, each with its own UI and logic.
    - `home`: The main screen with the note list.
    - `detail`: The screen for viewing and editing a note.
    - `backup`: The backup and restore feature.
    - `settings`: The settings screen.
- `notifications`: Handles system notifications.
- `widgets`: Implements home screen widgets.
- `build-logic`: Contains custom Gradle conventions for the project.

## How to Build

1. Clone the repository.
2. Open the project in Android Studio.
3. Build the project using the Gradle wrapper:
   ```bash
   ./gradlew build
   ```
