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
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg


class MainActivity : AppCompatActivity(), YouTubePlayer.OnInitializedListener {

    private val receiver = NetworkReceiver()
    private val dbSongHandler = MySongDBHandler(this)
    private val dbPlacemarkHandler = MyPlacemarkDBHandler(this)
    private val dbCollectedWordsHandler = MyCollectedWordsDBHandler(this)
    private var state = 0
    private var mapNo = 0
    private lateinit var myRenderer : MyRenderer
    private lateinit var titleStr : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mapNo = getIntInfo("mapNo")

        myRenderer = MyRenderer(applicationContext)
        surfaceView.setRenderer(myRenderer)

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        this.registerReceiver(receiver, filter)

        mapButton.setOnClickListener {
            if (getIntInfo("cached") == 5){
                val intent = Intent(this, MapsActivity::class.java)
                startActivityForResult(intent, 1)
            } else {
                textSongTitle.text = getString(R.string.downloading_interrupted)
            }
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()*/
        }

        checkProgressButton.setOnClickListener{
            if (getIntInfo("cached") == 5){
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
        //@todo dismiss all dem alertDialogs and fm.beginTransaction().remove(mDataFragment).commit()
    }

    override fun onPause() {
        super.onPause()
        //dismiss views and fm.beginTransaction().remove(mDataFragment).commit()
    }

    override fun onResume(){
        super.onResume()
        if (getIntInfo("currentSong") != 0){
            score.text = score()
        }
    }

    override fun onStart() {
        super.onStart()
        if (getIntInfo("currentSong") != 0){
            score.text = score()
        }
    }

    fun score() : String {
        val noOfWords = dbSongHandler.getProp(getIntInfo("currentSong"), "noOfWords").toDouble()
        val collectedWords = dbCollectedWordsHandler.howMany()
        val score = 3*(10*Math.log10((collectedWords/noOfWords)+0.1)+10)
        return "%.2f".format(score)
    }

    fun score(won : Boolean) : String {
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

    fun handRoll(x: Int){
        state = x-1
        for (it in 1..10){
            val task = FlagChange()
            val timer = Timer()
            timer.schedule(task, ((pow((it-11).toDouble(), 2.toDouble()))*40).toLong())

        }
        var won = false
        if (x == 2) {
            won = true
        }
        val task = TitleChange(won)
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

    private inner class TitleChange(val won : Boolean): TimerTask(){
        override fun run(){
            runOnUiThread{
                changeTitle()
                if (won){
                    playVideo()
                    score.text = score(won)
                }
            }
        }
    }

    private fun playVideo(){
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        val videoView = layoutInflater.inflate(R.layout.video, null)
        val videoFragment = supportFragmentManager.findFragmentById(R.id.videoView) as YouTubePlayerSupportFragment
        videoFragment.initialize(getString(R.string.api_key), this)
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
            if(resultCode == Activity.RESULT_OK && data != null){
                val songNo = data.getIntExtra("songNo", 0)
                val didUpgrade = data.getBooleanExtra("upgrade", false)
                val didReset = data.getBooleanExtra("reset", false)
                if (songNo != 0) {
                    textSongTitle.text = getString(R.string.how_did_you_do)
                    if (songNo == getIntInfo("currentSong")){
                        titleStr = getString(R.string.correct_guess)
                        handRoll(2)
                    } else {
                        titleStr = getString(R.string.incorrect_guess)
                        handRoll(1)
                    }
                } else if(didUpgrade) {
                    showDifficultyDialog()
                } else if(didReset) {
                    textSongTitle.text = getString(R.string.downloading_xml)
                    DownloadXmlTaskSong(NetworkReceiver(), WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
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
                if (getIntInfo("cached") == 5){
                    textSongTitle.text = getString(R.string.downloading_unnecessary)
                } else {
                    textSongTitle.text = getString(R.string.downloading_xml)
                    DownloadXmlTaskSong(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                }
            } else {
                if (getIntInfo("cached") == 5){
                    textSongTitle.text = getString(R.string.no_internet_but_available)
                } else {
                    textSongTitle.text = getString(R.string.no_internet)
                }
            }
        }

        override fun downloadComplete(result: Pair<DownloadType, List<Song>>?){
            // stage the downloads, keeping flags to indicate levels of completion
            if (result != null){
                if (result.first == DownloadType.SONGS) {
                    textSongTitle.text = getString(R.string.downloading_pngs)
                    dbSongHandler.deleteAll()
                    dbSongHandler.addAll(result.second)
                    DownloadPinPngs(this, WeakReference<Context>(applicationContext)).execute("http://maps.google.com/mapfiles/kml/paddle/")
                    saveIntInfo("cached", 1)
                } else if (result.first == DownloadType.NO_NEW_SONGS && getIntInfo("cached") == 5) {
                    textSongTitle.text = getString(R.string.downloading_unnecessary)
                    chooseSong()
                } else if (result.first == DownloadType.IMG && getIntInfo("cached") == 1) {
                    textSongTitle.text = getString(R.string.downloading_kmls)
                    DownloadKmlTaskLayers(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    saveIntInfo("cached", 2)
                } else if (result.first == DownloadType.KLMS && getIntInfo("cached") == 2) {
                    textSongTitle.text = getString(R.string.downloading_lyrics)
                    DownloadLyrics(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/")
                    saveIntInfo("cached", 3)
                } else if (result.first == DownloadType.LYRIC && getIntInfo("cached") == 3) {
                    textSongTitle.text = getString(R.string.downloading_complete)
                    chooseSong()
                    saveIntInfo("cached", 4)
                } else {
                    // edge case: when the download has been interrupted - the song list timestamp
                    // has been preserved, but later staging has failed - just restart the download
                    textSongTitle.text = getString(R.string.downloading_xml)
                    saveStrInfo("timestamp", "reset")
                    DownloadXmlTaskSong(this, WeakReference<Context>(applicationContext)).execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml")
                }
            } else {
                if (getIntInfo("cached") == 5){
                    textSongTitle.text = getString(R.string.no_internet_but_available)
                    chooseSong()
                } else {
                    textSongTitle.text = getString(R.string.no_internet)
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

        // resetting the collected words database, and preparing the to-be displayed lyric string
        dbCollectedWordsHandler.deleteAll()
        var lyric = dbSongHandler.getProp(getIntInfo("currentSong"), "lyric")
        lyric = lyric.replace(" ","\u2003")
        lyric = lyric.replace(Regex("[^\u2003\n]"), " ")
        saveStrInfo("lyric", lyric)

        textSongTitle.text = getIntInfo("currentSong").toString()
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
            diff5.setOnClickListener {
                mapNo = 5
                alert.dismiss()
                loadKml()
            }
        } else {
            val res = baseContext.resources.getDrawable(R.drawable.ic_diff5).mutate()
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
            diff5.setImageDrawable(res)
            diff5.isEnabled = false
        }
        if (currentMapNo < 4){
            diff4.setOnClickListener {
                mapNo = 4
                alert.dismiss()
                loadKml()
            }
        } else {
            val res = baseContext.resources.getDrawable(R.drawable.ic_diff4).mutate()
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
            diff4.setImageDrawable(res)
            diff4.isEnabled = false
        }
        if (currentMapNo < 3){
            diff3.setOnClickListener {
                mapNo = 3
                alert.dismiss()
                loadKml()
            }
        } else {
            val res = baseContext.resources.getDrawable(R.drawable.ic_diff3).mutate()
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
            diff3.setImageDrawable(res)
            diff3.isEnabled = false
        }
        if (currentMapNo < 2){
            diff2.setOnClickListener {
                mapNo = 2
                alert.dismiss()
                loadKml()
            }
        } else {
            val res = baseContext.resources.getDrawable(R.drawable.ic_diff2).mutate()
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
            diff2.setImageDrawable(res)
            diff2.isEnabled = false
        }
        if (currentMapNo < 1){
            diff1.setOnClickListener {
                mapNo = 1
                alert.dismiss()
                loadKml()
            }
        } else {
            val res = baseContext.resources.getDrawable(R.drawable.ic_diff1).mutate()
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
            diff1.setImageDrawable(res)
            diff1.isEnabled = false
        }
        alert.show()

        score.text = score()
    }


    fun loadKml(){
        if (mapNo != 0){
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
                }
            }
        }
    }

    fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start
}
