package com.example.expenceflow.ui.setting

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@Composable
fun ImprovementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        // 🔙 TOP BAR
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Help Improve ExpenseFlow",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Found a bug or have an idea?\nTell me what I can improve 👇",
            fontSize = 14.sp
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Write here…") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                if (text.isBlank()) {
                    Toast.makeText(context, "Please write something", Toast.LENGTH_SHORT).show()
                } else if (!isInternetAvailable(context)) {
                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                } else {
                    val db = FirebaseFirestore.getInstance()

                    val deviceId = getDeviceId(context)

                    val feedback = hashMapOf(
                        "message" to text,
                        "deviceId" to deviceId,
                        "appVersion" to "2.1.0",
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("feedback")
                        .document(deviceId)
                        .set(feedback)
                        .addOnSuccessListener {

                            Toast.makeText(
                                context,
                                "Thanks! Your feedback was sent 🙌",
                                Toast.LENGTH_LONG
                            ).show()

                            text = ""
                        }
                        .addOnFailureListener {

                            Toast.makeText(
                                context,
                                "Failed to send feedback",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        ) {
            Text("Submit")
        }
    }
}

/* ---------------- INTERNET CHECK ---------------- */

fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
}
