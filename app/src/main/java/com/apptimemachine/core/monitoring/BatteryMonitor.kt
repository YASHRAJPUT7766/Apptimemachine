package com.apptimemachine.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.apptimemachine.data.entities.BatteryEventType
import com.apptimemachine.data.entities.BatteryHistoryEntity
import com.apptimemachine.data.entities.ChargingMethod
import com.apptimemachine.data.repository.BatteryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Part 2.4 Battery Monitoring Engine — actually persists charging
 * sessions, unlike the earlier stub receiver. Registered as a dynamic
 * (non-manifest) receiver from [AppTimeMachineApplication] so it can be
 * a proper Hilt @Singleton with an injected repository, since manifest
 * receivers can't easily receive @Inject fields without extra Hilt
 * entry-point boilerplate.
 *
 * Per Part 2.4 Android Limitation: this only tracks device-level charging
 * sessions (start/end, battery %, temperature, method). It never attempts
 * per-app battery attribution — Android doesn't expose that to third-party
 * apps, and the spec explicitly forbids estimating it.
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryRepository: BatteryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastKnownPercent: Int? = null
    private var lastKnownTemperature: Float? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                Intent.ACTION_POWER_CONNECTED -> handlePowerConnected(intent)
                Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            lastKnownPercent = (level * 100) / scale
        }
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        if (tempTenths != Int.MIN_VALUE) {
            lastKnownTemperature = tempTenths / 10f
        }
    }

    private fun handlePowerConnected(intent: Intent) {
        scope.launch {
            // Avoid duplicate open sessions if we somehow get two CONNECTED
            // broadcasts in a row (Part 1.3 Duplicate Event Prevention).
            if (batteryRepository.getOpenSession() != null) return@launch

            val method = intent.chargingMethodFromExtras()
            batteryRepository.insert(
                BatteryHistoryEntity(
                    eventType = BatteryEventType.CHARGING_STARTED,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    batteryStartPercent = lastKnownPercent,
                    batteryEndPercent = null,
                    chargingMethod = method,
                    averageTemperature = lastKnownTemperature,
                    averageVoltage = null
                )
            )
        }
    }

    private fun handlePowerDisconnected() {
        scope.launch {
            val openSession = batteryRepository.getOpenSession() ?: return@launch
            val now = System.currentTimeMillis()
            batteryRepository.update(
                openSession.copy(
                    eventType = BatteryEventType.CHARGING_STOPPED,
                    endTime = now,
                    batteryEndPercent = lastKnownPercent
                )
            )
        }
    }
}
