package com.example.wilko.songle

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.GoogleMap

/**
 * Created by wilko on 10/25/2017.
 */
class GeofenceTransitionsIntentService(): IntentService("GeofenceTransitionsIntentService") {
    var ACTION_RESP = "com.example.wilko.songle.GEOFENCE_PROCESSED"
    private val TAG = "GeofTranIntentService"
    override fun onHandleIntent(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "geofencingEvent hasError")
        } else {
            val transition = geofencingEvent.geofenceTransition
            val geofenceList = geofencingEvent.triggeringGeofences
            for (geofence in geofenceList){
                val requestId = geofence.requestId
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    val broadcastIntent = Intent()
                    broadcastIntent.action = ACTION_RESP
                    broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT)
                    broadcastIntent.putExtra("name", requestId)
                    sendBroadcast(broadcastIntent)
                }
            }
        }
    }
}