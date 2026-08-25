package com.emmett222.alloyaudioplayer.Room.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.nio.file.Path

/**
 * Song entity. Keeps title, filepath, artist, length, and tags.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Entity(tableName = "playlist_table")
class Song(
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "filePath") var filePath: Path,
    @ColumnInfo(name = "artist") var artist: String,
    @ColumnInfo(name = "length") var length: String,
    @ColumnInfo(name = "tags") var tags: Array<String>?,
) {
}