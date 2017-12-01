package com.example.wilko.songle

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.ImageButton
import com.google.android.youtube.player.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.FileInputStream
import java.lang.Math.pow
import java.lang.ref.WeakReference
import java.util.*
import android.graphics.PorterDuff
import android.os.Vibrator
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg


class MainActivity : AppCompatActivity(), YouTubePlayer.OnInitializedListener {

    private val receiver = NetworkReceiver()
    private val dbSongHandler = MySongDBHandler(this)
    private val dbPlacemarkHandler = MyPlacemarkDBHandler(this)
    private val dbCollectedWordsHandler = MyCollectedWordsDBHandler(this)
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
            if (getIntInfo("newGame") == 1){
                startNewGame()
            } else if(getIntInfo("cached") == 5){
                val intent = Intent(this, MapsActivity::class.java)
                startActivityForResult(intent, 1)
            } else {
                mainTextLog.text = getString(R.string.downloading_interrupted)
            }
        }

        checkProgressButton.setOnClickListener {
            if (getIntInfo("newGame") == 1){
                startNewGame()
            } else if (getIntInfo("cached") == 5){
                val intent = Intent(this, CheckProgressActivity::class.java)
                startActivityForResult(intent, 1)
            } else {
                mainTextLog.text = getString(R.string.downloading_interrupted)
            }
        }
        continueTxt()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        //@todo dismiss all dem alertDialogs and fm.beginTransaction().remove(mDataFragment).commit()
    }

    override fun onPause() {
        super.onPause()
        //dismiss views and fm.beginTransaction().remove(mDataFragment).commit()
    }

    fun continueTxt() {
        if (getIntInfo("cached") == 5 && getIntInfo("newGame") == 0){
            score.text = score()
            mainTextLog.text = getString(R.string.continue_playing)
        } else {
            mainTextLog.text = getString(R.string.start_new_game)
        }
    }

    fun score() : String {
        val noOfWords = dbSongHandler.getProp(getIntInfo("currentSong"), "noOfWords").toDouble()
        val collectedWords = dbCollectedWordsHandler.howMany()
        val score = 3*(10*Math.log10((collectedWords/noOfWords)+0.1)+10)
        return "%.2f".format(score)
    }

    fun score(won: Boolean) : String {
        val noOfWords = dbSongHandler.getProp(getIntInfo("currentSong"), "noOfWords").toDouble()
        val collectedWords = dbCollectedWordsHandler.howMany()
        var score = 3*(10*Math.log10((collectedWords/noOfWords)+0.1)+10)
        if (won){ // award bonus points, in accordance to the difficulty level/selected map
            when (getIntInfo("mapNo")){
                1 -> score += 3*(-10*Math.log10((collectedWords/noOfWords)+1)+36-Math.pow((((collectedWords/noOfWords)-12.5)/20),2.0))
                2 -> score += 3*(-10*Math.log10((collectedWords/noOfWords)+1)+34-Math.pow((((collectedWords/noOfWords)-25)/20),2.0))
                3 -> score += 3*(-10*Math.log10((collectedWords/noOfWords)+1)+32-Math.pow((((collectedWords/noOfWords)-37.5)/20),2.0))
                4 -> score += 3*(-10*Math.log10((collectedWords/noOfWords)+1)+30-Math.pow((((collectedWords/noOfWords)-50)/20),2.0))
                5 -> score += 3*(-10*Math.log10((collectedWords/noOfWords)+1)+28-Math.pow((((collectedWords/noOfWords)-50)/20),2.0))
            }
        }
        return "%.2f".format(score)
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

    // variable representing which pose the hand is currently in
    private var state = 0
    fun handRoll(won: Boolean){
        mapButton.isEnabled = false
        checkProgressButton.isEnabled = false

        val x : Int
        if (won) {
            x = 2
        } else {
            x = 1
        }
        state = x-1

        for (it in 1..10){
            val task = FlagChange()
            val timer = Timer()
            timer.schedule(task, ((pow((it-11).toDouble(), 2.toDouble()))*40).toLong())
        }

        val task = FinalResult(won)
        val timer = Timer()
        timer.schedule(task, 4000)
    }

    private inner class FlagChange: TimerTask(){
        override fun run(){
            state = (state + 1) % 3
            myRenderer.changeStateFlag(state)
        }
    }

    private inner class FinalResult(val won : Boolean): TimerTask(){
        override fun run(){
            runOnUiThread{
                mainTextLog.text = titleStr
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(500)
                if (won){
                    playVideo()
                    score.text = score(won)
                }
                mapButton.isEnabled = true
                checkProgressButton.isEnabled = true
            }
        }
    }

    private fun playVideo(){
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        val videoView = layoutInflater.inflate(R.layout.video, null)
        val videoFragment = supportFragmentManager.findFragmentById(R.id.videoView) as YouTubePlayerSupportFragment
        videoFragment.initialize(getString(R.string.google_maps_key), this)
        mBuilder.setView(videoView)
        val videoPopup = mBuilder.create()
        videoPopup.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        videoPopup.window.attributes.gravity = Gravity.BOTTOM
        videoPopup.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        videoPopup.setOnCancelListener{
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(videoFragment)
            fragmentTransaction.commit()
        }
        videoPopup.show()
    }

    override fun onInitializationSuccess(p0: YouTubePlayer.Provider?, player: YouTubePlayer?, wasRestored: Boolean) {
        if (!wasRestored) {
            player!!.loadVideo(dbSongHandler.getProp(getIntInfo("currentSong"), "link"))
        }
    }

    override fun onInitializationFailure(p0: YouTubePlayer.Provider?, p1: YouTubeInitializationResult?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){
            // update the displayed score when returning from Map or Progress
            score.text = score()
            if(resultCode == Activity.RESULT_OK && data != null){
                val songNo = data.getIntExtra("songNo", 0)
                val didUpgrade = data.getBooleanExtra("upgrade", false)
                val didReset = data.getBooleanExtra("reset", false)
                if (songNo != 0) {
                    mainTextLog.text = getString(R.string.how_did_you_do)
                    if (songNo == getIntInfo("currentSong")){
                        titleStr = getString(R.string.correct_guess)
                        handRoll(true)
                    } else {
                        titleStr = getString(R.string.incorrect_guess)
                        handRoll(false)
                    }
                    saveIntInfo("newGame", 1)
                } else if (didUpgrade) {
                    mainTextLog.text = getString(R.string.map_upgrade)
                    showDifficultyDialog()
                } else if (didReset) {
                    startNewGame()
                }
            } else {
                mainTextLog.text = getString(R.string.continue_playing)
            }
        }
    }

    fun startNewGame() {
        saveIntInfo("newGame", 1)
        mainTextLog.text = getString(R.string.downloading_xml)
        DownloadXmlTaskSong(NetworkReceiver(), WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
    }

    private inner class NetworkReceiver : BroadcastReceiver(), AsyncCompleteListener<Pair<DownloadType, List<Song>>> {
        override fun onReceive(context: Context, intent: Intent) {

            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connMgr.activeNetworkInfo
            if (networkInfo != null) {
                if (getIntInfo("cached") != 5){
                    startNewGame()
                }
            } else {
                if (getIntInfo("cached") != 5){
                    mainTextLog.text = getString(R.string.no_internet)
                }
            }
        }

        override fun asyncComplete(result: Pair<DownloadType, List<Song>>?){
            // stage the downloads, keeping flags to indicate levels of completion
            if (result != null){
                if (result.first == DownloadType.SONGS) {
                    mainTextLog.text = getString(R.string.downloading_pngs)
                    dbSongHandler.deleteAll()
                    dbSongHandler.addAll(result.second)
                    DownloadPinPngs(this, WeakReference<Context>(applicationContext)).execute("http://maps.google.com/mapfiles/kml/paddle/")
                    saveIntInfo("cached", 1)
                } else if (result.first == DownloadType.NO_NEW_SONGS && getIntInfo("cached") == 5) {
                    mainTextLog.text = getString(R.string.downloading_unnecessary)
                    chooseSong()
                } else if (result.first == DownloadType.IMG && getIntInfo("cached") == 1) {
                    mainTextLog.text = getString(R.string.downloading_kmls)
                    DownloadKmlTaskLayers(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    saveIntInfo("cached", 2)
                } else if (result.first == DownloadType.KLMS && getIntInfo("cached") == 2) {
                    mainTextLog.text = getString(R.string.downloading_lyrics)
                    DownloadLyrics(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    saveIntInfo("cached", 3)
                } else if (result.first == DownloadType.LYRIC && getIntInfo("cached") == 3) {
                    mainTextLog.text = getString(R.string.downloading_complete)
                    chooseSong()
                    saveIntInfo("cached", 4)
                } else {
                    // edge case: when the download has been interrupted - the song list timestamp
                    // has been preserved, but later staging has failed - just restart the download
                    mainTextLog.text = getString(R.string.downloading_xml)
                    saveStrInfo("timestamp", "reset")
                    DownloadXmlTaskSong(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                }
            } else {
                if (getIntInfo("cached") == 5){
                    mainTextLog.text = getString(R.string.no_internet_but_available)
                    chooseSong()
                } else {
                    mainTextLog.text = getString(R.string.no_internet)
                    saveIntInfo("cached", 0)
                }
            }
        }
    }

    fun chooseSong() {
        // setting 0 for mapNo, indicating that it has not been assigned yet
        saveIntInfo("mapNo", 0)

        // choosing a song at random
        val num = dbSongHandler.howMany()
        var random = (1..(num+1)).random()
        while (getIntInfo("currentSong") == random) {
            // ensuring that it is not the previously guessed song
            random = (1..(num+1)).random()
        }
        saveIntInfo("currentSong", random)

        // resetting the collected words database
        dbCollectedWordsHandler.deleteAll()

        mainTextLog.text = getIntInfo("currentSong").toString()
        showDifficultyDialog()
    }

    fun showDifficultyDialog(){
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        val setDifficultyView = layoutInflater.inflate(R.layout.difficulity, null)
        val diff5 = setDifficultyView.findViewById<ImageButton>(R.id.diff5Button)
        val diff4 = setDifficultyView.findViewById<ImageButton>(R.id.diff4Button)
        val diff3 = setDifficultyView.findViewById<ImageButton>(R.id.diff3Button)
        val diff2 = setDifficultyView.findViewById<ImageButton>(R.id.diff2Button)
        val diff1 = setDifficultyView.findViewById<ImageButton>(R.id.diff1Button)
        mBuilder.setView(setDifficultyView)
        val alert = mBuilder.create()
        alert.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val currentMapNo = getIntInfo("mapNo")
        if (currentMapNo == 0){
            alert.setCancelable(false)
            alert.setCanceledOnTouchOutside(false)
        }
        if (currentMapNo < 5){
            setActiveButton(alert, diff5, 5)
        } else {
            grayOutButton(diff5, R.drawable.ic_diff5)
        }
        if (currentMapNo < 4){
            setActiveButton(alert, diff4, 4)
        } else {
            grayOutButton(diff4, R.drawable.ic_diff4)
        }
        if (currentMapNo < 3){
            setActiveButton(alert, diff3, 3)
        } else {
            grayOutButton(diff3, R.drawable.ic_diff3)
        }
        if (currentMapNo < 2){
            setActiveButton(alert, diff2, 2)
        } else {
            grayOutButton(diff2, R.drawable.ic_diff2)
        }
        if (currentMapNo < 1){
            setActiveButton(alert, diff1, 1)
        } else {
            grayOutButton(diff1, R.drawable.ic_diff1)
        }
        alert.show()

        score.text = score()
    }

    fun grayOutButton(ib : ImageButton, resource : Int){
        val res = baseContext.resources.getDrawable(resource).mutate()
        res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        ib.setImageDrawable(res)
        ib.isEnabled = false
    }

    fun setActiveButton(ad: AlertDialog, ib: ImageButton, mapNo: Int){
        ib.setOnClickListener {
            loadKml(mapNo)
            ad.dismiss()
        }
    }

    fun loadKml(mapNo : Int){
        async(UI){
            dbPlacemarkHandler.deleteAll()
            val kmlFile = FileInputStream(filesDir.toString() + "/map" + mapNo + "song" + getIntInfo("currentSong") + "cacheKml.kml")
            val kmlParser = KmlParser()
            val placemarks = bg{kmlParser.parse(kmlFile, applicationContext)}
            dbPlacemarkHandler.addAll(placemarks.await())
            // the flagging is delayed until the background thread's callback
            if (placemarks.await() != null) {
                saveIntInfo("mapNo", mapNo)
                saveIntInfo("cached", 5)
                saveIntInfo("newGame", 0)
            }
        }
    }

    fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start
}
