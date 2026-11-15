package com.familysms.forwarder.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderRuleDao {

    @Query("SELECT * FROM sender_rules ORDER BY createdAt DESC")
    fun observeAllRules(): Flow<List<SenderRuleEntity>>

    @Query("SELECT * FROM sender_rules WHERE isEnabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledRules(): List<SenderRuleEntity>

    @Query("SELECT * FROM sender_rules")
    suspend fun getAllRules(): List<SenderRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SenderRuleEntity): Long

    @Update
    suspend fun updateRule(rule: SenderRuleEntity)

    @Query("UPDATE sender_rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)

    @Delete
    suspend fun deleteRule(rule: SenderRuleEntity)

    @Query("DELETE FROM sender_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)
}
