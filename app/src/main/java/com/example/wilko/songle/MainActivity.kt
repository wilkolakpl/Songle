package com.example.wilko.songle

import android.app.Activity
import android.content.*
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.lang.Math.pow
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity() {

    private val receiver = NetworkReceiver()
    private val dbHandler = MySongDBHandler(this)
    private var state = 0
    private lateinit var myRenderer : MyRenderer
    private lateinit var titleStr : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        myRenderer = MyRenderer(applicationContext)
        surfaceView.setRenderer(myRenderer)

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        this.registerReceiver(receiver, filter)

        mapButton.setOnClickListener {
            if (getInfo("cached") == 1){
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
            } else {
                textSongTitle.text = getString(R.string.downloading_interrupted)
            }
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()*/
        }

        changeSongButton.setOnClickListener{
            if (getInfo("cached") == 1){
                val intent = Intent(this, SongSelectionActivity::class.java)
                startActivityForResult(intent, 1)
            } else {
                textSongTitle.text = getString(R.string.downloading_interrupted)
            }
        }

        changeStateButton.setOnClickListener {
            if (getInfo("cached") == 1){
                DownloadXmlTaskSong(NetworkReceiver()).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                textSongTitle.text = getString(R.string.downloading_xml)
                saveInfo("cached", 0)
            } else {
                textSongTitle.text = getString(R.string.downloading_interrupted)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onStart(){
        super.onStart()

    }

    fun saveInfo(key: String, int: Int){
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(key, int)
        editor.apply()
    }

    fun getInfo(key: String): Int{
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        return sharedPref.getInt(key, 0)
    }

    //override fun onResume() {
    //    super.onResume()
    //}

    fun handRoll(x: Int){

        state = x-1
        for (it in 1..10){
            val task = FlagChange()
            val timer = Timer()
            timer.schedule(task, ((pow((it-11).toDouble(), 2.toDouble()))*40).toLong())

        }
        val task = TitleChange()
        val timer = Timer()
        timer.schedule(task, 4000)
    }

    private fun changeStateFlag(){
        state = (state + 1) % 3
        myRenderer.changeStateFlag(state)
    }

    private fun changeTitle(){
        textSongTitle.text = titleStr
    }

    private inner class FlagChange(): TimerTask(){
        override fun run(){
            changeStateFlag()
        }
    }

    private inner class TitleChange(): TimerTask(){
        override fun run(){
            runOnUiThread{
                changeTitle()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_settings -> true
            R.id.action_favourite -> {
                changeStateFlag()
                val mySnackbar = Snackbar.make(myCoordinatorLayout, "testing", Snackbar.LENGTH_SHORT)
                mySnackbar.setAction("DARE ME!", MyUndoListener())
                mySnackbar.show()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class MyUndoListener : View.OnClickListener{
        override fun onClick(v : View){
            changeStateFlag()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){
            if(resultCode == Activity.RESULT_OK && data != null){
                titleStr = data.getStringExtra("songTitle")
                handRoll(1)
            }
            else{
                textSongTitle.text = getString(R.string.select_song)
            }
        }
    }

    private inner class NetworkReceiver : BroadcastReceiver(), DownloadCompleteListener<Pair<DownloadType, List<Song>>> {
        override fun onReceive(context: Context, intent: Intent) {

            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connMgr.activeNetworkInfo
            if (networkInfo != null) {
                if (getInfo("cached") == 1){
                    textSongTitle.text = getString(R.string.downloading_unnecessary)
                } else {
                    DownloadXmlTaskSong(this).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                    textSongTitle.text = getString(R.string.downloading_xml)
                }
            } else {
                Toast.makeText(context, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        override fun downloadComplete(result: Pair<DownloadType, List<Song>>?){
            if (result != null){
                if (result.first == DownloadType.SONGS) {
                    dbHandler.deleteAll()
                    dbHandler.addAll(result.second)
                    DownloadPinPngs(this, WeakReference<Context>(applicationContext)).execute("http://maps.google.com/mapfiles/kml/paddle/")
                    textSongTitle.text = getString(R.string.downloading_pngs)
                } else if (result.first == DownloadType.IMG) {
                    DownloadKmlTaskLayers(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    textSongTitle.text = getString(R.string.downloading_kmls)
                } else if (result.first == DownloadType.KLMS) {
                    DownloadLyrics(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    textSongTitle.text = getString(R.string.downloading_lyrics)
                } else if (result.first == DownloadType.LYRIC) {
                    saveInfo("cached", 1)
                    textSongTitle.text = getString(R.string.downloading_complete)
                }
            }
        }
    }
}
