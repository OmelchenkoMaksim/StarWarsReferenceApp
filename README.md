Star Wars Reference App

This project is a test assignment for Avelios Medical, written by Maksim Omel'chenko in 2024.
https://github.com/OmelchenkoMaksim

Overview

The Star Wars Reference App is designed to provide a comprehensive reference for Star Wars characters, starships, and planets.
The app utilizes modern Android development tools and libraries to deliver a seamless user experience,
including Jetpack Compose, Apollo GraphQL, Room, Koin.

Features

    Display Characters, Starships, and Planets: Browse through a detailed list of characters, starships, and planets from the Star Wars universe.
    GraphQL Integration: Fetch data efficiently using GraphQL queries.
    Offline Support: Store data locally using Room and access it even when offline.
    Dependency Injection: Utilize Koin for efficient dependency injection.
    Paging: Efficiently load and display paginated data.
    Theme and Typography Customization: Customize themes and typography variants through the app's settings.

Tech Stack

    Kotlin
    Jetpack Compose: For building a modern and responsive UI.
    Apollo GraphQL: For querying the GraphQL API.
    Room: For local data storage.
    Koin: For dependency injection.
    Timber: For logging.

Code Structure

    MainActivity.kt: Entry point of the app, sets up the main UI and initializes the ViewModel.
    MainViewModel.kt: Handles data fetching and state management for characters, starships, and planets.
    data/model/: Contains data classes for characters, starships, and planets.
    data/local/: Contains DAO and database classes for local storage.
    network/: Contains Apollo GraphQL setup and queries.
    ui/: Contains composables for UI components.
    repository/: Handles data operations and integrates local and remote data sources.

Configuration
Gradle Configuration

    gradle.properties: Configurations like JVM args, use of AndroidX, Kotlin code style, and incremental build settings.
    app/build.gradle.kts: Module-level Gradle configurations including dependencies, plugins, and compile options.
    gradle/libs.versions.toml: Manages versions of dependencies in a centralized manner.
    settings.gradle.kts: Configures plugin management and repositories.

GraphQL Schema

    graphql/: Contains the schema and query files for Apollo GraphQL.

Dependency Injection with Koin

    App.kt: Application class where Koin is initialized and network connectivity listener is set up.
    modules/: Defines Koin modules for providing dependencies.

Network Connectivity Listener

The app includes a network connectivity listener to show a toast message when the internet connection is established or lost. It also triggers data
fetch from the server when connectivity is restored and no data is available locally.
License

This project is licensed under the MIT License.

Thank you for reviewing our code! Feel free to reach out if you have any questions or suggestions.