package com.example.colocate.ble

class DistanceManager {
    private val rssiUnknown = 0 // "unknown"
    private val rssi1m = -46 // "immediate"
    private val rssi3m = -60 // "near"
    private val rssi5m = -65 // "semi-near"
    // lower = "far"

    private val maxReadings = 5
    private var readings = mutableMapOf<String, List<Int>>()
    private var ranges = mutableMapOf<String, String>()

    fun addDistance(remoteId: String, rssi: Int): String {
        if (rssi == 0) {
            return ranges.getOrDefault(remoteId, "unknown")
        }

        var currentReadings = readings.getOrDefault(remoteId, emptyList()).plus(rssi)
        if (currentReadings.size > maxReadings) {
            currentReadings = currentReadings.drop(1)
        }
        readings[remoteId] = currentReadings

        val average = currentReadings.sum() / currentReadings.size
        val currentRange = when {
            average == rssiUnknown -> "unknown"
            average >= rssi1m -> "immediate"
            average >= rssi3m -> "near"
            average >= rssi5m -> "semi-near"
            else -> "far"
        }

        ranges[remoteId] = currentRange
        return ranges.getOrDefault(remoteId, "unknown")
    }

    fun removeAll(except: List<String>) {
        readings
            .keys
            .minus(except)
            .forEach {
                readings.remove(it)
                ranges.remove(it)
            }
    }

    fun detected(): Map<String, String> = ranges

}