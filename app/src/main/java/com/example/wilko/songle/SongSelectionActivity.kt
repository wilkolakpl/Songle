package com.example.wilko.songle

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_song_selection.*

class SongSelectionActivity : AppCompatActivity() {

    private val dbHandler = MySongDBHandler(this)
    private var songs = mutableListOf<Song>()

    private val adapter = MyAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        dbHandler.populateList(songs)

        songList.adapter = adapter
        songList.isClickable = true
        songList.setOnItemClickListener { parent, view, position, id ->
            Toast.makeText(this, "Song Selected:"+" "+songs[position].title,Toast.LENGTH_SHORT).show()
            val returnIntent = Intent()
            returnIntent.putExtra("songNo", songs[position].number)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private inner class MyAdapter(context : Context): BaseAdapter() {

        private var mContext: Context

        init {
            mContext = context
        }

        override fun getCount(): Int {
            return songs.size
        }

        override fun getItemId(position : Int): Long {
            return position.toLong()
        }

        override fun getItem(position : Int): Any {
            return songs[position]
        }

        override fun getView(position : Int, convertView : View?, viewGroup: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)

            val rowMain = layoutInflater.inflate(R.layout.row_main, viewGroup, false)

            val songTitle = rowMain.findViewById<TextView>(R.id.songTitle)
            songTitle.text = songs[position].title

            val artistName = rowMain.findViewById<TextView>(R.id.artistName)
            artistName.text = songs[position].artist

            return rowMain
        }
    }
}
