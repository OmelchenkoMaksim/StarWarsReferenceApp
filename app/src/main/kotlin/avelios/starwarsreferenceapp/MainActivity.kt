package avelios.starwarsreferenceapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import avelios.starwarsreferenceapp.ui.theme.StarWarsReferenceAppTheme
import avelios.starwarsreferenceapp.ui.theme.ThemeVariant
import avelios.starwarsreferenceapp.ui.theme.TypographyVariant
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.network.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModel()
    private val settingsManager: SettingsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDarkMode = settingsManager.isDarkMode()
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContent {
            val savedThemeVariant = settingsManager.loadThemeVariant()
            val savedTypographyVariant = settingsManager.loadTypographyVariant()

            val themeVariant = remember { mutableStateOf(savedThemeVariant) }
            val typographyVariant = remember { mutableStateOf(savedTypographyVariant) }
            val isDarkTheme = remember { mutableStateOf(isDarkMode) }
            val showSettingsDialog = remember { mutableStateOf(false) }

            if (showSettingsDialog.value) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog.value = false },
                    themeVariant = themeVariant,
                    typographyVariant = typographyVariant
                ) { newThemeVariant, newTypographyVariant ->
                    settingsManager.saveThemeVariant(newThemeVariant)
                    settingsManager.saveTypographyVariant(newTypographyVariant)
                    themeVariant.value = newThemeVariant
                    typographyVariant.value = newTypographyVariant
                }
            }

            StarWarsReferenceAppTheme(
                themeVariant = themeVariant.value,
                typographyVariant = typographyVariant.value,
                darkTheme = isDarkTheme.value
            ) {
                MainScreen(
                    viewModel = mainViewModel,
                    showSettingsDialog = { showSettingsDialog.value = true },
                    toggleTheme = {
                        isDarkTheme.value = !isDarkTheme.value
                        settingsManager.setDarkMode(isDarkTheme.value)
                        AppCompatDelegate.setDefaultNightMode(
                            if (isDarkTheme.value) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    showSettingsDialog: () -> Unit,
    toggleTheme: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is MainState.Loading -> {
            CircularProgressIndicator()
        }

        is MainState.DataLoaded -> {
            DataScreen(
                onFavoriteClick = { id, isFavorite ->
                    viewModel.handleIntent(MainIntent.UpdateFavoriteStatus(id, isFavorite))
                },
                showSettingsDialog = showSettingsDialog,
                toggleTheme = toggleTheme
            )
        }

        is MainState.Error -> {
            Text(text = "Error: ${(state as MainState.Error).message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onFavoriteClick: (String, Boolean) -> Unit,
    showSettingsDialog: () -> Unit,
    toggleTheme: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Star Wars APP") },
                actions = {
                    IconButton(onClick = showSettingsDialog) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "Settings")
                    }
                    IconButton(onClick = toggleTheme) {
                        Image(painter = painterResource(id = R.drawable.ic_day_night), contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(navController, startDestination = NavigationItem.Characters.route, modifier = Modifier.padding(paddingValues)) {
            composable(NavigationItem.Characters.route) {
                CharactersScreen(
                    onCharacterClick = { characterId ->
                        navController.navigate("character_details/$characterId")
                    },
                    onFavoriteClick = onFavoriteClick
                )
            }
            composable(NavigationItem.Starships.route) {
                val viewModel: MainViewModel = koinViewModel()
                val state by viewModel.state.collectAsState()
                if (state is MainState.DataLoaded) {
                    StarshipsScreen(
                        onStarshipClick = { starshipId ->
                            navController.navigate("starship_details/$starshipId")
                        }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            composable(NavigationItem.Planets.route) {
                val viewModel: MainViewModel = koinViewModel()
                val state by viewModel.state.collectAsState()
                if (state is MainState.DataLoaded) {
                    PlanetsScreen(
                        onPlanetClick = { planetId ->
                            navController.navigate("planet_details/$planetId")
                        }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            composable("character_details/{characterId}") { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: return@composable
                CharacterDetailsScreen(characterId = characterId)
            }
            composable("starship_details/{starshipId}") { backStackEntry ->
                val starshipId = backStackEntry.arguments?.getString("starshipId") ?: return@composable
                StarshipDetailsScreen(starshipId = starshipId)
            }
            composable("planet_details/{planetId}") { backStackEntry ->
                val planetId = backStackEntry.arguments?.getString("planetId") ?: return@composable
                PlanetDetailsScreen(planetId = planetId)
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    themeVariant: MutableState<ThemeVariant>,
    typographyVariant: MutableState<TypographyVariant>,
    onSaveSettings: (ThemeVariant, TypographyVariant) -> Unit
) {
    val themeExpanded = remember { mutableStateOf(false) }
    val typographyExpanded = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Button(onClick = { themeExpanded.value = true }) {
                    OutlinedText("Select Theme (${themeVariant.value.name})")
                }
                DropdownMenu(
                    expanded = themeExpanded.value,
                    onDismissRequest = { themeExpanded.value = false }
                ) {
                    ThemeVariant.entries.forEach { variant ->
                        DropdownMenuItem(onClick = {
                            themeVariant.value = variant
                            themeExpanded.value = false
                            onSaveSettings(themeVariant.value, typographyVariant.value)
                        }, text = {
                            Text(variant.name)
                        })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { typographyExpanded.value = true }) {
                    OutlinedText("Select Typography (${typographyVariant.value.name})")
                }
                DropdownMenu(
                    expanded = typographyExpanded.value,
                    onDismissRequest = { typographyExpanded.value = false }
                ) {
                    TypographyVariant.entries.forEach { variant ->
                        DropdownMenuItem(onClick = {
                            typographyVariant.value = variant
                            typographyExpanded.value = false
                            onSaveSettings(themeVariant.value, typographyVariant.value)
                        }, text = {
                            Text(variant.name)
                        })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    style = typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        shadow = Shadow(
                            color = MaterialTheme.colorScheme.inverseSurface,
                            offset = Offset(2f, 2f),
                            blurRadius = 2f
                        )
                    )
                )
            }
        }
    )
}

@Composable
fun OutlinedText(text: String) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Text(
        text = text,
        style = typography.bodyMedium.copy(
            color = colors.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = colors.background,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        ),
        modifier = Modifier
            .padding(8.dp)
    )
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem.Characters,
        NavigationItem.Starships,
        NavigationItem.Planets
    )
    NavigationBar {
        val currentRoute = currentRoute(navController)
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Composable
fun CharactersScreen(
    onCharacterClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit
) {
    val viewModel: MainViewModel = koinViewModel()
    val lazyPagingItems: LazyPagingItems<StarWarsCharacter> = viewModel.charactersPager.collectAsLazyPagingItems()

    LazyColumn {
        items(lazyPagingItems.itemCount) { index ->
            val character = lazyPagingItems[index]
            character?.let {
                CharacterItem(character = it, onClick = { onCharacterClick(it.id) }) {
                    onFavoriteClick(it.id, !it.isFavorite)
                }
            }
        }

        lazyPagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }

                loadState.append is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }

                loadState.refresh is LoadState.Error -> {
                    val e = lazyPagingItems.loadState.refresh as LoadState.Error
                    item { Text(text = "Error: ${e.error.localizedMessage}") }
                }

                loadState.append is LoadState.Error -> {
                    val e = lazyPagingItems.loadState.append as LoadState.Error
                    item { Text(text = "Error: ${e.error.localizedMessage}") }
                }
            }
        }
    }
}

@Composable
fun StarshipsScreen(
    onStarshipClick: (String) -> Unit
) {
    val viewModel: MainViewModel = koinViewModel()
    val lazyPagingItems: LazyPagingItems<Starship> = viewModel.starshipsPager.collectAsLazyPagingItems()

    LazyColumn {
        items(lazyPagingItems.itemCount) { index ->
            val starship = lazyPagingItems[index]
            starship?.let {
                StarshipItem(starship = it, onClick = { onStarshipClick(it.id) })
            }
        }

        lazyPagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }

                loadState.append is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }

                loadState.refresh is LoadState.Error -> {
                    val e = lazyPagingItems.loadState.refresh as LoadState.Error
                    item { Text(text = "Error: ${e.error.localizedMessage}") }
                }

                loadState.append is LoadState.Error -> {
                    val e = lazyPagingItems.loadState.append as LoadState.Error
                    item { Text(text = "Error: ${e.error.localizedMessage}") }
                }
            }
        }
    }
}

@Composable
fun PlanetsScreen(
    onPlanetClick: (String) -> Unit
) {
    val viewModel: MainViewModel = koinViewModel()
    val lazyPagingItems: LazyPagingItems<Planet> = viewModel.planetsPager.collectAsLazyPagingItems()

    LazyColumn {
        items(lazyPagingItems.itemCount) { index ->
            val planet = lazyPagingItems[index]
            planet?.let {
                PlanetItem(planet = it, onClick = { onPlanetClick(it.id) })
            }
        }

        lazyPagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }

                loadState.append is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }

                loadState.refresh is LoadState.Error -> {
                    val e = lazyPagingItems.loadState.refresh as LoadState.Error
                    item { Text(text = "Error: ${e.error.localizedMessage}") }
                }

                loadState.append is LoadState.Error -> {
                    val e = lazyPagingItems.loadState.append as LoadState.Error
                    item { Text(text = "Error: ${e.error.localizedMessage}") }
                }
            }
        }
    }
}

@Composable
fun CharacterDetailsScreen(characterId: String) {
    val viewModel: MainViewModel = koinViewModel()
    val character by viewModel.selectedCharacter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(characterId) {
        viewModel.handleIntent(MainIntent.FetchCharacterDetails(characterId))
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        character?.let { characterDetails: StarWarsCharacter ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${characterDetails.name}", style = typography.bodyLarge)
                Text(text = "Birth Year: ${characterDetails.birthYear}", style = typography.bodyMedium)
                Text(text = "Eye Color: ${characterDetails.eyeColor}", style = typography.bodyMedium)
                Text(text = "Gender: ${characterDetails.gender}", style = typography.bodyMedium)
                Text(text = "Hair Color: ${characterDetails.hairColor}", style = typography.bodyMedium)
                Text(text = "Height: ${characterDetails.height}", style = typography.bodyMedium)
                Text(text = "Mass: ${characterDetails.mass}", style = typography.bodyMedium)
                Text(text = "Skin Color: ${characterDetails.skinColor}", style = typography.bodyMedium)
                Text(text = "Homeworld: ${characterDetails.homeworld}", style = typography.bodyMedium)
            }
        }
    }
}

@Composable
fun StarshipDetailsScreen(starshipId: String) {
    val viewModel: MainViewModel = koinViewModel()
    val starship by viewModel.selectedStarship.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(starshipId) {
        viewModel.handleIntent(MainIntent.FetchStarshipDetails(starshipId))
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        starship?.let { starshipDetails: Starship ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${starshipDetails.name}", style = typography.bodyLarge)
                Text(text = "Model: ${starshipDetails.model}", style = typography.bodyMedium)
                Text(text = "Starship Class: ${starshipDetails.starshipClass}", style = typography.bodyMedium)
                Text(text = "Manufacturers: ${starshipDetails.manufacturers.joinToString()}", style = typography.bodyMedium)
                Text(text = "Length: ${starshipDetails.length}", style = typography.bodyMedium)
                Text(text = "Crew: ${starshipDetails.crew}", style = typography.bodyMedium)
                Text(text = "Passengers: ${starshipDetails.passengers}", style = typography.bodyMedium)
                Text(text = "Max Atmosphering Speed: ${starshipDetails.maxAtmospheringSpeed}", style = typography.bodyMedium)
                Text(text = "Hyperdrive Rating: ${starshipDetails.hyperdriveRating}", style = typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PlanetDetailsScreen(planetId: String) {
    val viewModel: MainViewModel = koinViewModel()
    val planet by viewModel.selectedPlanet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(planetId) {
        viewModel.handleIntent(MainIntent.FetchPlanetDetails(planetId))
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        planet?.let { planetDetails: Planet ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${planetDetails.name}", style = typography.bodyLarge)
                Text(text = "Climates: ${planetDetails.climates.joinToString()}", style = typography.bodyMedium)
                Text(text = "Diameter: ${planetDetails.diameter}", style = typography.bodyMedium)
                Text(text = "Rotation Period: ${planetDetails.rotationPeriod}", style = typography.bodyMedium)
                Text(text = "Orbital Period: ${planetDetails.orbitalPeriod}", style = typography.bodyMedium)
                Text(text = "Gravity: ${planetDetails.gravity}", style = typography.bodyMedium)
                Text(text = "Population: ${planetDetails.population}", style = typography.bodyMedium)
                Text(text = "Terrains: ${planetDetails.terrains.joinToString()}", style = typography.bodyMedium)
                Text(text = "Surface Water: ${planetDetails.surfaceWater}", style = typography.bodyMedium)
            }
        }
    }
}

class CharacterPagingSource(
    private val apolloClient: ApolloClient
) : PagingSource<String, StarWarsCharacter>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, StarWarsCharacter> {
        return try {
            val response = apolloClient.query(GetCharactersQuery(Optional.presentIfNotNull(params.key), Optional.present(params.loadSize))).execute()
            val characters = response.data?.allPeople?.edges?.mapNotNull { edge ->
                edge?.node?.let { person ->
                    StarWarsCharacter(
                        id = person.id,
                        name = person.name,
                        filmsCount = person.filmConnection?.totalCount ?: 0,
                        birthYear = person.birthYear ?: "Unknown",
                        eyeColor = person.eyeColor ?: "Unknown",
                        gender = person.gender ?: "Unknown",
                        hairColor = person.hairColor ?: "Unknown",
                        height = person.height ?: 0,
                        mass = person.mass ?: 0.0,
                        skinColor = person.skinColor ?: "Unknown",
                        homeworld = person.homeworld?.name ?: "Unknown"
                    )
                }
            } ?: emptyList()

            val nextKey = response.data?.allPeople?.pageInfo?.endCursor

            LoadResult.Page(
                data = characters,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, StarWarsCharacter>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}

class PlanetPagingSource(
    private val apolloClient: ApolloClient
) : PagingSource<String, Planet>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Planet> {
        return try {
            val response = apolloClient.query(GetPlanetsQuery(Optional.presentIfNotNull(params.key), Optional.present(params.loadSize))).execute()
            val planets = response.data?.allPlanets?.edges?.mapNotNull { edge ->
                edge?.node?.let { planet ->
                    Planet(
                        id = planet.id,
                        name = planet.name,
                        climates = planet.climates?.filterNotNull() ?: emptyList(),
                        diameter = planet.diameter ?: 0,
                        rotationPeriod = planet.rotationPeriod ?: 0,
                        orbitalPeriod = planet.orbitalPeriod ?: 0,
                        gravity = planet.gravity ?: "Unknown",
                        population = planet.population ?: 0.0,
                        terrains = planet.terrains?.filterNotNull() ?: emptyList(),
                        surfaceWater = planet.surfaceWater ?: 0.0
                    )
                }
            } ?: emptyList()

            val nextKey = response.data?.allPlanets?.pageInfo?.endCursor

            LoadResult.Page(
                data = planets,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Planet>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}

class StarshipPagingSource(
    private val apolloClient: ApolloClient
) : PagingSource<String, Starship>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Starship> {
        return try {
            val response = apolloClient.query(GetStarshipsQuery(Optional.presentIfNotNull(params.key), Optional.present(params.loadSize))).execute()
            val starships = response.data?.allStarships?.edges?.mapNotNull { edge ->
                edge?.node?.let { starship ->
                    Starship(
                        id = starship.id,
                        name = starship.name,
                        model = starship.model ?: "",
                        starshipClass = starship.starshipClass ?: "",
                        manufacturers = starship.manufacturers?.filterNotNull() ?: emptyList(),
                        length = starship.length?.toFloat() ?: 0f,
                        crew = starship.crew ?: "",
                        passengers = starship.passengers ?: "",
                        maxAtmospheringSpeed = starship.maxAtmospheringSpeed ?: 0,
                        hyperdriveRating = starship.hyperdriveRating?.toFloat() ?: 0f
                    )
                }
            } ?: emptyList()

            val nextKey = response.data?.allStarships?.pageInfo?.endCursor

            LoadResult.Page(
                data = starships,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Starship>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}

@Composable
fun CharacterItem(character: StarWarsCharacter, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Name: ${character.name}", style = typography.bodyMedium)
            Text(text = "Films Count: ${character.filmsCount}", style = typography.bodyLarge)
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (character.isFavorite) Icons.Default.Favorite else Icons.Default.Star,
                contentDescription = if (character.isFavorite) "Remove from favorites" else "Add to favorites"
            )
        }
    }
}

@Composable
fun StarshipItem(starship: Starship, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp).clickable(onClick = onClick)) {
        Text(text = "Starship: ${starship.name}", style = typography.bodyMedium)
    }
}

@Composable
fun PlanetItem(planet: Planet, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp).clickable(onClick = onClick)) {
        Text(text = "Planet: ${planet.name}", style = typography.bodyMedium)
    }
}

sealed class NavigationItem(var route: String, var icon: ImageVector, var title: String) {
    data object Characters : NavigationItem("characters", Icons.Default.Person, "Characters")
    data object Starships : NavigationItem("starships", Icons.Default.Star, "Starships")
    data object Planets : NavigationItem("planets", Icons.Default.Place, "Planets")
}

@Entity(tableName = "characters")
data class StarWarsCharacter(
    @PrimaryKey val id: String,
    val name: String,
    val filmsCount: Int,
    val birthYear: String,
    val eyeColor: String,
    val gender: String,
    val hairColor: String,
    val height: Int,
    val mass: Double,
    val skinColor: String,
    val homeworld: String?,
    var isFavorite: Boolean = false
)

@Entity(tableName = "starships")
data class Starship(
    @PrimaryKey val id: String,
    val name: String,
    val model: String,
    val starshipClass: String,
    val manufacturers: List<String>,
    val length: Float,
    val crew: String,
    val passengers: String,
    val maxAtmospheringSpeed: Int,
    val hyperdriveRating: Float
)

@Entity(tableName = "planets")
data class Planet(
    @PrimaryKey val id: String,
    val name: String,
    val climates: List<String>,
    val diameter: Int,
    val rotationPeriod: Int,
    val orbitalPeriod: Int,
    val gravity: String,
    val population: Double,
    val terrains: List<String>,
    val surfaceWater: Double
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters")
    suspend fun getAllCharacters(): List<StarWarsCharacter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(vararg characters: StarWarsCharacter)

    @Query("UPDATE characters SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)
}

@Dao
interface StarshipDao {
    @Query("SELECT * FROM starships")
    suspend fun getAllStarships(): List<Starship>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStarships(vararg starships: Starship)
}

@Dao
interface PlanetDao {
    @Query("SELECT * FROM planets")
    suspend fun getAllPlanets(): List<Planet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanets(vararg planets: Planet)
}

@Database(entities = [StarWarsCharacter::class, Starship::class, Planet::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun starshipDao(): StarshipDao
    abstract fun planetDao(): PlanetDao
}

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}

class App : Application() {
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val appModule = module {
        viewModel { MainViewModel(get(), get(), get(), get()) }
        single {
            Room.databaseBuilder(get(), AppDatabase::class.java, DATABASE_NAME).build()
        }
        single { get<AppDatabase>().characterDao() }
        single { get<AppDatabase>().starshipDao() }
        single { get<AppDatabase>().planetDao() }
        single {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            ApolloClient.Builder()
                .serverUrl(HOST_GRAPHQL)
                .okHttpClient(okHttpClient)
                .build()
        }
        single {
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
        single { provideSharedPreferences(androidContext()) }
        single { SettingsManager(get()) }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
        setupNetworkListener()
    }

    private fun provideSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private fun setupNetworkListener() {
        val connectivityManager: ConnectivityManager by inject()
        val mainViewModel: MainViewModel by inject()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Toast.makeText(this@App, TOAST_INTERNET_AVAILABLE, Toast.LENGTH_SHORT).show()
                if (mainViewModel.areListsEmpty()) {
                    mainViewModel.refreshData()
                }
            }

            override fun onLost(network: Network) {
                Toast.makeText(this@App, TOAST_INTERNET_LOST, Toast.LENGTH_SHORT).show()
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onTerminate() {
        val connectivityManager: ConnectivityManager by inject()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onTerminate()
    }

    private companion object {
        const val DATABASE_NAME = "starwars-database"
        const val HOST_GRAPHQL = "https://swapi-graphql.netlify.app/.netlify/functions/index"
        const val TOAST_INTERNET_AVAILABLE = "Internet connection established"
        const val TOAST_INTERNET_LOST = "Lost internet connection"
    }
}

class SettingsManager(private val sharedPreferences: SharedPreferences) {

    fun saveThemeVariant(themeVariant: ThemeVariant) {
        sharedPreferences.edit().putString("theme_variant", themeVariant.name).apply()
    }

    fun loadThemeVariant(): ThemeVariant {
        val themeVariantName = sharedPreferences.getString("theme_variant", ThemeVariant.MorningMystic.name)
        return ThemeVariant.valueOf(themeVariantName ?: ThemeVariant.MorningMystic.name)
    }

    fun saveTypographyVariant(typographyVariant: TypographyVariant) {
        sharedPreferences.edit().putString("typography_variant", typographyVariant.name).apply()
    }

    fun loadTypographyVariant(): TypographyVariant {
        val typographyVariantName = sharedPreferences.getString("typography_variant", TypographyVariant.Classic.name)
        return TypographyVariant.valueOf(typographyVariantName ?: TypographyVariant.Classic.name)
    }

    fun setDarkMode(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean("isDarkMode", isDarkMode).apply()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("isDarkMode", false)
    }
}

sealed class MainState {
    object Loading : MainState()
    data class DataLoaded(
        val characters: List<StarWarsCharacter>,
        val starships: List<Starship>,
        val planets: List<Planet>
    ) : MainState()

    data class Error(val message: String) : MainState()
}

sealed class MainIntent {
    object LoadData : MainIntent()
    data class UpdateFavoriteStatus(val characterId: String, val isFavorite: Boolean) : MainIntent()
    data class FetchCharacterDetails(val characterId: String) : MainIntent()
    data class FetchStarshipDetails(val starshipId: String) : MainIntent()
    data class FetchPlanetDetails(val planetId: String) : MainIntent()
    object LoadMoreCharacters : MainIntent()
    object LoadMorePlanets : MainIntent()
    object LoadMoreStarships : MainIntent()
}

class MainViewModel(
    private val apolloClient: ApolloClient,
    private val characterDao: CharacterDao,
    private val starshipDao: StarshipDao,
    private val planetDao: PlanetDao
) : ViewModel() {
    private val _state = MutableStateFlow<MainState>(MainState.Loading)
    val state: StateFlow<MainState> = _state

    private var charactersEndCursor: String? = null
    private var charactersHasNextPage: Boolean = true

    private var starshipsEndCursor: String? = null
    private var starshipsHasNextPage: Boolean = true

    private var planetsEndCursor: String? = null
    private var planetsHasNextPage: Boolean = true

    private val _selectedCharacter = MutableStateFlow<StarWarsCharacter?>(null)
    val selectedCharacter: StateFlow<StarWarsCharacter?> = _selectedCharacter

    private val _selectedStarship = MutableStateFlow<Starship?>(null)
    val selectedStarship: StateFlow<Starship?> = _selectedStarship

    private val _selectedPlanet = MutableStateFlow<Planet?>(null)
    val selectedPlanet: StateFlow<Planet?> = _selectedPlanet

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val charactersPager: Flow<PagingData<StarWarsCharacter>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { CharacterPagingSource(apolloClient) }
    ).flow.cachedIn(viewModelScope)

    val planetsPager: Flow<PagingData<Planet>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { PlanetPagingSource(apolloClient) }
    ).flow.cachedIn(viewModelScope)

    val starshipsPager: Flow<PagingData<Starship>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { StarshipPagingSource(apolloClient) }
    ).flow.cachedIn(viewModelScope)

    init {
        handleIntent(MainIntent.LoadData)
    }

    fun handleIntent(intent: MainIntent) {
        viewModelScope.launch {
            when (intent) {
                is MainIntent.LoadData -> loadData()
                is MainIntent.UpdateFavoriteStatus -> updateFavoriteStatus(intent.characterId, intent.isFavorite)
                is MainIntent.FetchCharacterDetails -> fetchCharacterDetails(intent.characterId)
                is MainIntent.FetchStarshipDetails -> fetchStarshipDetails(intent.starshipId)
                is MainIntent.FetchPlanetDetails -> fetchPlanetDetails(intent.planetId)
                is MainIntent.LoadMoreCharacters -> loadMoreCharacters()
                is MainIntent.LoadMoreStarships -> loadMoreStarships()
                is MainIntent.LoadMorePlanets -> loadMorePlanets()
            }
        }
    }

    private suspend fun loadData() {
        _state.value = MainState.Loading
        try {
            val charactersDeferred = viewModelScope.async(Dispatchers.IO) { fetchCharacters() }
            val starshipsDeferred = viewModelScope.async(Dispatchers.IO) { fetchStarships() }
            val planetsDeferred = viewModelScope.async(Dispatchers.IO) { fetchPlanets() }

            val characters = charactersDeferred.await()
            val starships = starshipsDeferred.await()
            val planets = planetsDeferred.await()

            _state.value = MainState.DataLoaded(characters, starships, planets)
        } catch (e: Exception) {
            _state.value = MainState.Error(e.message ?: "Unknown Error")
        }
    }

    private suspend fun updateFavoriteStatus(characterId: String, isFavorite: Boolean) {
        characterDao.updateFavoriteStatus(characterId, isFavorite)
        val currentState = _state.value
        if (currentState is MainState.DataLoaded) {
            val updatedCharacters = currentState.characters.map {
                if (it.id == characterId) it.copy(isFavorite = isFavorite) else it
            }
            _state.value = currentState.copy(characters = updatedCharacters)
        }
    }

    private suspend fun fetchCharacterDetails(characterId: String) {
        _isLoading.value = true
        try {
            val character = fetchCharacterDetailsFromServer(characterId)
            _selectedCharacter.value = character
        } catch (e: Exception) {
            _state.value = MainState.Error(e.message ?: "Unknown Error")
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun fetchStarshipDetails(starshipId: String) {
        _isLoading.value = true
        try {
            val starship = fetchStarshipDetailsFromServer(starshipId)
            _selectedStarship.value = starship
        } catch (e: Exception) {
            _state.value = MainState.Error(e.message ?: "Unknown Error")
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun fetchPlanetDetails(planetId: String) {
        _isLoading.value = true
        try {
            val planet = fetchPlanetDetailsFromServer(planetId)
            _selectedPlanet.value = planet
        } catch (e: Exception) {
            _state.value = MainState.Error(e.message ?: "Unknown Error")
        } finally {
            _isLoading.value = false
        }
    }

    fun areListsEmpty(): Boolean {
        val currentState = _state.value
        return if (currentState is MainState.DataLoaded) {
            currentState.characters.isEmpty() || currentState.starships.isEmpty() || currentState.planets.isEmpty()
        } else {
            true
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun fetchCharacters(): List<StarWarsCharacter> {
        val cachedCharacters = characterDao.getAllCharacters()
        if (cachedCharacters.isNotEmpty()) {
            return cachedCharacters
        }

        val charactersList = getCharactersFromServer() ?: emptyList()
        if (charactersList.isNotEmpty()) {
            characterDao.insertCharacters(*charactersList.toTypedArray())
        }
        return charactersList
    }

    private suspend fun fetchStarships(): List<Starship> {
        val cachedStarships = starshipDao.getAllStarships()
        if (cachedStarships.isNotEmpty()) {
            return cachedStarships
        }

        val starshipsList = getStarshipsFromServer() ?: emptyList()
        if (starshipsList.isNotEmpty()) {
            starshipDao.insertStarships(*starshipsList.toTypedArray())
        }
        return starshipsList
    }

    private suspend fun fetchPlanets(): List<Planet> {
        val cachedPlanets = planetDao.getAllPlanets()
        if (cachedPlanets.isNotEmpty()) {
            return cachedPlanets
        }

        val planetsList = getPlanetsFromServer() ?: emptyList()
        if (planetsList.isNotEmpty()) {
            planetDao.insertPlanets(*planetsList.toTypedArray())
        }
        return planetsList
    }

    private suspend fun getCharactersFromServer(after: String? = null, first: Int = 10): List<StarWarsCharacter>? {
        val response = try {
            apolloClient.query(GetCharactersQuery(Optional.presentIfNotNull(after), Optional.present(first))).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        val newCharacters = response.data?.allPeople?.edges?.mapNotNull { edge ->
            edge?.node?.let { person: GetCharactersQuery.Node ->
                StarWarsCharacter(
                    id = person.id,
                    name = person.name,
                    filmsCount = person.filmConnection?.totalCount ?: 0,
                    birthYear = person.birthYear ?: "Unknown",
                    eyeColor = person.eyeColor ?: "Unknown",
                    gender = person.gender ?: "Unknown",
                    hairColor = person.hairColor ?: "Unknown",
                    height = person.height ?: 0,
                    mass = person.mass ?: 0.0,
                    skinColor = person.skinColor ?: "Unknown",
                    homeworld = person.homeworld?.name ?: "Unknown"
                )
            }
        } ?: emptyList()

        charactersEndCursor = response.data?.allPeople?.pageInfo?.endCursor
        charactersHasNextPage = response.data?.allPeople?.pageInfo?.hasNextPage ?: false

        return newCharacters
    }

    suspend fun loadMoreCharacters() {
        if (charactersHasNextPage) {
            try {
                val moreCharacters = getCharactersFromServer(charactersEndCursor)
                val currentState = _state.value
                if (currentState is MainState.DataLoaded) {
                    _state.value = currentState.copy(characters = currentState.characters + (moreCharacters ?: emptyList()))
                }
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    private suspend fun getStarshipsFromServer(after: String? = null, first: Int = 10): List<Starship>? {
        val response = try {
            apolloClient.query(GetStarshipsQuery(Optional.presentIfNotNull(after), Optional.present(first))).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        val newStarships = response.data?.allStarships?.edges?.mapNotNull { edge ->
            edge?.node?.let { starship ->
                Starship(
                    id = starship.id,
                    name = starship.name,
                    model = starship.model ?: "",
                    starshipClass = starship.starshipClass ?: "",
                    manufacturers = starship.manufacturers?.filterNotNull() ?: emptyList(),
                    length = starship.length?.toFloat() ?: 0f,
                    crew = starship.crew ?: "",
                    passengers = starship.passengers ?: "",
                    maxAtmospheringSpeed = starship.maxAtmospheringSpeed ?: 0,
                    hyperdriveRating = starship.hyperdriveRating?.toFloat() ?: 0f
                )
            }
        } ?: emptyList()

        starshipsEndCursor = response.data?.allStarships?.pageInfo?.endCursor
        starshipsHasNextPage = response.data?.allStarships?.pageInfo?.hasNextPage ?: false

        return newStarships
    }

    suspend fun loadMoreStarships() {
        if (starshipsHasNextPage) {
            try {
                val moreStarships = getStarshipsFromServer(starshipsEndCursor)
                val currentState = _state.value
                if (currentState is MainState.DataLoaded) {
                    _state.value = currentState.copy(starships = currentState.starships + (moreStarships ?: emptyList()))
                }
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    private suspend fun getPlanetsFromServer(after: String? = null, first: Int = 10): List<Planet>? {
        val response = try {
            apolloClient.query(GetPlanetsQuery(Optional.presentIfNotNull(after), Optional.present(first))).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        val newPlanets = response.data?.allPlanets?.edges?.mapNotNull { edge ->
            edge?.node?.let { planet ->
                Planet(
                    id = planet.id,
                    name = planet.name,
                    climates = planet.climates?.mapNotNull { it } ?: emptyList(),
                    diameter = planet.diameter ?: 0,
                    rotationPeriod = planet.rotationPeriod ?: 0,
                    orbitalPeriod = planet.orbitalPeriod ?: 0,
                    gravity = planet.gravity ?: "Unknown",
                    population = planet.population ?: 0.0,
                    terrains = planet.terrains?.mapNotNull { it } ?: emptyList(),
                    surfaceWater = planet.surfaceWater ?: 0.0
                )
            }
        } ?: emptyList()

        planetsEndCursor = response.data?.allPlanets?.pageInfo?.endCursor
        planetsHasNextPage = response.data?.allPlanets?.pageInfo?.hasNextPage ?: false

        return newPlanets
    }

    suspend fun loadMorePlanets() {
        if (planetsHasNextPage) {
            try {
                val morePlanets = getPlanetsFromServer(planetsEndCursor)
                val currentState = _state.value
                if (currentState is MainState.DataLoaded) {
                    _state.value = currentState.copy(planets = currentState.planets + (morePlanets ?: emptyList()))
                }
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    private suspend fun fetchCharacterDetailsFromServer(characterId: String): StarWarsCharacter? {
        val response = try {
            apolloClient.query(GetCharacterDetailsQuery(characterId)).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        return response.data?.person?.let { person ->
            StarWarsCharacter(
                id = person.id,
                name = person.name,
                birthYear = person.birthYear ?: "Unknown",
                eyeColor = person.eyeColor ?: "Unknown",
                gender = person.gender ?: "Unknown",
                hairColor = person.hairColor ?: "Unknown",
                height = person.height ?: 0,
                mass = person.mass ?: 0.0,
                homeworld = person.homeworld?.name ?: "Unknown",
                filmsCount = person.filmConnection?.totalCount ?: 0,
                skinColor = person.skinColor ?: "Unknown"
            )
        }
    }

    private suspend fun fetchStarshipDetailsFromServer(starshipId: String): Starship? {
        val response = try {
            apolloClient.query(GetStarshipDetailsQuery(starshipId)).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        return response.data?.starship?.let { starship ->
            Starship(
                id = starship.id,
                name = starship.name,
                model = starship.model ?: "",
                starshipClass = starship.starshipClass ?: "",
                manufacturers = starship.manufacturers?.filterNotNull() ?: emptyList(),
                length = starship.length?.toFloat() ?: 0f,
                crew = starship.crew ?: "",
                passengers = starship.passengers ?: "",
                maxAtmospheringSpeed = starship.maxAtmospheringSpeed ?: 0,
                hyperdriveRating = starship.hyperdriveRating?.toFloat() ?: 0f
            )
        }
    }

    private suspend fun fetchPlanetDetailsFromServer(planetId: String): Planet? {
        val response = try {
            apolloClient.query(GetPlanetDetailsQuery(planetId)).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        return response.data?.planet?.let { planet ->
            Planet(
                id = planet.id,
                name = planet.name,
                climates = planet.climates?.mapNotNull { it } ?: emptyList(),
                diameter = planet.diameter ?: 0,
                rotationPeriod = planet.rotationPeriod ?: 0,
                orbitalPeriod = planet.orbitalPeriod ?: 0,
                gravity = planet.gravity ?: "Unknown",
                population = planet.population ?: 0.0,
                terrains = planet.terrains?.mapNotNull { it } ?: emptyList(),
                surfaceWater = planet.surfaceWater ?: 0.0
            )
        }
    }
}
