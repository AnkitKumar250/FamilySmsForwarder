package com.familysms.forwarder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familysms.forwarder.domain.ForwardStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardLogDao {

    @Query("SELECT * FROM forward_logs ORDER BY receivedAt DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int = 100): Flow<List<ForwardLogEntity>>

    @Query("SELECT * FROM forward_logs WHERE status = 'FAILED' ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentErrors(limit: Int = 20): Flow<List<ForwardLogEntity>>

    @Query("SELECT * FROM forward_logs WHERE id = :logId LIMIT 1")
    suspend fun getLogById(logId: Long): ForwardLogEntity?

    @Query(
        """
        SELECT COUNT(*) FROM forward_logs
        WHERE dedupeKey = :dedupeKey
          AND receivedAt >= :sinceTimestamp
          AND status != 'FAILED'
          AND status != 'SKIPPED_DUPLICATE'
        """
    )
    suspend fun countActiveDuplicates(dedupeKey: String, sinceTimestamp: Long): Int

    @Query("SELECT * FROM forward_logs WHERE status = 'QUEUED' ORDER BY receivedAt ASC")
    suspend fun getQueuedLogs(): List<ForwardLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ForwardLogEntity): Long

    @Query(
        """
        UPDATE forward_logs
        SET status = :status,
            totalParts = :totalParts,
            errorMessage = :errorMessage,
            updatedAt = :updatedAt
        WHERE id = :logId
        """
    )
    suspend fun updateDispatchState(
        logId: Long,
        status: ForwardStatus,
        totalParts: Int,
        errorMessage: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE forward_logs
        SET status = :status,
            sentPartsCount = :sentPartsCount,
            deliveredPartsCount = :deliveredPartsCount,
            errorMessage = :errorMessage,
            updatedAt = :updatedAt
        WHERE id = :logId
        """
    )
    suspend fun updatePartProgress(
        logId: Long,
        status: ForwardStatus,
        sentPartsCount: Int,
        deliveredPartsCount: Int,
        errorMessage: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM forward_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)

    @Query("DELETE FROM forward_logs")
    suspend fun clearAllLogs()
}
