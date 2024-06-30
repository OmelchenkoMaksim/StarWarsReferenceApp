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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private val charactersViewModel: CharactersViewModel by viewModel()
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

            StarWarsReferenceAppTheme(
                themeVariant = themeVariant.value,
                typographyVariant = typographyVariant.value,
                darkTheme = isDarkTheme.value
            ) {
                val characters by charactersViewModel.characters.collectAsState()
                val starships by charactersViewModel.starships.collectAsState()
                val planets by charactersViewModel.planets.collectAsState()
                val isLoading by charactersViewModel.isLoading.collectAsState()

                MainScreen(
                    characters = characters,
                    starships = starships,
                    planets = planets,
                    isLoading = isLoading,
                    themeVariant = themeVariant,
                    typographyVariant = typographyVariant,
                    isDarkTheme = isDarkTheme,
                    onSaveSettings = { theme, typography ->
                        settingsManager.saveThemeVariant(theme)
                        settingsManager.saveTypographyVariant(typography)
                        settingsManager.setDarkMode(isDarkTheme.value)
                    },
                    charactersViewModel = charactersViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    characters: List<StarWarsCharacter>,
    starships: List<Starship>,
    planets: List<Planet>,
    isLoading: Boolean,
    themeVariant: MutableState<ThemeVariant>,
    typographyVariant: MutableState<TypographyVariant>,
    isDarkTheme: MutableState<Boolean>,
    onSaveSettings: (ThemeVariant, TypographyVariant) -> Unit,
    charactersViewModel: CharactersViewModel
) {
    val navController = rememberNavController()
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Star Wars") },
                actions = {
                    IconButton(onClick = {
                        isDarkTheme.value = !isDarkTheme.value
                        onSaveSettings(themeVariant.value, typographyVariant.value)
                    }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_day_night),
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues: PaddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavigationGraph(
                navController, characters, starships, planets, isLoading, charactersViewModel
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                themeVariant = themeVariant,
                typographyVariant = typographyVariant,
                onSaveSettings = onSaveSettings
            )
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        val currentRoute = currentRoute(navController)
        items.forEach { item: NavigationItem ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    characters: List<StarWarsCharacter>,
    starships: List<Starship>,
    planets: List<Planet>,
    isLoading: Boolean,
    charactersViewModel: CharactersViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = NavigationItem.Characters.route, modifier = modifier) {
        composable(NavigationItem.Characters.route) {
            CharactersScreen(
                characters = characters,
                isLoading = isLoading,
                viewModel = charactersViewModel,
                onCharacterClick = { characterId ->
                    navController.navigate("character_details/$characterId")
                }
            )
        }
        composable(NavigationItem.Starships.route) {
            StarshipsScreen(
                starships = starships,
                isLoading = isLoading,
                viewModel = charactersViewModel,
                onStarshipClick = { starshipId ->
                    navController.navigate("starship_details/$starshipId")
                }
            )
        }
        composable(NavigationItem.Planets.route) {
            PlanetsScreen(
                planets = planets,
                isLoading = isLoading,
                viewModel = charactersViewModel,
                onPlanetClick = { planetId ->
                    navController.navigate("planet_details/$planetId")
                }
            )
        }
        composable("character_details/{characterId}") { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: return@composable
            CharacterDetailsScreen(viewModel = charactersViewModel, characterId = characterId)
        }
        composable("starship_details/{starshipId}") { backStackEntry ->
            val starshipId = backStackEntry.arguments?.getString("starshipId") ?: return@composable
            StarshipDetailsScreen(viewModel = charactersViewModel, starshipId = starshipId)
        }
        composable("planet_details/{planetId}") { backStackEntry ->
            val planetId = backStackEntry.arguments?.getString("planetId") ?: return@composable
            PlanetDetailsScreen(viewModel = charactersViewModel, planetId = planetId)
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
    characters: List<StarWarsCharacter>,
    isLoading: Boolean,
    viewModel: CharactersViewModel,
    modifier: Modifier = Modifier,
    onCharacterClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filter { it != null && it >= characters.size - 1 }
            .distinctUntilChanged()
            .collect {
                viewModel.loadMoreCharacters()
            }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Characters", style = typography.bodyLarge)
        LazyColumn(state = listState) {
            items(characters) { character: StarWarsCharacter ->
                CharacterItem(character = character, onClick = { onCharacterClick(character.id) })
            }
            item {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun StarshipsScreen(
    starships: List<Starship>,
    isLoading: Boolean,
    viewModel: CharactersViewModel,
    modifier: Modifier = Modifier,
    onStarshipClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filter { it != null && it >= starships.size - 1 }
            .distinctUntilChanged()
            .collect {
                viewModel.loadMoreStarships()
            }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Starships", style = typography.bodyLarge)
        LazyColumn(state = listState) {
            items(starships) { starship: Starship ->
                StarshipItem(starship = starship, onClick = { onStarshipClick(starship.id) })
            }
            item {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun PlanetsScreen(
    planets: List<Planet>,
    isLoading: Boolean,
    viewModel: CharactersViewModel,
    modifier: Modifier = Modifier,
    onPlanetClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filter { it != null && it >= planets.size - 1 }
            .distinctUntilChanged()
            .collect {
                viewModel.loadMorePlanets()
            }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Planets", style = typography.bodyLarge)
        LazyColumn(state = listState) {
            items(planets) { planet: Planet ->
                PlanetItem(planet = planet, onClick = { onPlanetClick(planet.id) })
            }
            item {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun CharacterDetailsScreen(viewModel: CharactersViewModel, characterId: String) {
    val character by viewModel.selectedCharacter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(characterId) {
        viewModel.fetchCharacterDetails(characterId)
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        character?.let { characterDetails: StarWarsCharacter ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${characterDetails.name}", style = MaterialTheme.typography.bodyLarge)
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
fun StarshipDetailsScreen(viewModel: CharactersViewModel, starshipId: String) {
    val starship by viewModel.selectedStarship.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(starshipId) {
        viewModel.fetchStarshipDetails(starshipId)
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        starship?.let { starshipDetails: Starship ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${starshipDetails.name}", style = MaterialTheme.typography.bodyLarge)
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
fun PlanetDetailsScreen(viewModel: CharactersViewModel, planetId: String) {
    val planet by viewModel.selectedPlanet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(planetId) {
        viewModel.fetchPlanetDetails(planetId)
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        planet?.let { planetDetails: Planet ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${planetDetails.name}", style = MaterialTheme.typography.bodyLarge)
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

@Composable
fun CharacterItem(character: StarWarsCharacter, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp).clickable(onClick = onClick)) {
        Text(text = "Name: ${character.name}", style = typography.bodyMedium)
        Text(text = "Films Count: ${character.filmsCount}", style = typography.bodyLarge)
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
    val homeworld: String?
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
        viewModel { CharactersViewModel(get(), get(), get(), get()) }
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
        val charactersViewModel: CharactersViewModel by inject()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Toast.makeText(this@App, TOAST_INTERNET_AVAILABLE, Toast.LENGTH_SHORT).show()
                if (charactersViewModel.areListsEmpty()) {
                    charactersViewModel.refreshData()
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

class CharactersViewModel(
    private val apolloClient: ApolloClient,
    private val characterDao: CharacterDao,
    private val starshipDao: StarshipDao,
    private val planetDao: PlanetDao
) : ViewModel() {
    private val _characters = MutableStateFlow<List<StarWarsCharacter>>(emptyList())
    val characters: StateFlow<List<StarWarsCharacter>> = _characters

    private val _starships = MutableStateFlow<List<Starship>>(emptyList())
    val starships: StateFlow<List<Starship>> = _starships

    private val _planets = MutableStateFlow<List<Planet>>(emptyList())
    val planets: StateFlow<List<Planet>> = _planets

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

    init {
        viewModelScope.launch {
            fetchCharacters()
            fetchStarships()
            fetchPlanets()
        }
    }

    fun fetchCharacterDetails(characterId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = try {
                apolloClient.query(GetCharacterDetailsQuery(characterId)).execute()
            } catch (e: ApolloException) {
                println("ApolloException: $e")
                _isLoading.value = false
                return@launch
            }

            response.data?.person?.let { person: GetCharacterDetailsQuery.Person ->
                _selectedCharacter.value = StarWarsCharacter(
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
            _isLoading.value = false
        }
    }

    fun fetchStarshipDetails(starshipId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = try {
                apolloClient.query(GetStarshipDetailsQuery(starshipId)).execute()
            } catch (e: ApolloException) {
                println("ApolloException: $e")
                _isLoading.value = false
                return@launch
            }

            response.data?.starship?.let { starship: GetStarshipDetailsQuery.Starship ->
                _selectedStarship.value = Starship(
                    id = starship.id,
                    name = starship.name,
                    model = starship.model ?: "",
                    starshipClass = starship.starshipClass ?: "",
                    manufacturers = starship.manufacturers?.filterNotNull() ?: emptyList(),
                    length = starship.length?.toFloat() ?: 0f,
                    crew = starship.crew ?: "",
                    passengers = starship.passengers ?: "",
                    maxAtmospheringSpeed = starship.maxAtmospheringSpeed ?: 0,
                    hyperdriveRating = starship.hyperdriveRating?.toFloat() ?: 0f,
                )
            }
            _isLoading.value = false
        }
    }

    fun fetchPlanetDetails(planetId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = try {
                apolloClient.query(GetPlanetDetailsQuery(planetId)).execute()
            } catch (e: ApolloException) {
                println("ApolloException: $e")
                _isLoading.value = false
                return@launch
            }

            response.data?.planet?.let { planet: GetPlanetDetailsQuery.Planet ->
                _selectedPlanet.value = Planet(
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
            _isLoading.value = false
        }
    }

    fun areListsEmpty(): Boolean {
        return _characters.value.isEmpty() || _starships.value.isEmpty() || _planets.value.isEmpty()
    }

    fun refreshData() {
        viewModelScope.launch {
            fetchCharacters()
            fetchStarships()
            fetchPlanets()
        }
    }

    private suspend fun fetchCharacters() {
        _isLoading.value = true
        val charactersList = getCharactersFromServer() ?: characterDao.getAllCharacters()
        _characters.value = charactersList
        if (charactersList.isNotEmpty()) {
            characterDao.insertCharacters(*charactersList.toTypedArray())
        }
        _isLoading.value = false
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
            _isLoading.value = true
            val moreCharacters = getCharactersFromServer(charactersEndCursor)
            _characters.value += moreCharacters ?: emptyList()
            _isLoading.value = false
        }
    }

    private suspend fun fetchStarships() {
        _isLoading.value = true
        val starshipsList = getStarshipsFromServer() ?: starshipDao.getAllStarships()
        _starships.value = starshipsList
        if (starshipsList.isNotEmpty()) {
            starshipDao.insertStarships(*starshipsList.toTypedArray())
        }
        _isLoading.value = false
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
            _isLoading.value = true
            val moreStarships = getStarshipsFromServer(starshipsEndCursor)
            _starships.value += moreStarships ?: emptyList()
            _isLoading.value = false
        }
    }

    private suspend fun fetchPlanets() {
        _isLoading.value = true
        val planetsList = getPlanetsFromServer() ?: planetDao.getAllPlanets()
        _planets.value = planetsList
        if (planetsList.isNotEmpty()) {
            planetDao.insertPlanets(*planetsList.toTypedArray())
        }
        _isLoading.value = false
    }

    private suspend fun getPlanetsFromServer(after: String? = null, first: Int = 10): List<Planet>? {
        val response = try {
            apolloClient.query(GetPlanetsQuery(Optional.presentIfNotNull(after), Optional.present(first))).execute()
        } catch (e: ApolloException) {
            println("ApolloException: $e")
            return null
        }

        val newPlanets = response.data?.allPlanets?.edges?.mapNotNull { edge ->
            edge?.node?.let { planet: GetPlanetsQuery.Node ->
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
            _isLoading.value = true
            val morePlanets = getPlanetsFromServer(planetsEndCursor)
            _planets.value += morePlanets ?: emptyList()
            _isLoading.value = false
        }
    }
}