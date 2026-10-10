package com.example.expenceflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.expenceflow.data.db.DetectionDiagnostic
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDiagnosticDao {
    @Insert
    suspend fun insertDiagnostic(diagnostic: DetectionDiagnostic)

    @Query("SELECT * FROM detection_diagnostics ORDER BY timestamp DESC LIMIT 100")
    fun getRecentDiagnostics(): Flow<List<DetectionDiagnostic>>

    @Query("DELETE FROM detection_diagnostics WHERE id NOT IN (SELECT id FROM detection_diagnostics ORDER BY timestamp DESC LIMIT 100)")
    suspend fun purgeOldDiagnostics()
}
