package com.example.expenceflow.data.auto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenceflow.ui.EditTransactionDialog
import com.example.expenceflow.data.db.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionBottomSheet(
    viewModel: DetectionViewModel,
    onDismiss: () -> Unit
) {
    val candidates by viewModel.pendingCandidates.collectAsState()
    var editingCandidate by remember { mutableStateOf<TransactionCandidate?>(null) }

    if (editingCandidate != null) {
        val candidate = editingCandidate!!
        EditTransactionDialog(
            transaction = Transaction(
                title = candidate.merchant,
                amount = candidate.amount,
                date = candidate.date,
                type = candidate.type,
                category = candidate.category,
                account = candidate.account
            ),
            onDismiss = { editingCandidate = null },
            onSave = { updatedTx ->
                viewModel.confirmCandidate(TransactionCandidate(
                    amount = updatedTx.amount,
                    merchant = updatedTx.title,
                    category = updatedTx.category,
                    date = updatedTx.date,
                    account = updatedTx.account,
                    type = updatedTx.type,
                    notes = "",
                    confidence = 1.0f,
                    source = candidate.source
                ))
                editingCandidate = null
            }
        )
    }

    if (candidates.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Detected Transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(candidates) { candidate ->
                        CandidateItem(
                            candidate = candidate,
                            onConfirm = { viewModel.confirmCandidate(candidate) },
                            onEdit = { editingCandidate = candidate },
                            onDismiss = { viewModel.dismissCandidate(candidate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateItem(
    candidate: TransactionCandidate,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.merchant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "₹${candidate.amount} • ${candidate.category} • ${candidate.source}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Red)
            }
            
            IconButton(onClick = onConfirm) {
                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.Green)
            }
        }
    }
}
