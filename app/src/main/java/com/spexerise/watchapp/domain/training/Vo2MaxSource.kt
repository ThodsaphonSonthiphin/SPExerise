package com.spexerise.watchapp.domain.training

import com.spexerise.watchapp.data.db.Vo2MaxDao

object Vo2MaxSource {
    const val FALLBACK = 40f
    private const val MAX_AGE_MS = 90L * 24 * 60 * 60 * 1000

    fun fromDao(dao: Vo2MaxDao): Float {
        val record = dao.getLatest() ?: return FALLBACK
        val age = System.currentTimeMillis() - record.measuredAtMs
        return if (age <= MAX_AGE_MS) record.vo2Max else FALLBACK
    }
}
