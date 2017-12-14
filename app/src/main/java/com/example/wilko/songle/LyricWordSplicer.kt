package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import com.example.wilko.songle.databaseHelpers.DBCollectedWords
import com.example.wilko.songle.databaseHelpers.DBSongs
import com.example.wilko.songle.utils.AsyncCompleteListener

/**
 * Created by wilko on 10/29/2017.
 *
 * This async task prepares the string to be displayed in the progress activity.
 */

class LyricWordSplicer(private val caller : AsyncCompleteListener<String>) :
        AsyncTask<Unit, Void, String>() {

    override fun doInBackground(vararg vararg: Unit): String{

        // getting the complete lyrics
        val dbSongHandler = DBSongs
        val originalLyrics = dbSongHandler.getProp(getCurrSong(App.instance), "lyric")

        // getting the collected words so far
        val dbCollectedWordsHandler = DBCollectedWords
        val collectedWords = hashMapOf<String, Boolean>()
        dbCollectedWordsHandler.populateHashMap(collectedWords)

        if (collectedWords.isEmpty()){ // no words collected yet
            return App.instance.getString(R.string.no_words_yet)
        } else {
            // splicing the displayed string together from the complete lyrics and whitespaces
            val newString = StringBuilder()
            val lines = originalLyrics.split("\n")
            for (lineNo in (0..lines.size-1)){
                val words = lines[lineNo].split(" ")
                for (wordNo in (0..words.size-1)){
                    // checking if a word has been collected, if so, adding the word to displayed string
                    if (collectedWords.containsKey((lineNo + 1).toString() + ":" + (wordNo + 1).toString())){
                        if (wordNo == words.size -1){
                            newString.append(words[wordNo])
                        } else { // adding whitespace after word, unless it is the last one in line
                            newString.append(words[wordNo]+" ")
                        }
                    } else { // if it has not been collected, replacing the word's characters with whitespaces
                        if (wordNo == words.size -1){
                            newString.append(words[wordNo].replace(Regex("[^ ]"), " "))
                        } else { // adding whitespace after word, unless it is the last one in line
                            newString.append(words[wordNo].replace(Regex("[^ ]"), " ") + " ")
                        }
                    }
                } // adding newline after iterating through one line of words, except the last
                if (lineNo != lines.size-1){
                    newString.append("\n")
                }
            }
            return newString.toString()
        }
    }

    fun getCurrSong(context: Context): Int{
        val sharedPref = context.getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        return sharedPref.getInt("currentSong", 0)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        caller.asyncComplete(result)
    }
}