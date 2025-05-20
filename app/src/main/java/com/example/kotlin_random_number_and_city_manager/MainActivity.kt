package com.example.kotlin_random_number_and_city_manager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_random_number_and_city_manager.ui.theme.KotlinrandomnumberandcitymanagerTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinrandomnumberandcitymanagerTheme {
                val currentScreen = remember { mutableStateOf("menu") }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen.value) {
                            "menu" -> MenuScreen(onGameClick = { currentScreen.value = "game" }, onCityClick = { currentScreen.value = "city" })
                            "game" -> GameScreen(onBackToMenu = { currentScreen.value = "menu" }, snackbarHostState)
                            "city" -> CityScreen(onBackToMenu = { currentScreen.value = "menu" }, snackbarHostState)
                        }
                    }
                }
            }
        }
    }
}

data class City(val id: Int, val name: String, val country: String, val population: Long)

class CityDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "CityDatabase.db"
        private const val TABLE_CITIES = "cities"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_COUNTRY = "country"
        private const val KEY_POPULATION = "population"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_CITIES_TABLE = ("CREATE TABLE " + TABLE_CITIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_COUNTRY + " TEXT," + KEY_POPULATION + " INTEGER" + ")")
        db.execSQL(CREATE_CITIES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CITIES")
        onCreate(db)
    }

    fun insertCity(name: String, country: String, population: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_NAME, name)
        values.put(KEY_COUNTRY, country)
        values.put(KEY_POPULATION, population)
        val id = db.insert(TABLE_CITIES, null, values)
        db.close()
        return id
    }

    fun getCityByName(name: String): City? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_CITIES, arrayOf(KEY_ID, KEY_NAME, KEY_COUNTRY, KEY_POPULATION),
            "$KEY_NAME=?", arrayOf(name), null, null, null, null)
        var city: City? = null
        if (cursor.moveToFirst()) {
            city = City(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getLong(3)
            )
        }
        cursor.close()
        return city
    }

    fun deleteCity(name: String): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE_CITIES, "$KEY_NAME=?", arrayOf(name))
        db.close()
        return result
    }

    fun deleteCitiesByCountry(country: String): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE_CITIES, "$KEY_COUNTRY=?", arrayOf(country))
        db.close()
        return result
    }

    fun updateCityPopulation(name: String, newPopulation: Long): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_POPULATION, newPopulation)
        val result = db.update(TABLE_CITIES, values, "$KEY_NAME=?", arrayOf(name))
        db.close()
        return result
    }
}

@Composable
fun HeaderText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(bottom = 32.dp)
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(220.dp)
            .height(55.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
            singleLine = true,
            isError = isError,
            trailingIcon = {
                if (isError) {
                    Icon(Icons.Filled.Info, "Error", tint = MaterialTheme.colorScheme.error)
                }
            }
        )
        if (isError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun MenuScreen(onGameClick: () -> Unit, onCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeaderText("Selecciona un Módulo")

        PrimaryButton("Adivinar el número", onClick = onGameClick)
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton("Ciudades del mundo", onClick = onCityClick)
    }
}

@Composable
fun GameScreen(onBackToMenu: () -> Unit, snackbarHostState: SnackbarHostState) {
    val score = remember { mutableIntStateOf(0) }
    val attempts = remember { mutableIntStateOf(0) }
    val randomNumber = remember { mutableIntStateOf(Random.nextInt(1, 6)) }
    val userGuess = remember { mutableStateOf(TextFieldValue("")) }
    val showGameOverDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("GamePreferences", Context.MODE_PRIVATE)
    val highestScore = remember { mutableIntStateOf(sharedPreferences.getInt("highestScore", 0)) }
    var isInputError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeaderText("Adivina el número (1-5)")

        Text(
            text = "Puntaje: ${score.intValue}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Puntaje más alto: ${highestScore.intValue}",
            fontSize = 20.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        CustomOutlinedTextField(
            value = userGuess.value.text,
            onValueChange = { newValue ->
                userGuess.value = TextFieldValue(newValue.filter { it.isDigit() })
                isInputError = false // Reset error on change
            },
            label = "Ingresa tu número",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(200.dp),
            isError = isInputError,
            errorMessage = "Número inválido (1-5)"
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton("Adivinar", onClick = {
            val guessText = userGuess.value.text.trim()
            val guess = guessText.toIntOrNull()

            if (guess == null || guess !in 1..5) {
                isInputError = true
                scope.launch {
                    snackbarHostState.showSnackbar("Ingrese un número válido del 1 al 5", duration = SnackbarDuration.Short)
                }
                return@PrimaryButton
            }

            if (guess == randomNumber.intValue) {
                score.intValue += 10
                attempts.intValue = 0
                randomNumber.intValue = Random.nextInt(1, 6)
                userGuess.value = TextFieldValue("") // Limpiar el campo
                scope.launch {
                    snackbarHostState.showSnackbar("¡Correcto! Sumaste 10 puntos.", duration = SnackbarDuration.Short)
                }

                if (score.intValue > highestScore.intValue) {
                    highestScore.intValue = score.intValue
                    with(sharedPreferences.edit()) {
                        putInt("highestScore", score.intValue)
                        apply()
                    }
                }
            } else {
                attempts.intValue += 1
                userGuess.value = TextFieldValue("") // Limpiar el campo
                scope.launch {
                    snackbarHostState.showSnackbar("Incorrecto, intenta nuevamente. Intentos fallidos: ${attempts.intValue}", duration = SnackbarDuration.Short)
                }
                if (attempts.intValue >= 5) {
                    showGameOverDialog.value = true
                }
            }
        })

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                resetGame(score, attempts, randomNumber, userGuess)
                onBackToMenu() // Vuelve a la pantalla anterior (Menú)
            },
            modifier = Modifier
                .width(220.dp)
                .height(55.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Text("Volver al menú", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        if (showGameOverDialog.value) {
            AlertDialog(
                onDismissRequest = { showGameOverDialog.value = false },
                title = { Text("¡Juego Terminado!") },
                text = { Text("Has fallado 5 veces seguidas. Tu puntaje ha sido reiniciado.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showGameOverDialog.value = false
                            resetGame(score, attempts, randomNumber, userGuess)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Ok")
                    }
                }
            )
        }
    }
}

fun resetGame(
    score: MutableIntState,
    attempts: MutableIntState,
    randomNumber: MutableIntState,
    userGuess: MutableState<TextFieldValue>
) {
    score.intValue = 0
    attempts.intValue = 0
    randomNumber.intValue = Random.nextInt(1, 6)
    userGuess.value = TextFieldValue("")
}

@Composable
fun CityScreen(onBackToMenu: () -> Unit, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val dbHelper = remember { CityDatabaseHelper(context) }
    val scope = rememberCoroutineScope()

    var cityName by remember { mutableStateOf("") }
    var countryName by remember { mutableStateOf("") }
    var population by remember { mutableStateOf("") }

    var cityNameError by remember { mutableStateOf(false) }
    var countryNameError by remember { mutableStateOf(false) }
    var populationError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderText("Gestión de Ciudades")

        CustomOutlinedTextField(
            value = cityName,
            onValueChange = {
                cityName = it
                cityNameError = false
            },
            label = "Nombre de la ciudad",
            isError = cityNameError,
            errorMessage = "Campo requerido"
        )

        CustomOutlinedTextField(
            value = countryName,
            onValueChange = {
                countryName = it
                countryNameError = false
            },
            label = "Nombre del país",
            isError = countryNameError,
            errorMessage = "Campo requerido"
        )

        CustomOutlinedTextField(
            value = population,
            onValueChange = { newValue ->
                population = newValue.filter { it.isDigit() }
                populationError = false
            },
            label = "Población",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = populationError,
            errorMessage = "Ingrese un número válido"
        )

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryButton("Cargar ciudad", onClick = {
            cityNameError = cityName.isBlank()
            countryNameError = countryName.isBlank()
            populationError = population.toLongOrNull() == null

            if (!cityNameError && !countryNameError && !populationError) {
                val result = dbHelper.insertCity(cityName, countryName, population.toLong())
                val message = if (result != -1L) "Ciudad agregada exitosamente" else "Error al agregar la ciudad"
                scope.launch { snackbarHostState.showSnackbar(message) }
                cityName = ""
                countryName = ""
                population = ""
            } else {
                scope.launch { snackbarHostState.showSnackbar("Por favor, complete todos los campos correctamente") }
            }
        })

        PrimaryButton("Consultar ciudad", onClick = {
            cityNameError = cityName.isBlank()
            if (!cityNameError) {
                val city = dbHelper.getCityByName(cityName)
                val message = if (city != null) {
                    "Ciudad: ${city.name}, País: ${city.country}, Población: ${city.population}"
                } else {
                    "Ciudad no encontrada"
                }
                scope.launch { snackbarHostState.showSnackbar(message) }
            } else {
                scope.launch { snackbarHostState.showSnackbar("Por favor, ingrese el nombre de la ciudad") }
            }
        })

        PrimaryButton("Borrar ciudad", onClick = {
            cityNameError = cityName.isBlank()
            if (!cityNameError) {
                val result = dbHelper.deleteCity(cityName)
                val message = if (result > 0) "Ciudad eliminada exitosamente" else "Ciudad no encontrada"
                scope.launch { snackbarHostState.showSnackbar(message) }
                cityName = ""
            } else {
                scope.launch { snackbarHostState.showSnackbar("Por favor, ingrese el nombre de la ciudad") }
            }
        })

        PrimaryButton("Borrar ciudades por país", onClick = {
            countryNameError = countryName.isBlank()
            if (!countryNameError) {
                val result = dbHelper.deleteCitiesByCountry(countryName)
                val message = if (result > 0) "Ciudades de '$countryName' eliminadas" else "No se encontraron ciudades para '$countryName'"
                scope.launch { snackbarHostState.showSnackbar(message) }
                countryName = ""
            } else {
                scope.launch { snackbarHostState.showSnackbar("Por favor, ingrese el nombre del país") }
            }
        })

        PrimaryButton("Modificar población", onClick = {
            cityNameError = cityName.isBlank()
            populationError = population.toLongOrNull() == null
            if (!cityNameError && !populationError) {
                val result = dbHelper.updateCityPopulation(cityName, population.toLong())
                val message = if (result > 0) "Población de '$cityName' actualizada" else "Ciudad no encontrada"
                scope.launch { snackbarHostState.showSnackbar(message) }
                cityName = ""
                population = ""
            } else {
                scope.launch { snackbarHostState.showSnackbar("Ingrese nombre de la ciudad y población válida") }
            }
        })

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBackToMenu,
            modifier = Modifier
                .width(220.dp)
                .height(55.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Text("Volver al menú", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    KotlinrandomnumberandcitymanagerTheme {
        MenuScreen(onGameClick = {}, onCityClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    KotlinrandomnumberandcitymanagerTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        GameScreen(onBackToMenu = {}, snackbarHostState = snackbarHostState)
    }
}

@Preview(showBackground = true)
@Composable
fun CityScreenPreview() {
    KotlinrandomnumberandcitymanagerTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        CityScreen(onBackToMenu = {}, snackbarHostState = snackbarHostState)
    }
}