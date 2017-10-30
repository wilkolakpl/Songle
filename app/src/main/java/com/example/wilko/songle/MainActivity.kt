package com.example.wilko.songle

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.difficulity.*
import java.io.File
import java.io.FileInputStream
import java.lang.Math.pow
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity() {

    private val receiver = NetworkReceiver()
    private val dbSongHandler = MySongDBHandler(this)
    private val dbPlacemarkHandler = MyPlacemarkDBHandler(this)
    private var state = 0
    private var mapNo = -1
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
            if (getIntInfo("cached") == 1){
                val intent = Intent(this, MapsActivity::class.java)
                startActivityForResult(intent, 1)
            } else {
                textSongTitle.text = getString(R.string.downloading_interrupted)
            }
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()*/
        }

        checkProgressButton.setOnClickListener{
            if (getIntInfo("cached") == 1){
                val intent = Intent(this, CheckProgressActivity::class.java)
                startActivityForResult(intent, 1)
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

    fun saveIntInfo(key: String, int: Int){
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(key, int)
        editor.apply()
    }

    fun saveStrInfo(key: String, string: String){
        val sharedPref = getSharedPreferences("permStrs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(key, string)
        editor.apply()
    }

    fun getIntInfo(key: String): Int{
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
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
//            R.id.action_favourite -> {
//                changeStateFlag()
//                val mySnackbar = Snackbar.make(myCoordinatorLayout, "testing", Snackbar.LENGTH_SHORT)
//                mySnackbar.setAction("DARE ME!", MyUndoListener())
//                mySnackbar.show()
//                return true
//            }

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
                val songNo = data.getIntExtra("songNo", -1)
                val didReset = data.getBooleanExtra("reset", false)
                if (songNo != -1) {
                    textSongTitle.text = getString(R.string.how_did_you_do)
                    if (songNo == getIntInfo("currentSong")){
                        titleStr = getString(R.string.correct_guess)
                        handRoll(2)
                    } else {
                        titleStr = getString(R.string.incorrect_guess)
                        handRoll(1)
                    }
                } else if(didReset) {
                    DownloadXmlTaskSong(NetworkReceiver(), WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                    textSongTitle.text = getString(R.string.downloading_xml)
                    saveIntInfo("cached", 0)
                }
            }
            else{
                textSongTitle.text = getString(R.string.ready_to_guess_song)
            }
        }
    }

    private inner class NetworkReceiver : BroadcastReceiver(), DownloadCompleteListener<Pair<DownloadType, List<Song>>> {
        override fun onReceive(context: Context, intent: Intent) {

            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connMgr.activeNetworkInfo
            if (networkInfo != null) {
                if (getIntInfo("cached") == 1){
                    textSongTitle.text = getString(R.string.downloading_unnecessary)
                } else {
                    DownloadXmlTaskSong(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                    textSongTitle.text = getString(R.string.downloading_xml)
                }
            } else {
                Toast.makeText(context, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        override fun downloadComplete(result: Pair<DownloadType, List<Song>>?){
            if (result != null){
                if (result.first == DownloadType.SONGS) {
                    dbSongHandler.deleteAll()
                    dbSongHandler.addAll(result.second)
                    DownloadPinPngs(this, WeakReference<Context>(applicationContext)).execute("http://maps.google.com/mapfiles/kml/paddle/")
                    textSongTitle.text = getString(R.string.downloading_pngs)
                } else if (result.first == DownloadType.NO_NEW_SONGS) {
                    textSongTitle.text = getString(R.string.downloading_unnecessary)
                    chooseSong()
                    textSongTitle.text = getIntInfo("currentSong").toString()
                    showDiffDialog()
                } else if (result.first == DownloadType.IMG) {
                    DownloadKmlTaskLayers(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    textSongTitle.text = getString(R.string.downloading_kmls)
                } else if (result.first == DownloadType.KLMS) {
                    DownloadLyrics(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    textSongTitle.text = getString(R.string.downloading_lyrics)
                } else if (result.first == DownloadType.LYRIC) {
                    textSongTitle.text = getString(R.string.downloading_complete)
                    chooseSong()
                    textSongTitle.text = getIntInfo("currentSong").toString()
                    showDiffDialog()
                }
            }
        }
    }

    fun chooseSong() {
        val db = dbSongHandler.writableDatabase
        val num = db.compileStatement("SELECT Count(*) FROM songs")
                .simpleQueryForLong()
                .toInt()
        db.close()
        saveIntInfo("currentSong", (1..(num+1)).random())

        var lyric = dbSongHandler.getProp(getIntInfo("currentSong"), "lyric")
        lyric = lyric.replace(" ","\u2003")
        lyric = lyric.replace(Regex("[^\u2003\n]"), " ")
        saveStrInfo("lyric", lyric)

    }

    fun showDiffDialog(){
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        val mView = layoutInflater.inflate(R.layout.difficulity, null)
        val diff5 = mView.findViewById<ImageButton>(R.id.diff5Button)
        val diff4 = mView.findViewById<ImageButton>(R.id.diff4Button)
        val diff3 = mView.findViewById<ImageButton>(R.id.diff3Button)
        val diff2 = mView.findViewById<ImageButton>(R.id.diff2Button)
        val diff1 = mView.findViewById<ImageButton>(R.id.diff1Button)
        mBuilder.setView(mView)
        val alert = mBuilder.create()
        alert.setCancelable(false)
        alert.setCanceledOnTouchOutside(false)
        alert.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        diff5.setOnClickListener {
            mapNo = 5
            alert.dismiss()
            loadKlm()
        }
        diff4.setOnClickListener {
            mapNo = 4
            alert.dismiss()
            loadKlm()
        }
        diff3.setOnClickListener {
            mapNo = 3
            alert.dismiss()
            loadKlm()
        }
        diff2.setOnClickListener {
            mapNo = 2
            alert.dismiss()
            loadKlm()
        }
        diff1.setOnClickListener {
            mapNo = 1
            alert.dismiss()
            loadKlm()
        }
        alert.show()
    }


    fun loadKlm(){
        if (mapNo != -1){
            dbPlacemarkHandler.deleteAll()
            val kmlFile = FileInputStream(filesDir.toString() + "/map" + mapNo + "song" + getIntInfo("currentSong") + "cacheKml.kml")
            val kmlParser = KmlParser() //@todo kick to asynch
            val placemarks = kmlParser.parse(kmlFile)
            dbPlacemarkHandler.addAll(placemarks)
            saveIntInfo("cached", 1)
        }
    }

    fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start
}
