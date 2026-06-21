package com.spexerise.watchapp.domain.training

import com.google.common.truth.Truth.assertThat
import com.spexerise.watchapp.data.db.Vo2MaxDao
import com.spexerise.watchapp.data.db.Vo2MaxRecord
import org.junit.Test

class Vo2MaxSourceTest {

    private fun daoReturning(record: Vo2MaxRecord?) = object : Vo2MaxDao {
        override fun insert(record: Vo2MaxRecord) {}
        override fun getLatest(): Vo2MaxRecord? = record
    }

    @Test fun `returns fallback when no record in db`() {
        val result = Vo2MaxSource.fromDao(daoReturning(null))
        assertThat(result).isEqualTo(Vo2MaxSource.FALLBACK)
    }

    @Test fun `returns stored value for fresh record`() {
        val record = Vo2MaxRecord(
            measuredAtMs = System.currentTimeMillis(),
            vo2Max = 48.5f,
            source = "samsung_health"
        )
        val result = Vo2MaxSource.fromDao(daoReturning(record))
        assertThat(result).isEqualTo(48.5f)
    }

    @Test fun `returns fallback for record older than 90 days`() {
        val ninetyOneDays = 91L * 24 * 60 * 60 * 1000
        val record = Vo2MaxRecord(
            measuredAtMs = System.currentTimeMillis() - ninetyOneDays,
            vo2Max = 48.5f,
            source = "calibration"
        )
        val result = Vo2MaxSource.fromDao(daoReturning(record))
        assertThat(result).isEqualTo(Vo2MaxSource.FALLBACK)
    }

    @Test fun `returns stored value for record exactly at 90 days`() {
        val ninetyDays = 90L * 24 * 60 * 60 * 1000
        val record = Vo2MaxRecord(
            measuredAtMs = System.currentTimeMillis() - ninetyDays,
            vo2Max = 52f,
            source = "manual"
        )
        val result = Vo2MaxSource.fromDao(daoReturning(record))
        assertThat(result).isEqualTo(52f)
    }

    @Test fun `fallback constant is 40`() {
        assertThat(Vo2MaxSource.FALLBACK).isEqualTo(40f)
    }
}
