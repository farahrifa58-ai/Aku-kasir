package com.example.util

import java.util.Calendar

enum class BackupHealthStatus {
    /** Backed up today AND no new transactions/changes since last backup (Green). */
    SAFE,

    /** Over 24h since last backup (<= 3 days), not backed up today, OR new data since last backup (Yellow). */
    NEEDS_BACKUP,

    /** Never backed up (lastBackupAt == 0) OR more than 3 days since last backup (Red). */
    NEVER_OR_OVERDUE
}

object BackupStatusHelper {
    const val ONE_DAY_MILLIS: Long = 24L * 60L * 60L * 1000L
    const val THREE_DAYS_MILLIS: Long = 3L * 24L * 60L * 60L * 1000L

    fun resolveEffectiveLastBackupAt(
        prefsLastBackupAt: Long,
        storeLastBackupAtMillis: Long?
    ): Long {
        return maxOf(prefsLastBackupAt.coerceAtLeast(0L), (storeLastBackupAtMillis ?: 0L).coerceAtLeast(0L))
    }

    fun evaluateStatus(
        lastBackupAtMillis: Long,
        hasNewDataSinceBackup: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): BackupHealthStatus {
        if (lastBackupAtMillis <= 0L) {
            return BackupHealthStatus.NEVER_OR_OVERDUE
        }
        val elapsedMillis = (nowMillis - lastBackupAtMillis).coerceAtLeast(0L)
        if (elapsedMillis > THREE_DAYS_MILLIS) {
            return BackupHealthStatus.NEVER_OR_OVERDUE
        }
        if (elapsedMillis > ONE_DAY_MILLIS || hasNewDataSinceBackup || !isSameCalendarDay(lastBackupAtMillis, nowMillis)) {
            return BackupHealthStatus.NEEDS_BACKUP
        }
        return BackupHealthStatus.SAFE
    }

    fun formatRelativeElapsed(
        lastBackupAtMillis: Long,
        language: String = "id",
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val elapsedMillis = (nowMillis - lastBackupAtMillis).coerceAtLeast(0L)
        val minutes = elapsedMillis / (60L * 1000L)
        val hours = elapsedMillis / (60L * 60L * 1000L)
        val days = elapsedMillis / ONE_DAY_MILLIS

        return if (language == "en") {
            when {
                minutes < 1L -> "just now"
                minutes < 60L -> "$minutes min ago"
                hours < 24L -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
                else -> if (days == 1L) "1 day ago" else "$days days ago"
            }
        } else {
            when {
                minutes < 1L -> "baru saja"
                minutes < 60L -> "$minutes menit yang lalu"
                hours < 24L -> "$hours jam yang lalu"
                else -> "$days hari yang lalu"
            }
        }
    }

    private fun isSameCalendarDay(firstMillis: Long, secondMillis: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val c2 = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
