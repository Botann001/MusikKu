package com.example.musikku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musikku.data.local.dao.FavoriteDao
import com.example.musikku.data.local.dao.PlaylistDao
import com.example.musikku.data.local.dao.SongDao
import com.example.musikku.data.local.entity.FavoriteEntity
import com.example.musikku.data.local.entity.PlaylistEntity
import com.example.musikku.data.local.entity.PlaylistSongEntity
import com.example.musikku.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        /** Migrasi dari Versi 1 (hanya tabel songs dasar) ke Versi 2 (tambah source & tabel playlist/favorit) */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'LOCAL'")
                db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`songId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (`playlistId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `songId`))")
            }
        }

        /** Migrasi dari Versi 2 ke Versi 3 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Skema v2 dan v3 kompatibel
            }
        }

        /** Migrasi dari Versi 3 ke Versi 4 (tambah flag isFavorite di tabel songs) */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `songs` SET `isFavorite` = 1 WHERE `id` IN (SELECT `songId` FROM `favorites`)")
            }
        }

        /** Migrasi dari Versi 4 ke Versi 5 (tambah kolom filePath untuk lagu terunduh) */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `filePath` TEXT DEFAULT NULL")
            }
        }

        /** Migrasi langsung dari Versi 1 ke Versi 4 */
        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'LOCAL'")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`songId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (`playlistId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `songId`))")
            }
        }

        /** Migrasi langsung dari Versi 2 ke Versi 4 */
        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `songs` SET `isFavorite` = 1 WHERE `id` IN (SELECT `songId` FROM `favorites`)")
            }
        }

        /** Migrasi langsung dari Versi 1 ke Versi 5 */
        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'LOCAL'")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `filePath` TEXT DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`songId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (`playlistId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `songId`))")
            }
        }

        /** Migrasi langsung dari Versi 2 ke Versi 5 */
        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `filePath` TEXT DEFAULT NULL")
                db.execSQL("UPDATE `songs` SET `isFavorite` = 1 WHERE `id` IN (SELECT `songId` FROM `favorites`)")
            }
        }

        /** Migrasi langsung dari Versi 3 ke Versi 5 */
        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `filePath` TEXT DEFAULT NULL")
                db.execSQL("UPDATE `songs` SET `isFavorite` = 1 WHERE `id` IN (SELECT `songId` FROM `favorites`)")
            }
        }
    }
}
