package com.example.wilko.songle.downloaders

/**
 * Created by wilko on 10/24/2017.
 *
 * enum which indicates what stage the downloading process is on
 *
 */
enum class DownloadType{
    SONGS,
    NO_NEW_SONGS,
    LYRICS,
    KLMS,
    IMGS
}