package com.example.wilko.songle

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.wilko.songle.utils.AsyncCompleteListener
import kotlinx.android.synthetic.main.activity_check_progress.*
import kotlinx.android.synthetic.main.content_check_progress.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

class CheckProgressActivity : AppCompatActivity() {

    lateinit var alert : AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_progress)
        setSupportActionBar(toolbar)

        // loading the to-be-displayed spatially arranged collected words
        LyricWordSplicer(LyricStrLoad()).execute()

        guessButton.setOnClickListener { _ ->
            val intent = Intent(this, SongSelectionActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            startActivity(intent)
            finish()
        }

        resetButton.setOnClickListener { _ ->
            alert = alert(getString(R.string.give_up_message)) {
                yesButton { val returnIntent = Intent()
                    returnIntent.putExtra("reset", true)
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish() }
                noButton {}
            }.show()
        }

        upgradeButton.setOnClickListener { _ ->
            alert = alert(getString(R.string.upgrade_message)) {
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
        LyricWordSplicer(LyricStrLoad()).execute()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            alert.dismiss()
        } catch (e: UninitializedPropertyAccessException){
            Log.i(localClassName, "alert uninitialized")
        }
    }

    private inner class LyricStrLoad : AsyncCompleteListener<String> {
        override fun asyncComplete(result: String?) {
            if (result != null){
                collectedWords.text = "\n\n" + result
            }
        }
    }

}
