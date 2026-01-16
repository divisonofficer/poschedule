package com.jnkim.poschedule.data.repo

import com.jnkim.poschedule.data.local.dao.PlanDao
import com.jnkim.poschedule.data.local.dao.PlanRichDataDao
import com.jnkim.poschedule.data.local.entity.*
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.ui.components.ContactInputState
import com.jnkim.poschedule.ui.components.RichDataState
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val planDao: PlanDao,
    private val richDataDao: PlanRichDataDao
) {
    // --- Instance Management ---
    fun getPlanItems(date: String): Flow<List<PlanItemEntity>> {
        return planDao.getPlanItems(date)
    }

    suspend fun getPlanDay(date: String): PlanDayEntity? {
        return planDao.getPlanDay(date)
    }

    suspend fun insertPlanDay(date: String, mode: Mode) {
        planDao.insertPlanDay(
            PlanDayEntity(
                date = date,
                mode = mode,
                generatedAtMillis = System.currentTimeMillis(),
                sourceVersion = "3.0"
            )
        )
    }

    suspend fun updateItemStatus(id: String, status: String) {
        planDao.updatePlanItemStatus(id, status)
    }

    /**
     * Update item status with snooze timing.
     * Used by NotificationActionReceiver when user taps "Snooze 15 min".
     *
     * @param id Plan item ID
     * @param status Should be "SNOOZED"
     * @param snoozeUntilMillis Timestamp when snooze expires (Instant.toEpochMilli())
     */
    suspend fun updateItemWithSnooze(id: String, status: String, snoozeUntilMillis: Long) {
        planDao.updatePlanItemWithSnooze(id, status, snoozeUntilMillis)
    }

    suspend fun insertPlanItem(item: PlanItemEntity) {
        planDao.insertPlanItem(item)
    }

    suspend fun deleteItem(id: String) {
        planDao.deletePlanItem(id)
    }

    suspend fun deleteItemsBySource(date: String, source: PlanItemSource) {
        planDao.deleteItemsBySource(date, source)
    }

    // --- Slice 2: Deletion Semantics ---
    
    /**
     * "Remove this occurrence" logic. Adds an exception for the date.
     */
    suspend fun removeOccurrence(seriesId: String, date: String) {
        planDao.addException(PlanSeriesExceptionEntity(seriesId = seriesId, date = date))
        // Find and delete the existing instance for this date
        val instanceId = "${seriesId}_$date"
        planDao.deletePlanItem(instanceId)
    }

    /**
     * "Stop repeating" logic. Archives the series and purges future instances.
     */
    suspend fun stopRepeatingSeries(seriesId: String, fromDate: String) {
        planDao.archiveSeries(seriesId)
        planDao.deleteFutureInstances(seriesId, fromDate)
    }

    // --- Series Management ---
    suspend fun getAllActiveSeries(): List<PlanSeriesEntity> {
        return planDao.getAllActiveSeries()
    }

    suspend fun addSeries(series: PlanSeriesEntity) {
        planDao.insertSeries(series)
    }

    suspend fun addManualItem(date: String, title: String, window: PlanItemWindow) {
        val item = PlanItemEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            title = title,
            type = null,
            source = PlanItemSource.MANUAL,
            window = window,
            status = "PENDING",
            isCore = false,
            startTimeMillis = null,
            endTimeMillis = null
        )
        planDao.insertPlanItem(item)
    }

    /**
     * Creates a one-time event with specific date and time.
     * Unlike recurring plans, this creates a single plan item without a series.
     *
     * @param title Event title
     * @param date Date in ISO format (yyyy-MM-dd)
     * @param startHour Hour of day (0-23)
     * @param startMinute Minute of hour (0-59)
     * @param durationMinutes Duration in minutes
     * @param iconEmoji Optional emoji icon for the event
     */
    suspend fun addOneTimeEvent(
        title: String,
        date: String,
        startHour: Int,
        startMinute: Int,
        durationMinutes: Int,
        iconEmoji: String? = null
    ) {
        // Build timestamps using local timezone
        val localDate = java.time.LocalDate.parse(date)
        val startTime = java.time.LocalTime.of(startHour, startMinute)
        val endTime = startTime.plusMinutes(durationMinutes.toLong())

        val zoneId = java.time.ZoneId.systemDefault()
        val startDateTime = java.time.LocalDateTime.of(localDate, startTime)
        val endDateTime = java.time.LocalDateTime.of(localDate, endTime)

        val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()

        android.util.Log.d("PlanRepository",
            "addOneTimeEvent: $title on $date at $startHour:$startMinute | " +
            "startDateTime=$startDateTime, startMillis=$startMillis | " +
            "timezone=${zoneId.id}"
        )

        val item = PlanItemEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            title = title,
            type = null,
            iconEmoji = iconEmoji, // LLM-generated emoji icon
            source = PlanItemSource.MANUAL,
            window = PlanItemWindow.MIDDAY, // Use MIDDAY as default for one-time events
            status = "PENDING",
            isCore = false,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            seriesId = null // One-time events have no series
        )
        planDao.insertPlanItem(item)
    }

    // --- Widget Support ---
    /**
     * Get the next pending task for widget display.
     * Returns the earliest pending task that hasn't been snoozed or has expired snooze.
     *
     * @param date Date to query in ISO format (yyyy-MM-dd)
     * @return Next pending task or null if no tasks are pending
     */
    suspend fun getNextPendingTask(date: String): PlanItemEntity? {
        val now = System.currentTimeMillis()
        return planDao.getNextPendingTask(date, now)
    }

    // --- Rich Data Management (Phase 2) ---

    /**
     * Save rich data for a plan item.
     * Saves to normalized tables: plan_notes, plan_locations, plan_meetings, plan_contacts
     *
     * @param planId Plan item ID
     * @param richData Rich data state containing notes, location, meeting, contacts, tags, color
     */
    suspend fun saveRichData(planId: String, richData: RichDataState) {
        // Save notes (includes tags and color)
        if (richData.notes.isNotBlank() || richData.tags.isNotBlank() || richData.colorTag != null) {
            richDataDao.insertNote(
                PlanNoteEntity(
                    planId = planId,
                    notes = richData.notes.takeIf { it.isNotBlank() },
                    tags = richData.tags.takeIf { it.isNotBlank() },
                    colorTag = richData.colorTag
                )
            )
        }

        // Save location
        if (richData.locationText.isNotBlank()) {
            richDataDao.insertLocation(
                PlanLocationEntity(
                    planId = planId,
                    locationText = richData.locationText,
                    mapQuery = richData.mapQuery.takeIf { it.isNotBlank() }
                )
            )
        }

        // Save meeting
        if (richData.meetingUrl.isNotBlank() && richData.meetingType != null) {
            richDataDao.insertMeeting(
                PlanMeetingEntity(
                    planId = planId,
                    joinUrl = richData.meetingUrl,
                    meetingType = richData.meetingType
                )
            )
        }

        // Save contacts
        richData.contacts
            .filter { it.name.isNotBlank() || it.email.isNotBlank() || it.phoneNumber.isNotBlank() }
            .forEach { contact ->
                richDataDao.insertContact(
                    PlanContactEntity(
                        id = contact.id,
                        planId = planId,
                        name = contact.name.takeIf { it.isNotBlank() },
                        email = contact.email.takeIf { it.isNotBlank() },
                        phoneNumber = contact.phoneNumber.takeIf { it.isNotBlank() },
                        role = contact.role.takeIf { it.isNotBlank() }
                    )
                )
            }
    }

    /**
     * Load rich data for a plan item.
     * Queries all rich data tables and returns a RichDataState object.
     *
     * @param planId Plan item ID
     * @return RichDataState with loaded data, or empty state if no rich data exists
     */
    suspend fun loadRichData(planId: String): RichDataState {
        val richData = richDataDao.getPlanWithRichData(planId) ?: return RichDataState()

        return RichDataState(
            notes = richData.note?.notes ?: "",
            locationText = richData.location?.locationText ?: "",
            mapQuery = richData.location?.mapQuery ?: "",
            meetingUrl = richData.meeting?.joinUrl ?: "",
            meetingType = richData.meeting?.meetingType,
            contacts = richData.contacts.map {
                ContactInputState(
                    id = it.id,
                    name = it.name ?: "",
                    email = it.email ?: "",
                    phoneNumber = it.phoneNumber ?: "",
                    role = it.role ?: ""
                )
            },
            tags = richData.note?.tags ?: "",
            colorTag = richData.note?.colorTag
        )
    }

    /**
     * Update rich data for a plan item.
     * Deletes existing rich data and saves new data.
     *
     * @param planId Plan item ID
     * @param richData New rich data state
     */
    suspend fun updateRichData(planId: String, richData: RichDataState) {
        // Delete existing rich data
        richDataDao.deleteAllRichDataForPlan(planId)

        // Save new data
        saveRichData(planId, richData)
    }
}
