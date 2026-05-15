package com.yourname.pdftoolkit.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FamilyPDF") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "FamilyPDF",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Made with ❤️ by RR",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "For my family",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Big friendly buttons
            Button(onClick = { navController.navigate("pdf_viewer") }, modifier = Modifier.fillMaxWidth()) {
                Text("📄 Open a PDF", style = MaterialTheme.typography.titleMedium)
            }

            Button(onClick = { navController.navigate("tools") }, modifier = Modifier.fillMaxWidth()) {
                Text("🛠️ All Tools", style = MaterialTheme.typography.titleMedium)
            }

            Button(onClick = { navController.navigate("sign") }, modifier = Modifier.fillMaxWidth()) {
                Text("✍️ Sign Document", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}