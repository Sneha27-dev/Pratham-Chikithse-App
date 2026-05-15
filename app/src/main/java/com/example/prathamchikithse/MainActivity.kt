package com.example.prathamchikithse

import android.os.Bundle
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlinx.coroutines.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)

                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        insertData(this)

        setContent {

            val themeViewModel = remember { ThemeViewModel() }

            MaterialTheme(

                colorScheme =
                    if (themeViewModel.isDarkMode.value)
                        darkColorScheme()
                    else
                        lightColorScheme()

            ) {

                AppNavigation(
                    speak = ::speak,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
    fun speak(text: String?) {

        if (!isTtsReady) return

        // ✅ Stop speaking if screen closes
        if (text.isNullOrBlank()) {

            tts.stop()
            return
        }

        tts.stop()

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
@Composable
fun AppNavigation(
    speak: (String?) -> Unit,
    themeViewModel: ThemeViewModel
) {

    val navController = rememberNavController()

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("home")
                    },
                    icon = {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    },
                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("contacts")
                    },
                    icon = {
                        Icon(Icons.Default.Call, contentDescription = "Contacts")
                    },
                    label = {
                        Text("Contacts")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("about")
                    },
                    icon = {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    },
                    label = {
                        Text("About")
                    }
                )
            }
        }

    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {

            composable("home") {
                HomeScreen(
                    navController,
                    themeViewModel
                )
            }

            composable("detail/{name}") {

                val name = it.arguments?.getString("name") ?: ""

                DetailScreen(name, speak)
            }

            composable("contacts") {
                ContactsScreen(navController)
            }

            composable("about") {
                AboutScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {

    val context = androidx.compose.ui.platform.LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var list by remember { mutableStateOf<List<Emergency>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    var searchText by remember { mutableStateOf("") }

    // 🎤 Voice Search Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == android.app.Activity.RESULT_OK) {

            val data = result.data

            val res = data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )

            if (!res.isNullOrEmpty()) {

                searchText = res[0]
            }
        }
    }

    // ✅ Load Database
    LaunchedEffect(Unit) {

        val db = AppDatabase.getDatabase(context)

        list = withContext(Dispatchers.IO) {
            db.emergencyDao().getAll()
        }

        delay(1000)

        isLoading = false
    }

    // ✅ Loading Screen
    if (isLoading) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator()
        }

        return
    }

    // ✅ Search Filter
    val filteredList = list.filter {

        it.title.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // 🚨 SOS BUTTON
        Button(
            onClick = {

                val intent = android.content.Intent(
                    android.content.Intent.ACTION_DIAL
                )

                intent.data = "tel:108".toUri()

                context.startActivity(intent)
            },

            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            ),

            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)

        ) {

            Text(
                "🚨 SOS - Call Emergency",
                color = Color.White
            )
        }

        // 📞 CONTACT BUTTON
        Button(
            onClick = {
                navController.navigate("contacts")
            },

            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)

        ) {

            Text("📞 Emergency Contacts")
        }

        // 🚨 EMERGENCY TIP
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),

            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "🚨 Emergency Tip",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stay calm and call emergency services immediately during serious situations.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // 🌙 DARK MODE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "🚑 Emergency Guide",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(
                onClick = {
                    themeViewModel.toggleTheme()
                }
            ) {

                Text(
                    if (themeViewModel.isDarkMode.value)
                        "☀️"
                    else
                        "🌙",

                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ℹ ABOUT BUTTON
        Button(
            onClick = {

                val sample = hashMapOf(
                    "message" to "Pratham Chikithse Connected",
                    "time" to System.currentTimeMillis()
                )

                firestore.collection("app_test")
                    .add(sample)
                    .addOnSuccessListener {

                        android.widget.Toast.makeText(
                            context,
                            "Firebase Connected Successfully",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        navController.navigate("about")
                    }

                    .addOnFailureListener {

                        android.widget.Toast.makeText(
                            context,
                            "Firebase Failed",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        )
        {
            Text("About App")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔍 SEARCH + 🎤
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = searchText,

                onValueChange = {
                    searchText = it
                },

                label = {
                    Text("Search emergency...")
                },

                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {

                    val intent = android.content.Intent(
                        android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    )

                    intent.putExtra(
                        android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )

                    intent.putExtra(
                        android.speech.RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.getDefault()
                    )

                    try {

                        launcher.launch(intent)

                    } catch (e: Exception) {

                        android.widget.Toast.makeText(
                            context,
                            "Voice search not supported",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {

                Text("🎤")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📋 EMERGENCY LIST
        filteredList.forEach { item ->

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500))
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),

                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    ),

                    onClick = {
                        navController.navigate("detail/${item.title}")
                    }
                ) {

                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = when (item.title) {

                                "Snake Bite" -> "🐍"
                                "Burn" -> "🔥"
                                "Heart Attack" -> "❤️"
                                "Fracture" -> "🦴"
                                "Fainting" -> "😵"
                                "Choking" -> "😮"
                                "Electric Shock" -> "⚡"
                                "Stroke" -> "🧠"
                                "Poisoning" -> "☠️"
                                "Nose Bleeding" -> "🩸"
                                "Asthma Attack" -> "🫁"
                                "Dog Bite" -> "🐕"
                                "Heat Stroke" -> "☀️"
                                "Cuts & Bleeding" -> "🩹"
                                "Eye Injury" -> "👁️"

                                else -> "🚑"
                            },

                            style = MaterialTheme.typography.headlineMedium
                        )

                        Column {

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Tap to view emergency instructions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun DetailScreen(name: String, speak: (String?) -> Unit) {

    var data by remember { mutableStateOf<Emergency?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // ✅ Load data
    LaunchedEffect(Unit) {

        val db = AppDatabase.getDatabase(context)

        data = withContext(Dispatchers.IO) {
            db.emergencyDao().getByTitle(name)
        }
    }

    // ✅ Stop voice when screen closes
    DisposableEffect(Unit) {

        onDispose {
            speak("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔊 SPEAK BUTTON
        Button(
            onClick = {

                speak(
                    "Emergency $name. " +
                            "Steps: ${data?.steps}. " +
                            "Do's: ${data?.dos}. " +
                            "Don'ts: ${data?.donts}. " +
                            "Stay calm and act immediately."
                )
            }
        ) {

            Text("🔊 Listen Instructions")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔵 STEPS
        Text(
            text = "Steps:",
            style = MaterialTheme.typography.titleMedium
        )

        data?.steps?.split(",")?.forEach {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Text(
                    text = "• $it",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🟢 DO'S
        Text(
            text = "Do's:",
            style = MaterialTheme.typography.titleMedium
        )

        data?.dos?.split(",")?.forEach {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Text(
                    text = "✔ $it",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔴 DON'TS
        Text(
            text = "Don'ts:",
            style = MaterialTheme.typography.titleMedium
        )

        data?.donts?.split(",")?.forEach {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Text(
                    text = "✘ $it",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
@Composable
fun ContactsScreen(navController: NavController) {

    val context = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.padding(16.dp)) {

        Text(
            "🏥 Emergency Help",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ OFFLINE HOSPITAL LIST
        Text("📍 Nearby Hospitals ", style = MaterialTheme.typography.titleMedium)

        val hospitals = listOf(
            Pair("District Hospital", "08012345678"),
            Pair("City Medical Center", "08087654321"),
            Pair("Life Care Hospital", "08011223344")
        )

        hospitals.forEach {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ){

                    Text("🏥 ${it.first}")
                    Text("📞 ${it.second}")

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_DIAL
                        )
                        intent.data = "tel:${it.second}".toUri()
                        context.startActivity(intent)
                    }) {
                        Text("Call")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🌐 ONLINE FEATURE (EXTRA)
        Text("🌐 Find More (Online)", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val uri = "geo:0,0?q=hospitals near me".toUri()
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📍 Open Nearby Hospitals in Map")
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅ Back")
        }
    }
}
// ✅ INSERT DATA (NOW CORRECTLY OUTSIDE)
fun insertData(context: Context) {

    val db = AppDatabase.getDatabase(context)

    CoroutineScope(Dispatchers.IO).launch {

        val dao = db.emergencyDao()

        // 🔥 ALWAYS RESET DATA (FIX FOR YOUR ISSUE)
        dao.deleteAll()

        dao.insertAll(
            listOf(

                    Emergency(
                        title = "Snake Bite",
                        steps = "Keep calm,Immobilize limb,Remove tight items,Go hospital",
                        dos = "Keep patient still,Keep bite below heart level,Seek medical help immediately",
                        donts = "Do not suck venom,Do not cut wound,Do not apply ice"
                    ),

                    Emergency(
                        title = "Burn",
                        steps = "Cool with running water,Remove tight items,Cover with clean cloth",
                        dos = "Use clean cloth,Keep area clean,Seek medical help if severe",
                        donts = "Do not apply ice,Do not burst blisters,Do not apply oil or paste"
                    ),

                    Emergency(
                        title = "Heart Attack",
                        steps = "Call emergency,Let patient sit,Give aspirin if available",
                        dos = "Keep calm,Loosen tight clothes,Call ambulance immediately",
                        donts = "Do not ignore chest pain,Do not make patient walk,Do not delay help"
                    ),

                    Emergency(
                        title = "Fracture",
                        steps = "Immobilize area,Use splint,Go to hospital",
                        dos = "Keep limb still,Support injured area,Apply cold pack gently",
                        donts = "Do not move bone,Do not apply pressure,Do not massage area"
                    ),

                    Emergency(
                        title = "Fainting",
                        steps = "Lay person flat,Raise legs,Loosen clothes,Give fresh air",
                        dos = "Check breathing,Keep airway open,Call help if needed",
                        donts = "Do not give food or water immediately,Do not panic,Do not shake person"
                    ),

                    Emergency(
                        title = "Choking",
                        steps = "Perform back blows,Use Heimlich maneuver,Call emergency",
                        dos = "Act quickly,Encourage coughing if possible,Seek help",
                        donts = "Do not give water,Do not slap forcefully on face,Do not delay action"
                    ),

                    Emergency(
                        title = "Electric Shock",
                        steps = "Switch off power source,Separate victim safely,Call emergency",
                        dos = "Use dry object to separate,Check breathing,Start CPR if needed",
                        donts = "Do not touch directly,Do not use wet hands,Do not ignore burns"
                    ),

                    Emergency(
                        title = "Stroke",
                        steps = "Call emergency,Note time of symptoms,Keep patient resting",
                        dos = "Keep head elevated,Monitor breathing,Seek immediate hospital care",
                        donts = "Do not give food or water,Do not delay treatment,Do not let patient sleep unattended"
                    ),

                    Emergency(
                        title = "Poisoning",
                        steps = "Call emergency,Identify poison if possible,Keep patient calm",
                        dos = "Bring poison container to hospital,Monitor breathing,Seek urgent care",
                        donts = "Do not induce vomiting,Do not give food or drink,Do not delay help"
                    ),

                    Emergency(
                        title = "Nose Bleeding",
                        steps = "Sit upright,Pinch nose,Lean forward",
                        dos = "Apply cold compress,Keep calm,Breathe through mouth",
                        donts = "Do not lie down,Do not tilt head back,Do not blow nose"
                    ),

                    Emergency(
                        title = "Asthma Attack",
                        steps = "Use inhaler,Help sitting position,Call help if severe",
                        dos = "Keep calm,Assist breathing,Use prescribed inhaler",
                        donts = "Do not panic,Do not ignore symptoms,Do not force lying down"
                    ),

                    Emergency(
                        title = "Dog Bite",
                        steps = "Wash wound with soap,Apply clean dressing,Go hospital",
                        dos = "Clean wound thoroughly,Take rabies vaccine,Seek medical care",
                        donts = "Do not ignore wound,Do not close wound tightly,Do not delay treatment"
                    ),

                    Emergency(
                        title = "Heat Stroke",
                        steps = "Move to cool place,Remove excess clothing,Apply cool water",
                        dos = "Give water if conscious,Use fan or shade,Call help",
                        donts = "Do not leave alone,Do not give alcohol,Do not delay cooling"
                    ),

                    Emergency(
                        title = "Cuts & Bleeding",
                        steps = "Apply pressure,Use clean cloth,Elevate wound",
                        dos = "Clean wound,Apply bandage,Seek care if deep",
                        donts = "Do not touch with dirty hands,Do not leave bleeding unattended,Do not use dirty cloth"
                    ),

                    Emergency(
                        title = "Eye Injury",
                        steps = "Do not rub eye,Wash with clean water,Seek hospital",
                        dos = "Keep eye closed,Use clean water,Get medical help",
                        donts = "Do not apply pressure,Do not use drops without advice,Do not rub eye"
                    )
                )
            )
        }
    }
