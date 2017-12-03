package com.example.wilko.songle

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var mLastLocation : Location? = null
    private val dbCollectedWordsHandler = DBCollectedWords(this)
    private val dbPlacemarkHandler = DBPlacemarks(this)
    private val dbSongHandler = DBSongs(this)
    private val mapMarkers = hashMapOf<String, Marker>()
    private var vibration : Boolean = true

    private val TAG = "MapsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)
        point.imageAlpha = 0

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
    }

    override fun onResume(){
        super.onResume()
        if (getCurrentSong() != 0){
            score.text = score()
        }
        val sharedPref = baseContext.defaultSharedPreferences
        vibration = sharedPref.getBoolean("vibration", true)
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

    // when a user grants location permissions, the function calls requiring them get repeated
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onConnected(null)
                    onMapReady(mMap)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private val recalculationDilution = 5
    private val closeNo = 50
    private var currentPoll = 0
    private var mapMarkersTop = mutableListOf<Pair<String, Marker>>()
    override fun onLocationChanged(current : Location?) {
        // to avoid having to compare the distance to every marker on each call, this method creates
        // a list of the closest markers (sparsely updated), and checks only the distances within it
        if (current == null){
            Log.i(TAG, "[onLocationChanged] Location unknown")
        } else {

            val currentLocation = Location("currentLocation")
            currentLocation.latitude = current.latitude
            currentLocation.longitude = current.longitude

            // ensuring that there exist markers to collect
            if (mapMarkers.size != 0) {

                // update the 'closeNo' closest points once every 'recalculationDilution' location updates
                if (currentPoll % recalculationDilution == 0){

                    // sort the map markers by distance to the current position
                    mapMarkersTop = mapMarkers.toList().sortedWith(
                            compareBy{
                                val markerLocation = Location("markerLocation")
                                markerLocation.latitude = it.second.position.latitude
                                markerLocation.longitude = it.second.position.longitude

                                currentLocation.distanceTo(markerLocation)}
                    ).toMutableList()

                    // take only the top 'closeNo' markers for tracking - this avoids the necessity of
                    // calculating the distance between each marker every frame
                    mapMarkersTop = mapMarkersTop.subList(0, Math.min(mapMarkersTop.size, closeNo))
                }
                currentPoll = (currentPoll + 1) % recalculationDilution

                // iterate through the list of closest 'closeNo' of markers, checking if the
                // required distance for marker collection is satisfied
                val iter = mapMarkersTop.iterator()
                while (iter.hasNext()) {
                    val pair = iter.next()
                    val key = pair.first
                    val marker = pair.second

                    val markerLocation = Location("markerLocation")
                    markerLocation.latitude = marker.position.latitude
                    markerLocation.longitude = marker.position.longitude

                    if (currentLocation.distanceTo(markerLocation) < 25) {

                        // removing marker from google map
                        mapMarkers[key]!!.remove()

                        // removing marker from closest markers list
                        iter.remove()

                        // removing marker from hashmap
                        mapMarkers.remove(key)

                        // removing marker from SQL database
                        dbPlacemarkHandler.delete(key)

                        // adding word to collected words SQL database
                        dbCollectedWordsHandler.add(CollectedWord(key))

                        collectedWord()
                    }
                }
            }
        }
    }

    fun collectedWord(){
        point.imageAlpha = 255
        val task = Alpha0()
        val timer = Timer()
        timer.schedule(task, 500.toLong())

        if (vibration){
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(500)
        }

        val container = point.parent as ViewGroup
        val pointX = point.left + point.width/2
        val pointY = point.top + point.height/2
        CommonConfetti.explosion(container, pointX, pointY,
                intArrayOf(getColor(R.color.colorPrimary),
                           getColor(R.color.colorPrimaryDark),
                           getColor(R.color.colorAccent))).oneShot()

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
        val edin = LatLng(55.9445390, -3.1885250)
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(edin))

        try {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.clear()

            // adding all the markers to the map
            val mapMarkerOptions = hashMapOf<String, MarkerOptions>()
            dbPlacemarkHandler.populateHashMap(mapMarkerOptions)
            for (key in mapMarkerOptions.keys){
                val marker = mMap.addMarker(mapMarkerOptions[key])
                mapMarkers[key] = marker
            }
        } catch (se : SecurityException) {
            Log.e(TAG, "Security exception thrown [onMapReady]")
        }
    }

    fun getCurrentSong(): Int{
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        return sharedPref.getInt("currentSong", 0)
    }
}
