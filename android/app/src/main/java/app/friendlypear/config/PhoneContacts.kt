package app.friendlypear.config

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Data
import app.friendlypear.core.model.Contact
import app.friendlypear.core.voice.RelationWords

/**
 * The phone's contact book, read on the phone and never sent anywhere. "Laura hija" becomes Laura,
 * a child; nicknames ("el chico") become aliases. The family setup task will add guardians on top.
 */
object PhoneContacts {
    fun load(context: Context): List<Contact> {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val resolver = context.contentResolver
        val names = LinkedHashMap<String, String>()
        val phones = HashMap<String, MutableList<String>>()
        resolver.query(Phone.CONTENT_URI, arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                names.getOrPut(id) { c.getString(1).orEmpty() }
                c.getString(2)?.let { phones.getOrPut(id) { mutableListOf() } += it }
            }
        }
        val nicknames = HashMap<String, MutableList<String>>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data.CONTACT_ID, Nickname.NAME),
            "${Data.MIMETYPE} = ?",
            arrayOf(Nickname.CONTENT_ITEM_TYPE),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                c.getString(1)?.let { nicknames.getOrPut(id) { mutableListOf() } += it }
            }
        }
        return names.map { (id, saved) ->
            val relation = RelationWords.inSavedName(saved)
            Contact(
                id = id,
                name = relation?.second ?: saved,
                phones = phones[id].orEmpty().distinct(),
                relation = relation?.first,
                aliases = nicknames[id].orEmpty() + listOfNotNull(saved.takeIf { relation != null }),
            )
        }
    }
}
