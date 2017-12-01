package com.example.wilko.songle

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_check_progress.*
import kotlinx.android.synthetic.main.content_check_progress.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.lang.ref.WeakReference

class CheckProgressActivity : AppCompatActivity() {

    lateinit var alert : AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_progress)
        setSupportActionBar(toolbar)

        LyricWordSplicer(LyricStrLoad(), WeakReference<Context>(applicationContext)).execute()

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

    override fun onStart() {
        super.onStart()
        LyricWordSplicer(LyricStrLoad(), WeakReference<Context>(applicationContext)).execute()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            alert.dismiss()
        } catch (e: UninitializedPropertyAccessException){

        }
    }

    private inner class LyricStrLoad : AsyncCompleteListener<String>{
        override fun asyncComplete(result: String?) {
            if (result != null){
                collectedWords.text = "\n\n" + result
            }
        }
    }

}
