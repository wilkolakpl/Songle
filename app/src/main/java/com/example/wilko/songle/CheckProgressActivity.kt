package com.example.wilko.songle

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_check_progress.*
import kotlinx.android.synthetic.main.content_check_progress.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.alert
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

class CheckProgressActivity : AppCompatActivity() {

    lateinit var alert : AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_progress)
        setSupportActionBar(toolbar)

        async(UI){ // avoiding the first, long access time blocking the UI thread
            val lyric = bg{getLyric()}
            collectedWords.text = "\n\n" + lyric.await()
        }

        guessButton.setOnClickListener { _ ->
            val intent = Intent(this, SongSelectionActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            startActivity(intent)
            finish()
        }

        resetButton.setOnClickListener { _ ->
            alert = alert("You are about to clear your progress, proceed?") {
                yesButton { val returnIntent = Intent()
                    returnIntent.putExtra("reset", true)
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish() }
                noButton {}
            }.show()
        }

        upgradeButton.setOnClickListener { _ ->
            alert = alert("You are about to lower the difficulty and the obtainable score, proceed?") {
                yesButton { val upgradeIntent = Intent()
                    upgradeIntent.putExtra("upgrade", true)
                    setResult(Activity.RESULT_OK, upgradeIntent)
                    finish() }
                noButton {}
            }.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            alert.dismiss()
        } catch (e: UninitializedPropertyAccessException){

        }
    }

    fun saveInfo(key: String, int: Int){
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(key, int)
        editor.apply()
    }

    fun getLyric(): String{
        val sharedPref = getSharedPreferences("permStrs", Context.MODE_PRIVATE)
        return sharedPref.getString("lyric", "")
    }

    fun replaceWord(lyric: String, row: Int, column: Int): String {
        val dbHandler = MySongDBHandler(applicationContext)
        val sharedPref = getSharedPreferences("permInts", Context.MODE_PRIVATE)
        val originalLyrics = dbHandler.getProp(sharedPref.getInt("currentSong", 0), "lyric")

        val newString = StringBuilder()
        val lines = lyric.split("\n")
        val origLines = originalLyrics.split("\n")
        for (lineNo in (0..lines.size-1)){
            val words = lines[lineNo].split("\t")
            val origWords = origLines[lineNo].split(" ")
            for (wordNo in (0..words.size-1)){
                if ((lineNo + 1 == row) && (wordNo + 1 == column)){
                    newString.append(origWords[wordNo]+"\t")
                } else {
                    newString.append(words[wordNo]+"\t")
                }
            }
            newString.append("\n")
        }
        return newString.toString()
    }

}
