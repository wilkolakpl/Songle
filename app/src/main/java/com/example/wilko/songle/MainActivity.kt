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
import java.lang.Math.pow
import java.util.*
import android.graphics.PorterDuff
import android.os.Vibrator
import android.preference.PreferenceManager
import android.widget.ProgressBar
import com.github.jinatonic.confetti.CommonConfetti
import com.github.ybq.android.spinkit.style.*
import android.preference.PreferenceActivity
import android.util.Log
import org.jetbrains.anko.defaultSharedPreferences
import com.example.wilko.songle.databaseHelpers.DBCollectedWords
import com.example.wilko.songle.databaseHelpers.DBSongs
import com.example.wilko.songle.downloaders.*
import com.example.wilko.songle.openGL.MyRenderer
import com.example.wilko.songle.utils.AsyncCompleteListener


/**
 * Shared preferences flags to indicate whether the downloadable data has been cached,
 * or whether a new game should be started are used throughout this class.
 *
 * Cached has multiple states indicating download stages (as they have to be done in sequence).
 *  *disclaimer: even though it is named cached, the downloaded files are kept in persistent storage
 *
 * NewGame has 2 stages, indicating whether a new game should be started.
 *  *disclaimer: it is not used on the initial app launch, as seeing that the files are not cached
 *               is enough of a condition to start a new game
 */

class MainActivity : AppCompatActivity(), YouTubePlayer.OnInitializedListener {

    private val receiver = NetworkReceiver()
    private val dbSongHandler = DBSongs
    private val dbCollectedWordsHandler = DBCollectedWords
    private lateinit var myRenderer : MyRenderer
    private lateinit var titleStr : String
    private lateinit var progressBar : ProgressBar
    private lateinit var toolMenu : Menu
    private var vibration : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initial population of setting preferences on first launch
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)

        // setting the vibration preference
        val sharedPref = baseContext.defaultSharedPreferences
        vibration = sharedPref.getBoolean("vibration", true)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // setting up open GL renderer
        myRenderer = MyRenderer(applicationContext)
        surfaceView.setRenderer(myRenderer)

        // setting up an internet receiver
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(receiver, filter)

        mapButton.setOnClickListener {
            if (getNewGame()){
                startNewGame()
            } else if(getIntInfo("cached") == 4){
                val intent = Intent(this, MapsActivity::class.java)
                startActivityForResult(intent, 1)
            }
        }

        checkProgressButton.setOnClickListener {
            if (getNewGame()){
                startNewGame()
            } else if (getIntInfo("cached") == 4){
                val intent = Intent(this, CheckProgressActivity::class.java)
                startActivityForResult(intent, 1)
                // same request code as above, as user can check Progress switching from Map
            }
        }

        continueTxt()

        // setting up progressBar
        val rotatingPlane = WanderingCubes()
        progressBar = spin_kit
        progressBar.indeterminateDrawable = rotatingPlane
        progressBar.visibility = View.GONE // hiding it
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    fun continueTxt() {
        // displaying text according to context
        if (getIntInfo("cached") == 4 && !getNewGame()){
            score.text = score()
            mainTextLog.text = getString(R.string.continue_playing)
        } else {
            mainTextLog.text = getString(R.string.start_new_game)
        }
    }

    fun score(won: Boolean = false) : String {
        val noOfWords = dbSongHandler.getProp(getIntInfo("currentSong"), "noOfWords").toDouble()
        val collectedWords = dbCollectedWordsHandler.howMany()
        val proportionCollected = (collectedWords/noOfWords)*100

        var score = 3*(10*Math.log10(proportionCollected+0.1)+10)
        if (won){ // award bonus points, in accordance to the difficulty level/selected map
            when (getIntInfo("mapNo")){
                1 -> score += 3*(-10*Math.log10(proportionCollected+1)+36-Math.pow(((proportionCollected-12.5)/20),2.0))
                2 -> score += 3*(-10*Math.log10(proportionCollected+1)+34-Math.pow(((proportionCollected-25)/20),2.0))
                3 -> score += 3*(-10*Math.log10(proportionCollected+1)+32-Math.pow(((proportionCollected-37.5)/20),2.0))
                4 -> score += 3*(-10*Math.log10(proportionCollected+1)+30-Math.pow(((proportionCollected-50)/20),2.0))
                5 -> score += 3*(-10*Math.log10(proportionCollected+1)+28-Math.pow(((proportionCollected-50)/20),2.0))
            }
        }
        return "%.2f".format(score)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                // opening settings fragment directly, bypassing headers, as there is only one
                intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                        SettingsActivity.GeneralPreferenceFragment::class.java.name )
                intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true )
                startActivityForResult(intent, 2)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        toolMenu = menu // need reference to menu for disabling it during win/loose animation
        if (getIntInfo("cached") != 4) {
            // edge case, blocks input during initial download, on first launch
            toolMenu.getItem(0).isEnabled = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // METHODS THAT RELATE TO SHARED PREFERENCES FOLLOW:
    fun setIntInfo(key: String, int: Int){
        val sharedPref = getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(key, int)
        editor.apply()
    }

    fun getIntInfo(key: String): Int{
        val sharedPref = getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        return sharedPref.getInt(key, 0)
    }

    fun setTimestamp(string: String){
        val sharedPref = getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("timestamp", string)
        editor.apply()
    }

    fun setNewGame(boolean: Boolean){
        val sharedPref = getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("newGame", boolean)
        editor.apply()
    }

    fun getNewGame(): Boolean{
        val sharedPref = getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("newGame", true)
    }

    // variable representing which pose the hand is currently in
    private var state = 0
    fun handRoll(won: Boolean){
        // block input during animation
        blockInput(false) // false blocks the default loading spin

        // setting initial pose and changing it 10 times - final pose is deterministic
        val x : Int
        if (won) {
            x = 2
        } else {
            x = 1
        }
        state = x-1

        // scheduling all the pose changes
        for (it in 1..10){
            val task = FlagChange()
            val timer = Timer()
            // using a power function to model the distribution so that the switching
            // is rapid at first and then slows down
            timer.schedule(task, ((pow((it-11).toDouble(), 2.toDouble()))*40).toLong())
        }

        // also scheduling the final screen
        val task = FinalResult(won)
        val timer = Timer()
        timer.schedule(task, 4000)
    }

    private inner class FlagChange: TimerTask(){
        override fun run(){
            state = (state + 1) % 3
            myRenderer.changeStateFlag(state)
            if (vibration){
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(50)
            }
        }
    }

    private inner class FinalResult(val won : Boolean): TimerTask(){
        override fun run(){
            runOnUiThread{
                mainTextLog.text = titleStr // displaying the titleStr set in onActivityResult
                if (vibration){
                    (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(500)
                }
                if (won){
                    playVideo()
                    score.text = score(won)
                    CommonConfetti.rainingConfetti(window.decorView.rootView as ViewGroup,
                            intArrayOf(Color.YELLOW)).oneShot()
                }
                unblockInput()
            }
        }
    }

    private fun blockInput(spin : Boolean = true){
        if (spin) {
            // its not going to show the loading spin during guess animation
            progressBar.visibility = View.VISIBLE
        }
        grayOutButton(mapButton, R.drawable.ic_mapicon) // disabling and graying-out the buttons
        grayOutButton(checkProgressButton, R.drawable.ic_progressicon)
        try {
            toolMenu.getItem(0).isEnabled = false
        } catch (e : UninitializedPropertyAccessException) {
            Log.i(localClassName, "toolMenu uninitialized")
        }
    }

    private fun unblockInput(){
        progressBar.visibility = View.GONE
        mapButton.isEnabled = true
        mapButton.setImageDrawable(getDrawable(R.drawable.ic_mapicon)) // reversing the button grey-out
        checkProgressButton.isEnabled = true
        checkProgressButton.setImageDrawable(getDrawable(R.drawable.ic_progressicon))
        try {
            toolMenu.getItem(0).isEnabled = true
        } catch (e : UninitializedPropertyAccessException) {
            Log.i(localClassName, "toolMenu uninitialized")
        }
    }

    private fun playVideo(){
        // setting up a YouTubePlayerFragment in a Dialog
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        val videoView = layoutInflater.inflate(R.layout.video, null)

        // getting the fragment from the inflated view
        val videoPopUpFragment = supportFragmentManager.findFragmentById(R.id.videoView) as YouTubePlayerSupportFragment
        videoPopUpFragment.initialize(getString(R.string.google_maps_key), this)
        mBuilder.setView(videoView)
        val videoPopUp = mBuilder.create()

        // additional Dialog properties
        videoPopUp.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        videoPopUp.window.attributes.gravity = Gravity.BOTTOM
        videoPopUp.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        videoPopUp.setOnDismissListener{
            // removing fragment to avoid leaks
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(videoPopUpFragment)
            fragmentTransaction.commit()
        }
        videoPopUp.show()
    }

    override fun onInitializationSuccess(p0: YouTubePlayer.Provider?, player: YouTubePlayer?, wasRestored: Boolean) {
        if (!wasRestored) {
            player?.loadVideo(dbSongHandler.getProp(getIntInfo("currentSong"), "link"))
        }
    }

    override fun onInitializationFailure(p0: YouTubePlayer.Provider?, p1: YouTubeInitializationResult?) {
        Log.e(localClassName, "failed to initialize YouTube player fragment")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){ // returning from Map or Progress
            // update the displayed score
            score.text = score()
            if(resultCode == Activity.RESULT_OK && data != null){

                val songNo = data.getIntExtra("songNo", 0)
                val didUpgrade = data.getBooleanExtra("upgrade", false)
                val didReset = data.getBooleanExtra("reset", false)

                // since starting map or progress activities need the same requestCodes (as progress
                // may be opened from the map), the control flow is altered based on returned data:
                if (songNo != 0) {
                    mainTextLog.text = getString(R.string.how_did_you_do)
                    if (songNo == getIntInfo("currentSong")){
                        titleStr = getString(R.string.correct_guess) // storing this text in a var, to delay the displaying
                        handRoll(true) // trigger animation
                    } else {
                        titleStr = getString(R.string.incorrect_guess)
                        handRoll(false)
                    }
                    setNewGame(true) // flag
                } else if (didUpgrade) {
                    mainTextLog.text = getString(R.string.map_upgrade)
                    showDifficultyDialog()
                } else if (didReset) {
                    startNewGame()
                }
            } else {
                mainTextLog.text = getString(R.string.continue_playing)
            }
        } else if (requestCode == 2) {
            // returning from settings and updating the view to reflect preferences
            finish()
            startActivity(intent)
            // recreate() cannot be used here as it has a bug associated with it,
            // where the polish comma instead of a period wouldn't properly refresh in the score :P
        }
        // reset hand graphic to neutral pose when returning from an activity
        myRenderer.changeStateFlag(0)

    }

    fun startNewGame() {
        // UI appearance
        score.text =  getString(R.string.soon)
        mainTextLog.text = getString(R.string.downloading_xml)

        // UI behaviour
        setNewGame(true)
        blockInput()

        // initialize the required download on start of new game
        DownloadSongXml(NetworkReceiver()).execute()
    }

    private inner class NetworkReceiver : BroadcastReceiver(), AsyncCompleteListener<DownloadType> {
        override fun onReceive(context: Context, intent: Intent) {
            // triggered if the required data is not already cached - on the initial app launch
            if (getIntInfo("cached") != 4){

                val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connMgr.activeNetworkInfo

                if (networkInfo != null) {
                    startNewGame()
                } else {
                    mainTextLog.text = getString(R.string.no_internet)
                }
            }
        }

        override fun asyncComplete(result: DownloadType?){
            // stage the downloads, keeping flags to indicate levels of completion
            if (result != null){
                // the async results are enums indicating the staging
                if (result == DownloadType.SONGS) {
                    mainTextLog.text = getString(R.string.downloading_lyrics)
                    DownloadLyrics(this).execute() // chaining the asyncs
                    setIntInfo("cached", 1)
                } else if (result == DownloadType.NO_NEW_SONGS && getIntInfo("cached") == 4) {
                    chooseSong()
                } else if (result == DownloadType.LYRICS && getIntInfo("cached") == 1) {
                    mainTextLog.text = getString(R.string.downloading_kmls)
                    DownloadKmls(this).execute()
                    setIntInfo("cached", 2)
                } else if (result == DownloadType.KLMS && getIntInfo("cached") == 2) {
                    mainTextLog.text = getString(R.string.downloading_pngs)
                    DownloadPinPngs(this).execute()
                    setIntInfo("cached", 3)
                } else if (result == DownloadType.IMGS && getIntInfo("cached") == 3) {
                    chooseSong()
                    setIntInfo("cached", 4)
                } else {
                    // edge case: when the download has been interrupted and the song list timestamp
                    // has been preserved, but later staging has failed - just restart the download
                    // and reset the timestamp
                    mainTextLog.text = getString(R.string.downloading_xml)
                    setTimestamp("reset")
                    DownloadSongXml(this).execute()
                }
            } else {
                if (getIntInfo("cached") == 4){
                    chooseSong()
                } else {
                    mainTextLog.text = getString(R.string.no_internet_or_storage)

                    // reset flag
                    setIntInfo("cached", 0)

                    unblockInput()
                }
            }
        }
    }

    fun chooseSong() {
        mainTextLog.text = getString(R.string.new_game)

        // setting 0 for mapNo, indicating that it has not been assigned yet
        setIntInfo("mapNo", 0)

        // choosing a song at random
        val num = dbSongHandler.howMany()
        var random = (1..(num+1)).random()
        while (getIntInfo("currentSong") == random) {
            // ensuring that it is not the previously guessed song
            random = (1..(num+1)).random()
        }
        setIntInfo("currentSong", random)

        // resetting the collected words database
        dbCollectedWordsHandler.deleteAll()
        // showing the new score (of 0.00)
        score.text = score()

        // cheat, to display the number of the chosen song
        // mainTextLog.text = getIntInfo("currentSong").toString()

        showDifficultyDialog()
    }

    fun showDifficultyDialog(){
        // this is needed in case of map upgrade, the user needs to wait till kml is parsed
        blockInput()

        val mBuilder = AlertDialog.Builder(this@MainActivity)
        val setDifficultyView = layoutInflater.inflate(R.layout.difficulity, null)
        val diff5 = setDifficultyView.findViewById<ImageButton>(R.id.diff5Button)
        val diff4 = setDifficultyView.findViewById<ImageButton>(R.id.diff4Button)
        val diff3 = setDifficultyView.findViewById<ImageButton>(R.id.diff3Button)
        val diff2 = setDifficultyView.findViewById<ImageButton>(R.id.diff2Button)
        val diff1 = setDifficultyView.findViewById<ImageButton>(R.id.diff1Button)
        mBuilder.setView(setDifficultyView)
        val difficultyPopUp = mBuilder.create()
        difficultyPopUp.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val currentMapNo = getIntInfo("mapNo")
        if (currentMapNo == 0){
            // user must choose the difficulty (cannot cancel) if no map chosen yet (on new game start)
            difficultyPopUp.setCancelable(false)
            difficultyPopUp.setCanceledOnTouchOutside(false)
        }
        // disabling the difficulty levels which can be chosen
        // based on the current chosen difficulty level (relevant during map upgrade)
        if (currentMapNo < 5){
            setUpActiveButton(difficultyPopUp, diff5, 5)
        } else {
            grayOutButton(diff5, R.drawable.ic_diff5)
        }
        if (currentMapNo < 4){
            setUpActiveButton(difficultyPopUp, diff4, 4)
        } else {
            grayOutButton(diff4, R.drawable.ic_diff4)
        }
        if (currentMapNo < 3){
            setUpActiveButton(difficultyPopUp, diff3, 3)
        } else {
            grayOutButton(diff3, R.drawable.ic_diff3)
        }
        if (currentMapNo < 2){
            setUpActiveButton(difficultyPopUp, diff2, 2)
        } else {
            grayOutButton(diff2, R.drawable.ic_diff2)
        }
        if (currentMapNo < 1){
            setUpActiveButton(difficultyPopUp, diff1, 1)
        } else {
            grayOutButton(diff1, R.drawable.ic_diff1)
        }

        difficultyPopUp.setOnCancelListener{
            unblockInput()
        }

        difficultyPopUp.show()
    }

    fun grayOutButton(ib : ImageButton, resource : Int){
        val res = baseContext.resources.getDrawable(resource).mutate()
        res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        ib.setImageDrawable(res)
        ib.isEnabled = false
    }

    fun setUpActiveButton(ad: AlertDialog, ib: ImageButton, mapNo: Int){
        ib.setOnClickListener {
            KmlLoaderAsync(KmlReceiver()).execute(mapNo)
            ad.dismiss()
        }
    }

    private inner class KmlReceiver : AsyncCompleteListener<Int> {
        override fun asyncComplete(result: Int?) {
            val mapNo = result
            if (mapNo == -1 || mapNo == null){ // an error occurred
                unblockInput()
            } else {
                setIntInfo("mapNo", mapNo)
                setNewGame(false)
                unblockInput()
            }
        }
    }

    fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start
}
