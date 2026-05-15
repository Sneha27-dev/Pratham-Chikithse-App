package com.example.prathamchikithse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = "About Pratham Chikithse",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "🚑 Emergency First Aid App",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text =
                        "Pratham Chikithse is a first aid emergency application designed to help users during emergency situations like snake bite, burns, fractures, choking, heart attack and more."
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text =
                        "Features:\n\n" +
                                "• Emergency Guides\n" +
                                "• Voice Search\n" +
                                "• Dark Mode\n" +
                                "• Emergency Contacts\n" +
                                "• Nearby Hospitals\n" +
                                "• Text To Speech Instructions\n" +
                                "• Firebase Integration"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Developed using Kotlin + Jetpack Compose + Firebase",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}