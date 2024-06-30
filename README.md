Star Wars Reference App

This project is a test assignment for Avelios Medical,
written by Maksim Omel'chenko in 2024. https://github.com/OmelchenkoMaksim

It aims to create a reference application for Star Wars characters, starships, and planets.
The app leverages modern Android development tools and libraries
including Jetpack Compose, Apollo GraphQL, Room, Koin, and Coil.

Features

    Display Characters, Starships, and Planets: Browse through 
    a list of characters, starships, and planets from the Star Wars universe.
    GraphQL Integration: Fetch data using GraphQL queries.
    Offline Support: Store data locally using Room and load it when there's no internet connection.
    Dependency Injection: Utilize Koin for dependency injection.
    Image Loading: Load images using Coil.

Tech Stack

    Kotlin
    Jetpack Compose: For building the UI.
    Apollo GraphQL: For querying the GraphQL API.
    Room: For local data storage.
    Koin: For dependency injection.
    Coil: For loading images.
    OkHttp: For networking.

Code Structure

    MainActivity.kt: Entry point of the app, sets up the main UI and initializes the ViewModel.
    CharactersViewModel.kt: Handles data fetching and state management for characters, starships, and planets.
    data/model/: Contains data classes for characters, starships, and planets.
    data/local/: Contains DAO and database classes for local storage.
    network/: Contains Apollo GraphQL setup and queries.
    ui/: Contains composables for UI components.

Configuration
Gradle Configuration

    gradle.properties: Configurations like JVM args, use of AndroidX, Kotlin code style, and incremental build settings.
    build.gradle.kts: Module-level Gradle configurations including dependencies, plugins, and compile options.
    libs.versions.toml: Manages versions of dependencies in a centralized manner.

GraphQL Schema

    graphql/: Contains the schema and query files for Apollo GraphQL.

Dependency Injection with Koin

    App.kt: Application class where Koin is initialized and network connectivity listener is set up.

Network Connectivity Listener

The app includes a network connectivity listener to show a toast message when
the internet connection is established or lost. It also triggers data fetch from
the server when connectivity is restored and no data is available locally.

Since this is a test assignment, contributions are not expected.
However, if you'd like to suggest improvements or report issues, feel free to open an issue or a pull request.

License
This project is licensed under the MIT License.

This project is a test assignment for Avelios Medical.

Thank you for reviewing our code!