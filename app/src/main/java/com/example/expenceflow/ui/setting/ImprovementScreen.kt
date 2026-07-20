package com.example.expenceflow.ui.setting

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                text = "Help us improve",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Found a bug or have an idea? Tell us what we can improve to make your experience better.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write your feedback here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = text.isNotBlank() && !isSending,
                onClick = {
                    if (!isInternetAvailable(context)) {
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isSending = true
                    val db = FirebaseFirestore.getInstance()
                    val deviceId = getDeviceId(context)

                    val feedback = hashMapOf(
                        "message" to text,
                        "deviceId" to deviceId,
                        "appVersion" to "1.0.0",
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("feedback")
                        .document(deviceId + "_" + System.currentTimeMillis())
                        .set(feedback)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Feedback sent! Thanks! 🙌", Toast.LENGTH_LONG).show()
                            text = ""
                            isSending = false
                            onBack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to send feedback", Toast.LENGTH_SHORT).show()
                            isSending = false
                        }
                }
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Send Feedback", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
}
