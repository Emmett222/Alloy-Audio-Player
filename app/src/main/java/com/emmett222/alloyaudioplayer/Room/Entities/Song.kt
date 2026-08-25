package com.emmett222.alloyaudioplayer.Room.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.nio.file.Path

/**
 * Song entity. Keeps title, filepath, artist, length, and tags.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Entity(tableName = "song_table")
class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "filePath") var filePath: Path,
    @ColumnInfo(name = "artist") var artist: String,
    @ColumnInfo(name = "length") var length: String,
    @ColumnInfo(name = "tags") var tags: Array<String>?,
    @ColumnInfo(name = "timestamps") var timestamps: Array<Long>?,
    @ColumnInfo(name = "views") var views: Int = 0,
) {
}