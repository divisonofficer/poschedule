package com.jnkim.poschedule.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jnkim.poschedule.ui.components.ContactInputState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper object for Android contacts integration
 *
 * Provides utilities to:
 * - Check READ_CONTACTS permission
 * - Request READ_CONTACTS permission
 * - Load contacts from device (name, email, phone)
 */
object ContactPickerHelper {

    const val CONTACTS_PERMISSION_REQUEST_CODE = 1001

    /**
     * Check if READ_CONTACTS permission is granted
     */
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request contacts permission from user
     *
     * Note: This should be called from an Activity context.
     * Result will be delivered to Activity.onRequestPermissionsResult()
     */
    fun requestContactsPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            CONTACTS_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Load all contacts from device with name, email, and phone
     *
     * Returns a list of ContactInputState objects populated from device contacts.
     * Only contacts with at least a name are included.
     *
     * This is a suspend function that runs on IO dispatcher.
     *
     * @return List of contacts, or empty list if permission denied
     */
    suspend fun loadContacts(context: Context): List<ContactInputState> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission(context)) {
            return@withContext emptyList()
        }

        val contacts = mutableListOf<ContactInputState>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)

                    // Skip contacts without names
                    if (name.isNullOrBlank()) continue

                    // Get email
                    val email = getContactEmail(context, id)

                    // Get phone
                    val phone = getContactPhone(context, id)

                    contacts.add(
                        ContactInputState(
                            name = name,
                            email = email ?: "",
                            phoneNumber = phone ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("ContactPickerHelper", "Error loading contacts", e)
        }

        return@withContext contacts
    }

    /**
     * Get email address for a contact
     */
    private fun getContactEmail(context: Context, contactId: String): String? {
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    return cursor.getString(emailIndex)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactPickerHelper", "Error loading email for contact $contactId", e)
        }
        return null
    }

    /**
     * Get phone number for a contact
     */
    private fun getContactPhone(context: Context, contactId: String): String? {
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    return cursor.getString(phoneIndex)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactPickerHelper", "Error loading phone for contact $contactId", e)
        }
        return null
    }
}
