package com.example.wilko.songle

import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.kml.KmlLayer
import java.io.File
import java.io.FileInputStream
import java.net.MalformedURLException
import com.google.android.gms.maps.model.MapStyleOptions



class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private lateinit var mLastLocation : Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)
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

    override fun onDestroy() {
        super.onDestroy()

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
            println("IllegalStateException thrown [onConnected]")
        }

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)}
        else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onLocationChanged(current : Location?) {
        if (current == null){
            println("[onLocationChanged] Location unknown")
        } else {
            println("""[onLocationChanged] Lat/long now
            (${current.getLatitude()},
            ${current.getLongitude()})""")
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
                println("Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            println("Can't find style.")
        }

        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val edin = LatLng(55.9445390, -3.1885250)

        mMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(edin))

        try {
            mMap.isMyLocationEnabled = true
        } catch (se : SecurityException) {
            println("Security exception thrown [onMapReady]")
        }

        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.clear()
        try {
            val file = File(applicationContext.cacheDir, "map5song11cacheKml.kml")
            val input = FileInputStream(file)
            try {
                val layer = KmlLayer(mMap, input, applicationContext)
                layer.map = mMap
                for(placemark in layer.placemarks){
                    placemark.
                }
            } catch (e : MalformedURLException){
                println("////////////////////////////////////////////////////////////")
                //exitProcess(1)//@todo
            } finally {
                input.close()
            }
        } finally {
            //@todo
        }
    }
}
