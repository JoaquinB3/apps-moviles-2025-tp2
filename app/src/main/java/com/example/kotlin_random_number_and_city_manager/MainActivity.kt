package com.example.kotlin_random_number_and_city_manager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.kotlin_random_number_and_city_manager.ui.theme.KotlinrandomnumberandcitymanagerTheme
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinrandomnumberandcitymanagerTheme {
                Menu()
            }
        }
    }
}

@Composable
fun HeaderText(text: String) {
    Text(
        text = text,
        fontSize = 38.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 50.dp)
    )
}

@Composable
fun ModuleButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = White
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CityModuleButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(300.dp)
            .height(55.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Menu() {
    val selectedModule = remember { mutableStateOf("Seleccione un Módulo:") }
    val buttonsVisible = remember { mutableStateOf(true) }
    val score = remember { mutableStateOf(0) }
    val attempts = remember { mutableStateOf(0) }
    val randomNumber = remember { mutableStateOf(Random.nextInt(1, 6)) }
    val userGuess = remember { mutableStateOf(TextFieldValue("")) }
    val showGameOverDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("GamePreferences", Context.MODE_PRIVATE)
    val highestScore = remember { mutableStateOf(sharedPreferences.getInt("highestScore", 0)) }

    val dbHelper = remember { CityDatabaseHelper(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeaderText(selectedModule.value)

        if (buttonsVisible.value) {
            ModuleButton("Adivinar el número", onClick = {
                selectedModule.value = "Adivinar el número entre 1 y 5"
                buttonsVisible.value = false
            })
            Spacer(modifier = Modifier.height(16.dp))
            ModuleButton("Ciudades del mundo", onClick = {
                selectedModule.value = "Ciudades del mundo"
                buttonsVisible.value = false
            })
        } else {
            if (selectedModule.value == "Adivinar el número entre 1 y 5") {
                GameModule(
                    score = score,
                    attempts = attempts,
                    randomNumber = randomNumber,
                    userGuess = userGuess,
                    onGameOver = {
                        showGameOverDialog.value = true
                    },
                    onCorrectGuess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("¡Correcto!", duration = SnackbarDuration.Short)
                        }
                    },
                    onWrongGuess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Incorrecto, intenta nuevamente", duration = SnackbarDuration.Short)
                        }
                    },
                    highestScore = highestScore,
                    saveHighestScore = { newScore ->
                        with(sharedPreferences.edit()) {
                            putInt("highestScore", newScore)
                            apply()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModuleButton("Volver al menú", onClick = {
                    selectedModule.value = "Seleccione un Módulo"
                    buttonsVisible.value = true
                })
            } else {
                CityModule(dbHelper, snackbarHostState)

                Spacer(modifier = Modifier.height(16.dp))

                ModuleButton("Volver al menú", onClick = {
                    selectedModule.value = "Seleccione un Módulo"
                    buttonsVisible.value = true
                })
            }
        }

        if (showGameOverDialog.value) {
            AlertDialog(
                onDismissRequest = { showGameOverDialog.value = false },
                title = { Text("Fin del juego") },
                text = { Text("Has fallado 5 veces seguidas. Tu puntaje ha sido reiniciado.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showGameOverDialog.value = false
                            selectedModule.value = "Seleccione un Módulo"
                            buttonsVisible.value = true
                            resetGame(score, attempts, randomNumber)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = White
                        )
                    ) {
                        Text("Ok")
                    }
                }
            )
        }
    }
    SnackbarHost(hostState = snackbarHostState)
}

@Composable
fun GameModule(
    score: MutableState<Int>,
    attempts: MutableState<Int>,
    randomNumber: MutableState<Int>,
    userGuess: MutableState<TextFieldValue>,
    onGameOver: () -> Unit,
    onCorrectGuess: () -> Unit,
    onWrongGuess: () -> Unit,
    highestScore: MutableState<Int>,
    saveHighestScore: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Puntaje: ${score.value}",
            fontSize = 30.sp,
            lineHeight = 50.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Text(
            text = "Puntaje más alto: ${highestScore.value}",
            fontSize = 20.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 50.dp)
        )

        OutlinedTextField(
            value = userGuess.value,
            onValueChange = { userGuess.value = it },
            label = { Text("Ingrese un número:") },
            modifier = Modifier
                .width(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ModuleButton("Adivinar", onClick = {
            val guess = userGuess.value.text.toIntOrNull()
            if (guess != null && guess in 1..5) {
                if (guess == randomNumber.value) {
                    score.value += 10
                    attempts.value = 0
                    randomNumber.value = Random.nextInt(1, 6)
                    onCorrectGuess()

                    if (score.value > highestScore.value) {
                        highestScore.value = score.value
                        saveHighestScore(score.value)
                    }
                } else {
                    attempts.value += 1
                    onWrongGuess()
                    if (attempts.value == 5) {
                        score.value = 0
                        attempts.value = 0
                        randomNumber.value = Random.nextInt(1, 6)
                        onGameOver()
                    }
                }
            }
        })
    }
}

fun resetGame(
    score: MutableState<Int>,
    attempts: MutableState<Int>,
    randomNumber: MutableState<Int>
) {
    score.value = 0
    attempts.value = 0
    randomNumber.value = Random.nextInt(1, 6)
}

@Composable
fun CityModule(dbHelper: CityDatabaseHelper, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var cityName by remember { mutableStateOf("") }
    var countryName by remember { mutableStateOf("") }
    var population by remember { mutableStateOf("") }
    var operationResult by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = cityName,
            onValueChange = { cityName = it },
            label = { Text("Nombre de la ciudad") },
            modifier = Modifier.width(300.dp).padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = countryName,
            onValueChange = { countryName = it },
            label = { Text("Nombre del país") },
            modifier = Modifier.width(300.dp).padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = population,
            onValueChange = { population = it },
            label = { Text("Población") },
            modifier = Modifier.width(300.dp).padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        CityModuleButton("Cargar una ciudad capital", onClick = {
            if (cityName.isNotEmpty() && countryName.isNotEmpty() && population.isNotEmpty()) {
                val result = dbHelper.insertCity(cityName, countryName, population.toLongOrNull() ?: 0)
                operationResult = if (result != -1L) "Ciudad agregada exitosamente" else "Error al agregar la ciudad"
                scope.launch {
                    snackbarHostState.showSnackbar(operationResult)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Por favor, complete todos los campos")
                }
            }
        })

        Spacer(modifier = Modifier.height(8.dp))

        CityModuleButton("Consultar ciudad por nombre", onClick = {
            if (cityName.isNotEmpty()) {
                val city = dbHelper.getCityByName(cityName)
                operationResult = if (city != null) {
                    "Ciudad: ${city.name}, País: ${city.country}, Población: ${city.population}"
                } else {
                    "Ciudad no encontrada"
                }
                scope.launch {
                    snackbarHostState.showSnackbar(operationResult)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Por favor, ingrese el nombre de la ciudad")
                }
            }
        })

        Spacer(modifier = Modifier.height(8.dp))

        CityModuleButton("Borrar ciudad por su nombre", onClick = {
            if (cityName.isNotEmpty()) {
                val result = dbHelper.deleteCity(cityName)
                operationResult = if (result > 0) "Ciudad eliminada exitosamente" else "Ciudad no encontrada"
                scope.launch {
                    snackbarHostState.showSnackbar(operationResult)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Por favor, ingrese el nombre de la ciudad")
                }
            }
        })

        Spacer(modifier = Modifier.height(8.dp))

        CityModuleButton("Borrar todas las ciudades de un país", onClick = {
            if (countryName.isNotEmpty()) {
                val result = dbHelper.deleteCitiesByCountry(countryName)
                operationResult = if (result > 0) "Ciudades eliminadas exitosamente" else "No se encontraron ciudades para el país"
                scope.launch {
                    snackbarHostState.showSnackbar(operationResult)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Por favor, ingrese el nombre del país")
                }
            }
        })

        Spacer(modifier = Modifier.height(8.dp))

        CityModuleButton("Modificar la población de una ciudad", onClick = {
            if (cityName.isNotEmpty() && population.isNotEmpty()) {
                val result = dbHelper.updateCityPopulation(cityName, population.toLongOrNull() ?: 0)
                operationResult = if (result > 0) "Población actualizada exitosamente" else "Ciudad no encontrada"
                scope.launch {
                    snackbarHostState.showSnackbar(operationResult)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Por favor, ingrese el nombre de la ciudad y la nueva población")
                }
            }
        })
    }
}

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

data class City(val id: Int, val name: String, val country: String, val population: Long)

@Preview(showBackground = true)
@Composable
fun MenuPreview() {
    KotlinrandomnumberandcitymanagerTheme {
        Menu()
    }
}