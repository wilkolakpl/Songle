package com.example.wilko.songle

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.net.MalformedURLException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private lateinit var mLastLocation : Location
    private var receiver = GeofencingReceiver()
    private val dbPlacemarkHandler = MyPlacemarkDBHandler(this)
    private val mapMarkersWithGeofences = hashMapOf<String, Pair<Marker, Geofence>>()

    private val TAG = "MapsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        val filter = IntentFilter(GeofencingReceiver().ACTION_RESP)
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        registerReceiver(receiver, filter)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_maps, menu)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onStart(){
        super.onStart()
        mGoogleApiClient.connect()
    }

    override fun onStop(){
        super.onStop()
        if (mGoogleApiClient.isConnected){
            mGoogleApiClient.disconnect()
        }
        if (!mapMarkersWithGeofences.isEmpty()){
            dbPlacemarkHandler.deleteAll()
            val list = mutableListOf<Placemark>()
            for (marker in mapMarkersWithGeofences.values){
                val placemark = Placemark(marker.first.title, marker.first.snippet, "#" + marker.first.snippet, marker.first.position.latitude, marker.first.position.longitude)
                list.add(placemark)
            }
            dbPlacemarkHandler.addAll(list)
            //SaveRemainingPlacemarks(mapMarkersWithGeofences, WeakReference<Context>(applicationContext)).execute() @todo I must async this
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_check_progress -> {
                val intent = Intent(this, CheckProgressActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                startActivity(intent)
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun createLocationRequest(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 5000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
        }
    }

    override fun onConnected(connectionHint: Bundle?) {
        try {
            createLocationRequest()
        } catch (ise : IllegalStateException){
            Log.e(TAG, "IllegalStateException thrown [onConnected]")
        }

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onLocationChanged(current : Location?) {
        if (current == null){
            Log.i(TAG, "[onLocationChanged] Location unknown")
        } else {
            Log.i(TAG, """[onLocationChanged] Lat/long now
            (${current.getLatitude()},
            ${current.getLongitude()})""")
            /////////////////////////////////////////////////////

            updateGeofenceMonitoring(current.getLatitude(), current.getLongitude())

            ////////////////////////////////////////////////////
        }
    }

    override fun onConnectionFailed(result : ConnectionResult) {
        println(" >>>> onConnectionSuspended")
    }

    override fun onConnectionSuspended(flag: Int) {
        println(" >>>> onConnectionFailed")
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_theme_retro))
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style.")
        }

        mMap = googleMap
        // Add a marker and move the camera
        val edin = LatLng(55.9445390, -3.1885250)
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(edin))

        try {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.clear()
            try {
                val file = File(applicationContext.filesDir, "map5song" + getInfo("currentSong") + "cacheKml.kml")
                val input = FileInputStream(file)
                try {
                    //KmlLayer(mMap, input, applicationContext).addLayerToMap()

                    val mapMarkerOptions = hashMapOf<String, MarkerOptions>()
                    dbPlacemarkHandler.populateList(mapMarkerOptions)
                    //@todo This is horribly slow, kick to an asynch task
                    for (key in mapMarkerOptions.keys){
                        val marker = mMap.addMarker(mapMarkerOptions[key])
                        val geofence = Geofence.Builder()
                                .setRequestId(key)
                                .setCircularRegion(
                                        marker.position.latitude,
                                        marker.position.longitude,
                                        60f
                                )
                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                .setNotificationResponsiveness(1000)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                .build()
                        mapMarkersWithGeofences[key] = Pair(marker, geofence)
                    }

                    //for (x in (1..600)){
                        //mMap.addMarker(MarkerOptions().position(LatLng(55.9445390,-3.1885250))).setIcon(bs)
                    //}
                } finally {
                    input.close()
                }
            } finally {
                //@todo
            }
        } catch (se : SecurityException) {
            Log.e(TAG, "Security exception thrown [onMapReady]")
        }
    }

    fun getInfo(key: String): Int{
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        return sharedPref.getInt(key, 0)
    }

    fun updateGeofenceMonitoring(lat: Double, long: Double){
        try{
            val geofences = mutableListOf<Geofence>()
            val mrkrsAndGeofences = mutableListOf<Pair<Marker, Geofence>>()
            for (key in mapMarkersWithGeofences.keys){
                mrkrsAndGeofences.add(mapMarkersWithGeofences[key]!!)
            }
            mrkrsAndGeofences.sortBy { haversine(lat, long, it.first.position.latitude, it.first.position.longitude) }

            for (x in (0..50)){
                geofences.add(mrkrsAndGeofences[x].second)
            }

            val geofenceRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofences).build()

            val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
            val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            if (!mGoogleApiClient.isConnected)  {
                Log.e(TAG, "api client not connected")
            } else {
                LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, pendingIntent)
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofenceRequest, pendingIntent)
                        .setResultCallback({ status ->
                            if (status.isSuccess) {
                                Log.i(TAG, "Saving Geofence")

                            } else {
                                Log.e(TAG, "Registering geofence failed: " + status.statusMessage +
                                        " : " + status.statusCode)
                            }
                        })
            }
        }
        catch(e : SecurityException){
            Log.d(TAG, "Security Exception" + e.message)
        }
    }


    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6372.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1P = Math.toRadians(lat1)
        val lat2P = Math.toRadians(lat2)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1P) * Math.cos(lat2P)
        val c = 2 * Math.asin(Math.sqrt(a))
        return (R * c) * 1000
    }

    inner class GeofencingReceiver : BroadcastReceiver() {
        val ACTION_RESP = "com.example.wilko.songle.GEOFENCE_PROCESSED"
        override fun onReceive(context: Context, intent: Intent) {
            val nameOfPlacemark = intent.getStringExtra("name")
            Log.d(TAG, String.format("GEOFENCE_TRANSITION_ENTER on %s", nameOfPlacemark))

            val idx = nameOfPlacemark.split(":")
            //addWordToLyric(idx[0].toInt(), idx[1].toInt())
            LyricWordAdder(idx[0].toInt(), idx[1].toInt(), WeakReference<Context>(applicationContext)).execute()

            if (mapMarkersWithGeofences[nameOfPlacemark] != null){
                mapMarkersWithGeofences[nameOfPlacemark]!!.first.remove()
                mapMarkersWithGeofences.remove(nameOfPlacemark)
            }
        }

        fun addWordToLyric(row: Int, column: Int){
            val dbSongHandler = MySongDBHandler(applicationContext)
            val originalLyrics = dbSongHandler.getProp(getCurrSong(), "lyric")

            val newString = StringBuilder()
            val liriyc = getLyric()
            val lines = liriyc.split("\n")
            val origLines = originalLyrics.split("\n")
            for (lineNo in (0..lines.size-1)){
                val words = lines[lineNo].split("\t")
                val origWords = origLines[lineNo].split(" ")
                for (wordNo in (0..words.size-1)){
                    if ((lineNo + 1 == row) && (wordNo + 1 == column)){
                        if (wordNo == words.size -1){
                            newString.append(origWords[wordNo])
                        } else {
                            newString.append(origWords[wordNo]+"\t")
                        }
                    } else {
                        if (wordNo == words.size -1){
                            newString.append(words[wordNo])
                        } else {
                            newString.append(words[wordNo]+"\t")
                        }
                    }
                }
                if (lineNo != lines.size-1){
                    newString.append("\n")
                }
            }
            saveLyric(newString.toString())
        }

        fun saveLyric(string: String){
            val sharedPref = getSharedPreferences("permStrs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("lyric", string)
            editor.apply()
        }

        fun getLyric(): String{
            val sharedPref = getSharedPreferences("permStrs", Context.MODE_PRIVATE)
            return sharedPref.getString("lyric", "")
        }

        fun getCurrSong(): Int{
            val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
            return sharedPref.getInt("currentSong", 0)
        }
    }
}
