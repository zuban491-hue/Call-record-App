package com.callcapture.app.data.messages

import androidx.core.app.NotificationCompat
import com.callcapture.app.data.legal.LegalConsent
import android.app.Notification
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.Settings
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest

/** Local, opt-in copies of notification text. Never queries another app's files. */
class MessageHistoryStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("message_history", Context.MODE_PRIVATE)
    private val helper = object : SQLiteOpenHelper(app,
        File(app.noBackupFilesDir, "message_history.db").absolutePath, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_SQL)
            db.execSQL("CREATE TABLE deleted(id TEXT PRIMARY KEY, removed INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX history_time ON history(received DESC)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
    private val revision = MutableStateFlow(0L)
    val changes = revision.asStateFlow()
    val enabled: Boolean get() = prefs.getBoolean("enabled", false)
    fun selected(pkg: String) = prefs.getBoolean(pkg, true)

    @Synchronized fun setEnabled(value: Boolean) {
        if (value) {
            check(LegalConsent.accepted(app)) { "Review the Terms of Use first." }
            LegalConsent.recordFeature(app, "message_history")
        }
        prefs.edit().putBoolean("enabled", value)
            .putLong("since", System.currentTimeMillis()).commit()
        revision.value += 1
    }
    @Synchronized fun select(pkg: String, value: Boolean) {
        require(pkg in APPS)
        prefs.edit().putBoolean(pkg, value).putLong("since:$pkg", System.currentTimeMillis()).commit()
        revision.value += 1
    }
    fun hasNotificationAccess(): Boolean {
        val component = ComponentName(app, "com.callcapture.app.service.VoipNotificationListenerService")
        return Settings.Secure.getString(app.contentResolver, "enabled_notification_listeners")
            ?.split(':')?.any { ComponentName.unflattenFromString(it) == component } == true
    }

    // Invoked off the main thread. Eligibility is checked again inside the lock so pause is effective.
    @Synchronized fun capture(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (com.callcapture.app.data.legal.LocalDataControls.deleting || !LegalConsent.accepted(app) || !enabled || pkg !in APPS || !selected(pkg)) return
        val since = maxOf(prefs.getLong("since", Long.MAX_VALUE), prefs.getLong("since:$pkg", 0L))
        if (sbn.postTime < since) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0 || n.category == Notification.CATEGORY_CALL) return
        val extras = n.extras
        val messages = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
            ?.messages.orEmpty()
        if (n.category != Notification.CATEGORY_MESSAGE && messages.isEmpty()) return
        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: APPS.getValue(pkg)
        val candidates = if (messages.isNotEmpty()) messages.mapNotNull { m ->
            val body = m.text?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            // Do not backfill earlier conversation messages bundled with a new notification.
            if (m.timestamp < since) return@mapNotNull null
            @Suppress("DEPRECATION")
            Entry("", pkg, title.take(300), m.sender?.toString()?.take(300) ?: title.take(300),
                body.take(12000), m.timestamp, sbn.postTime)
        } else {
            val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            if (body.isBlank()) emptyList() else listOf(Entry("", pkg, title.take(300), title.take(300),
                body.take(12000), sbn.postTime, sbn.postTime))
        }
        if (candidates.isEmpty()) return
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            for (entry in candidates) {
                // Key + message timestamp preserves repeated messages but ignores notification re-posts.
                val identity = listOf(sbn.key, entry.sent.toString(), entry.sender, entry.body).joinToString("\u0000")
                val id = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it.toInt() and 255) }
                val wasDeleted = db.rawQuery("SELECT 1 FROM deleted WHERE id = ?", arrayOf(id)).use { it.moveToFirst() }
                if (wasDeleted) continue
                val values = ContentValues().apply {
                    put("id", id); put("pkg", pkg); put("title", entry.title); put("sender", entry.sender)
                    put("body", entry.body); put("sent", entry.sent); put("received", entry.received)
                }
                db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            prune(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        revision.value += 1
    }
    private fun prune(db: SQLiteDatabase) {
        db.delete("history", "received < ?", arrayOf((System.currentTimeMillis() - RETENTION_MS).toString()))
        db.delete("deleted", "removed < ?", arrayOf((System.currentTimeMillis() - RETENTION_MS).toString()))
        db.execSQL(CAP_SQL)
    }
    @Synchronized fun load(): List<Entry> {
        val db = helper.writableDatabase
        prune(db)
        return db.query("history", null, null, null, null, null, "received DESC", "5000").use { c ->
            buildList {
                while (c.moveToNext()) {
                    fun text(name: String) = c.getString(c.getColumnIndexOrThrow(name))
                    fun time(name: String) = c.getLong(c.getColumnIndexOrThrow(name))
                    add(Entry(text("id"), text("pkg"), text("title"), text("sender"), text("body"), time("sent"), time("received")))
                }
            }
        }
    }
    @Synchronized fun delete(id: String) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("deleted", null, ContentValues().apply {
                put("id", id); put("removed", System.currentTimeMillis())
            }, SQLiteDatabase.CONFLICT_IGNORE)
            db.delete("history", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        revision.value += 1
    }
    @Synchronized fun count(): Int = helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM history", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }
    @Synchronized fun clear() {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("history", null, null)
            db.delete("deleted", null, null)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        // Prevent an in-flight old callback from putting cleared history back.
        prefs.edit().putLong("since", System.currentTimeMillis()).commit()
        revision.value += 1
    }
    data class Entry(val id: String, val pkg: String, val title: String, val sender: String,
        val body: String, val sent: Long, val received: Long)
    companion object {
        val APPS = linkedMapOf("com.whatsapp" to "WhatsApp", "com.whatsapp.w4b" to "WhatsApp Business",
            "com.facebook.orca" to "Messenger", "com.facebook.mlite" to "Messenger Lite")
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        const val CREATE_SQL = "CREATE TABLE history(id TEXT PRIMARY KEY, pkg TEXT NOT NULL, title TEXT NOT NULL, sender TEXT NOT NULL, body TEXT NOT NULL, sent INTEGER NOT NULL, received INTEGER NOT NULL)"
        const val CAP_SQL = "DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY received DESC LIMIT 5000)"
        @Volatile private var instance: MessageHistoryStore? = null
        fun get(context: Context): MessageHistoryStore = instance ?: synchronized(this) {
            instance ?: MessageHistoryStore(context).also { instance = it }
        }
    }
}
