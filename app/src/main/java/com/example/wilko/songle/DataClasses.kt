package com.example.wilko.songle

/**
 * Created by wilko on 10/13/2017.
 */
data class Song(val number: Int, val artist: String, val title: String, val link: String,
                val lyric: String, val noOfWords: Int, val kmlLocation1: String, val kmlLocation2: String,
                val kmlLocation3: String, val kmlLocation4: String, val kmlLocation5: String)

data class Placemark(val name: String, val description: String, val styleUrl: String, val lat: Double, val long: Double)

data class CollectedWord(val name: String)