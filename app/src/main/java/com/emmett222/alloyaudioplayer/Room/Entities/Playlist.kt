package com.emmett222.alloyaudioplayer.Room.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.nio.file.Path

/**
 * Playlist entity. Keeps title, songs, icon path, description, tags, mood.
 *
 * @author Emmett Grebe
 * @version 8-24-2026
 */
@Entity(tableName = "playlist_table")
class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "songs") var songs: Array<Song>?,
    @ColumnInfo(name = "iconPath") var iconPath: Path?,
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "tags") var tags: Array<String>?,
    @ColumnInfo(name = "mood") var mood: Array<String>?,
) {
}