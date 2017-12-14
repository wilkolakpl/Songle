package com.example.wilko.songle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.wilko.songle.dataClasses.Song
import com.example.wilko.songle.databaseHelpers.DBSongs
import kotlinx.android.synthetic.main.activity_song_selection.*

/**
 *
 * A ListView container activity.
 *
 * credits to Brian Voong, whose YouTube tutorial guide was followed in the creation the ListView adapter
 * https://www.youtube.com/watch?v=EwwdQt3_fFU
 */

class SongSelectionActivity : AppCompatActivity() {

    private val dbHandler = DBSongs
    private var songs = mutableListOf<Song>()

    private val adapter = MyAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        dbHandler.populateList(songs) // getting songs

        songList.adapter = adapter // setting custom adapter
        songList.isClickable = true
        songList.setOnItemClickListener { _, _, position, _ ->
            val returnIntent = Intent()
            returnIntent.putExtra("songNo", songs[position].number)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    private inner class MyAdapter(context : Context): BaseAdapter() {

        private var mContext = context

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
            var row = convertView
            // making use of the ViewHolder class, to prevent calling findViewById every time
            // a list item appears on screen
            val viewHolder : ViewHolder
            if (convertView == null) {
                val layoutInflater = LayoutInflater.from(mContext)
                row = layoutInflater.inflate(R.layout.row_song_selection, viewGroup, false)
                viewHolder = ViewHolder(row)
                row.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }

            viewHolder.songTitle.text = songs[position].title
            viewHolder.artistName.text = songs[position].artist
            return row!!
        }

        private inner class ViewHolder(view: View) {
            var songTitle = view.findViewById<TextView>(R.id.songTitle)
            var artistName = view.findViewById<TextView>(R.id.artistName)
        }
    }
}
