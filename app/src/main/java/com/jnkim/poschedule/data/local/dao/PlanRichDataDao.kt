package com.jnkim.poschedule.data.local.dao

import androidx.room.*
import com.jnkim.poschedule.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * PlanRichDataDao - Single DAO for all rich data entities
 *
 * Design: Centralized access to plan meetings, locations, contacts, and notes
 * Phase 1.5: Separated rich data tables for unlimited extensibility
 */
@Dao
interface PlanRichDataDao {

    // --- Plan Meetings ---

    @Query("SELECT * FROM plan_meetings WHERE planId = :planId")
    suspend fun getMeeting(planId: String): PlanMeetingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: PlanMeetingEntity)

    @Delete
    suspend fun deleteMeeting(meeting: PlanMeetingEntity)

    @Query("DELETE FROM plan_meetings WHERE planId = :planId")
    suspend fun deleteMeetingByPlanId(planId: String)

    // --- Plan Locations ---

    @Query("SELECT * FROM plan_locations WHERE planId = :planId")
    suspend fun getLocation(planId: String): PlanLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: PlanLocationEntity)

    @Delete
    suspend fun deleteLocation(location: PlanLocationEntity)

    @Query("DELETE FROM plan_locations WHERE planId = :planId")
    suspend fun deleteLocationByPlanId(planId: String)

    // --- Plan Contacts ---

    @Query("SELECT * FROM plan_contacts WHERE planId = :planId")
    suspend fun getContacts(planId: String): List<PlanContactEntity>

    @Query("SELECT * FROM plan_contacts WHERE planId = :planId")
    fun getContactsFlow(planId: String): Flow<List<PlanContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: PlanContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<PlanContactEntity>)

    @Delete
    suspend fun deleteContact(contact: PlanContactEntity)

    @Query("DELETE FROM plan_contacts WHERE planId = :planId")
    suspend fun deleteContactsByPlanId(planId: String)

    // --- Plan Notes ---

    @Query("SELECT * FROM plan_notes WHERE planId = :planId")
    suspend fun getNote(planId: String): PlanNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PlanNoteEntity)

    @Delete
    suspend fun deleteNote(note: PlanNoteEntity)

    @Query("DELETE FROM plan_notes WHERE planId = :planId")
    suspend fun deleteNoteByPlanId(planId: String)

    // --- Batch Operations ---

    /**
     * Get all rich data for a plan in one query.
     * Returns a PlanWithRichData object containing the plan and all related data.
     */
    @Transaction
    @Query("SELECT * FROM plan_items WHERE id = :planId")
    suspend fun getPlanWithRichData(planId: String): PlanWithRichData?

    /**
     * Delete all rich data for a plan (meetings, locations, contacts, notes).
     * Used when deleting a plan item.
     */
    @Transaction
    suspend fun deleteAllRichDataForPlan(planId: String) {
        deleteMeetingByPlanId(planId)
        deleteLocationByPlanId(planId)
        deleteContactsByPlanId(planId)
        deleteNoteByPlanId(planId)
    }
}

/**
 * PlanWithRichData - Embedded object containing plan + all rich data
 *
 * Used for efficient querying with @Transaction
 */
data class PlanWithRichData(
    @Embedded val plan: PlanItemEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val meeting: PlanMeetingEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val location: PlanLocationEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val contacts: List<PlanContactEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val note: PlanNoteEntity?
)
