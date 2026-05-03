/* Copyright (c) 2026 Jatin Kumar <jnkr2409@gmail.com>
* This program is free software; you can redistribute it and/or modify it under        *
* the terms of the GNU General Public License as published by the Free Software        *
* Foundation; either version 3 of the License, or (at your option) any later           *
* version.                                                                             *
* *
* This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
* PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
* *
* You should have received a copy of the GNU General Public License along with         *
* this program.  If not, see <http://www.gnu.org/licenses/>.                           */

package com.ichi2.anki.android
import android.content.Context
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.squareup.seismic.ShakeDetector
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/*
 * Wrapper for the Seismic ShakeDetector to provide a cooldown mechanism.
 * This prevents the "Undo" action or other gestures from triggering multiple times
 * in rapid succession during a single physical shake event.
 */
class AnkiShakeDetector(
    private val sensorManager: SensorManager?,
    private val sensorDelay: Int,
    private val listener: ShakeDetector.Listener,
    /*
     * Through trial and error, 800ms was found to be the best timing.
     *
     * - It is fast enough so you can quickly turn a flag on and off.
     * - It is slow enough to stop a single shake from triggering twice.
     * - It also gives the app enough time to open a new screen (like "Add Note")
     *   before another shake can happen.
     */
    private val cooldown: Duration = 800.milliseconds,
) : ShakeDetector.Listener {
    private val shakeDetector = ShakeDetector(this)
    private var lastShakeTime = 0L

    companion object {
        @JvmStatic
        fun createInstance(
            context: Context,
            listener: ShakeDetector.Listener,
        ): AnkiShakeDetector {
            val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
            return AnkiShakeDetector(
                sensorManager = sensorManager,
                sensorDelay = SensorManager.SENSOR_DELAY_UI,
                listener = listener,
            )
        }
    }

    fun start() {
        sensorManager?.let {
            shakeDetector.start(it, sensorDelay)
        }
    }

    fun stop() {
        shakeDetector.stop()
    }

    override fun hearShake() {
        Timber.d("The time since the last shake was: ${SystemClock.elapsedRealtime() - lastShakeTime}")
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastShakeTime < cooldown.inWholeMilliseconds) {
            return
        }
        try {
            listener.hearShake()
        } finally {
            lastShakeTime = SystemClock.elapsedRealtime()
        }
    }
}
