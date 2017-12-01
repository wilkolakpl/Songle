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
import android.os.Vibrator
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
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private lateinit var mLastLocation : Location
    private var receiver = GeofencingReceiver()
    private val dbCollectedWordsHandler = MyCollectedWordsDBHandler(this)
    private val dbPlacemarkHandler = MyPlacemarkDBHandler(this)
    private val dbSongHandler = MySongDBHandler(this)
    private val mapMarkersWithGeofences = hashMapOf<String, Pair<Marker, Geofence>>()

    private val TAG = "MapsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)
        point.imageAlpha = 0

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
        mGoogleApiClient.connect()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_maps, menu)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mGoogleApiClient.isConnected){
            mGoogleApiClient.disconnect()
        }
        unregisterReceiver(receiver)
    }

    override fun onResume(){
        super.onResume()
        if (getCurrentSong() != 0){
            score.text = score()
        }
    }

    override fun onStart() {
        super.onStart()
        if (getCurrentSong() != 0){
            score.text = score()
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

    val geofenceRecalculationDilution = 10
    var currentGeofencePoll = 0
    override fun onLocationChanged(current : Location?) {
        if (current == null){
            Log.i(TAG, "[onLocationChanged] Location unknown")
        } else {
            if (mapMarkersWithGeofences.size == 0) {
                Log.i(TAG, "no geofences to add")
            } else {
                // update geofences once every 'geofenceRecalculationDilution' location updates
                if (currentGeofencePoll % geofenceRecalculationDilution == 0){
                    updateGeofenceMonitoring(current.getLatitude(), current.getLongitude())
                }
                currentGeofencePoll = (currentGeofencePoll + 1) % geofenceRecalculationDilution
            }
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
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(edin))

        try {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.clear()
            try {
                val mapMarkerOptions = hashMapOf<String, MarkerOptions>()
                dbPlacemarkHandler.populateHashMap(mapMarkerOptions)
                //@todo This is horribly slow, kick to an asynch task ///// also terrible bug when no location, just crashes
                for (key in mapMarkerOptions.keys){
                    val marker = mMap.addMarker(mapMarkerOptions[key])
                    val geofence = Geofence.Builder()
                            .setRequestId(key)
                            .setCircularRegion(
                                    marker.position.latitude,
                                    marker.position.longitude,
                                    40f
                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setNotificationResponsiveness(0)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build()
                    mapMarkersWithGeofences[key] = Pair(marker, geofence)
                }
            } finally {
                //@todo
            }
        } catch (se : SecurityException) {
            Log.e(TAG, "Security exception thrown [onMapReady]")
        }
    }

    fun getCurrentSong(): Int{
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        return sharedPref.getInt("currentSong", 0)
    }


    fun updateGeofenceMonitoring(lat: Double, long: Double){
        try{
            val geofences = mutableListOf<Geofence>()
            val mrkrsAndGeofences = mutableListOf<Pair<Marker, Geofence>>()

            for (key in mapMarkersWithGeofences.keys){
                mrkrsAndGeofences.add(mapMarkersWithGeofences[key]!!)
            }

            if (mrkrsAndGeofences.size > 50){
                mrkrsAndGeofences.sortBy { haversine(lat, long, it.first.position.latitude, it.first.position.longitude) }
                for (x in (0..50)){
                    geofences.add(mrkrsAndGeofences[x].second)
                }
            } else {
                for (mrkrAndGeofence in mrkrsAndGeofences){
                    geofences.add(mrkrAndGeofence.second)
                }
            }

            if (!mGoogleApiClient.isConnected)  {
                Log.e(TAG, "api client not connected")
            } else {

                val geofenceRequest = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofences(geofences).build()

                val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
                val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

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

            if (mapMarkersWithGeofences[nameOfPlacemark] != null){
                mapMarkersWithGeofences[nameOfPlacemark]!!.first.remove()
                mapMarkersWithGeofences.remove(nameOfPlacemark)
                dbPlacemarkHandler.delete(nameOfPlacemark)
                dbCollectedWordsHandler.add(CollectedWord(nameOfPlacemark))
                collectedWord()
            }
        }
    }

    fun collectedWord(){
        point.imageAlpha = 255
        val task = Alpha0()
        val timer = Timer()
        timer.schedule(task, 500.toLong())
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(500)

        score.text = score()
    }

    private inner class Alpha0: TimerTask(){
        override fun run(){
            runOnUiThread{
                point.imageAlpha = 0
            }
        }
    }

    fun score() : String {
        val noOfWords = dbSongHandler.getProp(getCurrentSong(), "noOfWords").toDouble()
        val collectedWords = dbCollectedWordsHandler.howMany()
        val score = 3*(10*Math.log10((collectedWords/noOfWords)+0.1)+10)
        return "%.2f".format(score)
    }
}
