package com.oracle.mrt3.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.STATION_NAMES

object OfflineModeHelper {

    val FARE_MATRIX: Map<Pair<String, String>, Double> = mapOf(
        // North Avenue
        "North Avenue" to "Quezon Avenue"         to 6.0,
        "North Avenue" to "GMA Kamuning"          to 6.0,
        "North Avenue" to "Araneta Center-Cubao"  to 8.0,
        "North Avenue" to "Santolan-Annapolis"    to 8.0,
        "North Avenue" to "Ortigas"               to 10.0,
        "North Avenue" to "Shaw Boulevard"        to 10.0,
        "North Avenue" to "Boni"                  to 10.0,
        "North Avenue" to "Guadalupe"             to 12.0,
        "North Avenue" to "Buendia"               to 12.0,
        "North Avenue" to "Ayala"                 to 12.0,
        "North Avenue" to "Magallanes"            to 14.0,
        "North Avenue" to "Taft Avenue"           to 14.0,
        // Quezon Avenue
        "Quezon Avenue" to "North Avenue"         to 6.0,
        "Quezon Avenue" to "GMA Kamuning"         to 6.0,
        "Quezon Avenue" to "Araneta Center-Cubao" to 6.0,
        "Quezon Avenue" to "Santolan-Annapolis"   to 8.0,
        "Quezon Avenue" to "Ortigas"              to 8.0,
        "Quezon Avenue" to "Shaw Boulevard"       to 10.0,
        "Quezon Avenue" to "Boni"                 to 10.0,
        "Quezon Avenue" to "Guadalupe"            to 10.0,
        "Quezon Avenue" to "Buendia"              to 12.0,
        "Quezon Avenue" to "Ayala"                to 12.0,
        "Quezon Avenue" to "Magallanes"           to 12.0,
        "Quezon Avenue" to "Taft Avenue"          to 14.0,
        // GMA Kamuning
        "GMA Kamuning" to "North Avenue"          to 6.0,
        "GMA Kamuning" to "Quezon Avenue"         to 6.0,
        "GMA Kamuning" to "Araneta Center-Cubao"  to 6.0,
        "GMA Kamuning" to "Santolan-Annapolis"    to 6.0,
        "GMA Kamuning" to "Ortigas"               to 8.0,
        "GMA Kamuning" to "Shaw Boulevard"        to 8.0,
        "GMA Kamuning" to "Boni"                  to 10.0,
        "GMA Kamuning" to "Guadalupe"             to 10.0,
        "GMA Kamuning" to "Buendia"               to 10.0,
        "GMA Kamuning" to "Ayala"                 to 12.0,
        "GMA Kamuning" to "Magallanes"            to 12.0,
        "GMA Kamuning" to "Taft Avenue"           to 12.0,
        // Araneta Center-Cubao
        "Araneta Center-Cubao" to "North Avenue"          to 8.0,
        "Araneta Center-Cubao" to "Quezon Avenue"         to 6.0,
        "Araneta Center-Cubao" to "GMA Kamuning"          to 6.0,
        "Araneta Center-Cubao" to "Santolan-Annapolis"    to 6.0,
        "Araneta Center-Cubao" to "Ortigas"               to 6.0,
        "Araneta Center-Cubao" to "Shaw Boulevard"        to 8.0,
        "Araneta Center-Cubao" to "Boni"                  to 8.0,
        "Araneta Center-Cubao" to "Guadalupe"             to 10.0,
        "Araneta Center-Cubao" to "Buendia"               to 10.0,
        "Araneta Center-Cubao" to "Ayala"                 to 10.0,
        "Araneta Center-Cubao" to "Magallanes"            to 12.0,
        "Araneta Center-Cubao" to "Taft Avenue"           to 12.0,
        // Santolan-Annapolis
        "Santolan-Annapolis" to "North Avenue"          to 8.0,
        "Santolan-Annapolis" to "Quezon Avenue"         to 8.0,
        "Santolan-Annapolis" to "GMA Kamuning"          to 6.0,
        "Santolan-Annapolis" to "Araneta Center-Cubao"  to 6.0,
        "Santolan-Annapolis" to "Ortigas"               to 6.0,
        "Santolan-Annapolis" to "Shaw Boulevard"        to 6.0,
        "Santolan-Annapolis" to "Boni"                  to 8.0,
        "Santolan-Annapolis" to "Guadalupe"             to 8.0,
        "Santolan-Annapolis" to "Buendia"               to 10.0,
        "Santolan-Annapolis" to "Ayala"                 to 10.0,
        "Santolan-Annapolis" to "Magallanes"            to 10.0,
        "Santolan-Annapolis" to "Taft Avenue"           to 12.0,
        // Ortigas
        "Ortigas" to "North Avenue"          to 10.0,
        "Ortigas" to "Quezon Avenue"         to 8.0,
        "Ortigas" to "GMA Kamuning"          to 8.0,
        "Ortigas" to "Araneta Center-Cubao"  to 6.0,
        "Ortigas" to "Santolan-Annapolis"    to 6.0,
        "Ortigas" to "Shaw Boulevard"        to 6.0,
        "Ortigas" to "Boni"                  to 6.0,
        "Ortigas" to "Guadalupe"             to 8.0,
        "Ortigas" to "Buendia"               to 8.0,
        "Ortigas" to "Ayala"                 to 10.0,
        "Ortigas" to "Magallanes"            to 10.0,
        "Ortigas" to "Taft Avenue"           to 10.0,
        // Shaw Boulevard
        "Shaw Boulevard" to "North Avenue"          to 10.0,
        "Shaw Boulevard" to "Quezon Avenue"         to 10.0,
        "Shaw Boulevard" to "GMA Kamuning"          to 8.0,
        "Shaw Boulevard" to "Araneta Center-Cubao"  to 8.0,
        "Shaw Boulevard" to "Santolan-Annapolis"    to 6.0,
        "Shaw Boulevard" to "Ortigas"               to 6.0,
        "Shaw Boulevard" to "Boni"                  to 6.0,
        "Shaw Boulevard" to "Guadalupe"             to 6.0,
        "Shaw Boulevard" to "Buendia"               to 8.0,
        "Shaw Boulevard" to "Ayala"                 to 8.0,
        "Shaw Boulevard" to "Magallanes"            to 10.0,
        "Shaw Boulevard" to "Taft Avenue"           to 10.0,
        // Boni
        "Boni" to "North Avenue"          to 10.0,
        "Boni" to "Quezon Avenue"         to 10.0,
        "Boni" to "GMA Kamuning"          to 10.0,
        "Boni" to "Araneta Center-Cubao"  to 8.0,
        "Boni" to "Santolan-Annapolis"    to 8.0,
        "Boni" to "Ortigas"               to 6.0,
        "Boni" to "Shaw Boulevard"        to 6.0,
        "Boni" to "Guadalupe"             to 6.0,
        "Boni" to "Buendia"               to 6.0,
        "Boni" to "Ayala"                 to 8.0,
        "Boni" to "Magallanes"            to 8.0,
        "Boni" to "Taft Avenue"           to 10.0,
        // Guadalupe
        "Guadalupe" to "North Avenue"          to 12.0,
        "Guadalupe" to "Quezon Avenue"         to 10.0,
        "Guadalupe" to "GMA Kamuning"          to 10.0,
        "Guadalupe" to "Araneta Center-Cubao"  to 10.0,
        "Guadalupe" to "Santolan-Annapolis"    to 8.0,
        "Guadalupe" to "Ortigas"               to 8.0,
        "Guadalupe" to "Shaw Boulevard"        to 6.0,
        "Guadalupe" to "Boni"                  to 6.0,
        "Guadalupe" to "Buendia"               to 6.0,
        "Guadalupe" to "Ayala"                 to 6.0,
        "Guadalupe" to "Magallanes"            to 8.0,
        "Guadalupe" to "Taft Avenue"           to 8.0,
        // Buendia
        "Buendia" to "North Avenue"          to 12.0,
        "Buendia" to "Quezon Avenue"         to 12.0,
        "Buendia" to "GMA Kamuning"          to 10.0,
        "Buendia" to "Araneta Center-Cubao"  to 10.0,
        "Buendia" to "Santolan-Annapolis"    to 10.0,
        "Buendia" to "Ortigas"               to 8.0,
        "Buendia" to "Shaw Boulevard"        to 8.0,
        "Buendia" to "Boni"                  to 6.0,
        "Buendia" to "Guadalupe"             to 6.0,
        "Buendia" to "Ayala"                 to 6.0,
        "Buendia" to "Magallanes"            to 6.0,
        "Buendia" to "Taft Avenue"           to 8.0,
        // Ayala
        "Ayala" to "North Avenue"          to 12.0,
        "Ayala" to "Quezon Avenue"         to 12.0,
        "Ayala" to "GMA Kamuning"          to 12.0,
        "Ayala" to "Araneta Center-Cubao"  to 10.0,
        "Ayala" to "Santolan-Annapolis"    to 10.0,
        "Ayala" to "Ortigas"               to 10.0,
        "Ayala" to "Shaw Boulevard"        to 8.0,
        "Ayala" to "Boni"                  to 8.0,
        "Ayala" to "Guadalupe"             to 6.0,
        "Ayala" to "Buendia"               to 6.0,
        "Ayala" to "Magallanes"            to 6.0,
        "Ayala" to "Taft Avenue"           to 6.0,
        // Magallanes
        "Magallanes" to "North Avenue"          to 14.0,
        "Magallanes" to "Quezon Avenue"         to 12.0,
        "Magallanes" to "GMA Kamuning"          to 12.0,
        "Magallanes" to "Araneta Center-Cubao"  to 12.0,
        "Magallanes" to "Santolan-Annapolis"    to 10.0,
        "Magallanes" to "Ortigas"               to 10.0,
        "Magallanes" to "Shaw Boulevard"        to 10.0,
        "Magallanes" to "Boni"                  to 8.0,
        "Magallanes" to "Guadalupe"             to 8.0,
        "Magallanes" to "Buendia"               to 6.0,
        "Magallanes" to "Ayala"                 to 6.0,
        "Magallanes" to "Taft Avenue"           to 6.0,
        // Taft Avenue
        "Taft Avenue" to "North Avenue"          to 14.0,
        "Taft Avenue" to "Quezon Avenue"         to 14.0,
        "Taft Avenue" to "GMA Kamuning"          to 12.0,
        "Taft Avenue" to "Araneta Center-Cubao"  to 12.0,
        "Taft Avenue" to "Santolan-Annapolis"    to 12.0,
        "Taft Avenue" to "Ortigas"               to 10.0,
        "Taft Avenue" to "Shaw Boulevard"        to 10.0,
        "Taft Avenue" to "Boni"                  to 10.0,
        "Taft Avenue" to "Guadalupe"             to 8.0,
        "Taft Avenue" to "Buendia"               to 8.0,
        "Taft Avenue" to "Ayala"                 to 6.0,
        "Taft Avenue" to "Magallanes"            to 6.0,
    )

    fun dialEmergencyContact(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun getFareOffline(origin: String, destination: String): Double =
        FARE_MATRIX[origin to destination] ?: 0.0

    fun getEstimatedTravelTimeMinutes(origin: String, destination: String): Int {
        val fromIdx = STATION_NAMES.indexOf(origin)
        val toIdx   = STATION_NAMES.indexOf(destination)
        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return 0
        val range = if (fromIdx < toIdx) fromIdx until toIdx else toIdx until fromIdx
        val totalSeconds = range.sumOf { i ->
            MRT3_STATIONS[i].travelToNext + MRT3_STATIONS[i].baseDwell
        }
        return totalSeconds / 60
    }
}
