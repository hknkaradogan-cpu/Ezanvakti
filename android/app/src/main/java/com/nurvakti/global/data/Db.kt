package com.nurvakti.global.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable @Entity(tableName = "prayer_log", primaryKeys = ["date", "prayer"])
data class PrayerLog(val date: String, val prayer: String, val done: Boolean)

@Serializable @Entity(tableName = "kaza")
data class Kaza(@PrimaryKey val prayer: String, val count: Int)

@Serializable @Entity(tableName = "fast_log")
data class FastLog(@PrimaryKey val date: String, val status: String) // FASTED | MISSED

@Serializable @Entity(tableName = "zikir_session")
data class ZikirSession(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val count: Int, val date: String, val ts: Long)

@Serializable @Entity(tableName = "custom_zikir")
data class CustomZikir(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val target: Int)

@Serializable @Entity(tableName = "bookmark")
data class Bookmark(@PrimaryKey(autoGenerate = true) val id: Long = 0, val surah: Int, val ayah: Int, val kind: String, val ts: Long)

@Serializable @Entity(tableName = "saved_place")
data class SavedPlaceRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val region: String, val lat: Double, val lng: Double, val tz: String, val country: String)

@Serializable @Entity(tableName = "favorite", primaryKeys = ["kind", "key"])
data class Favorite(val kind: String, val key: String)

@Entity(tableName = "download")
data class DownloadRow(
    @PrimaryKey val id: String, val kind: String, val title: String, val url: String, val path: String,
    val total: Long, val done: Long, val status: String, val error: String?, val ts: Long,
)

@Dao interface AppDao {
    @Query("SELECT * FROM prayer_log WHERE date BETWEEN :from AND :to") fun logs(from: String, to: String): Flow<List<PrayerLog>>
    @Query("SELECT * FROM prayer_log WHERE date BETWEEN :from AND :to") suspend fun logsNow(from: String, to: String): List<PrayerLog>
    @Upsert suspend fun setLog(l: PrayerLog)
    @Query("SELECT * FROM kaza") fun kaza(): Flow<List<Kaza>>
    @Upsert suspend fun setKaza(k: Kaza)
    @Query("SELECT * FROM fast_log ORDER BY date") fun fasts(): Flow<List<FastLog>>
    @Upsert suspend fun setFast(f: FastLog)
    @Query("DELETE FROM fast_log WHERE date = :date") suspend fun delFast(date: String)
    @Insert suspend fun addZikir(z: ZikirSession)
    @Query("SELECT * FROM zikir_session ORDER BY ts DESC LIMIT 200") fun zikirHistory(): Flow<List<ZikirSession>>
    @Query("SELECT COALESCE(SUM(count),0) FROM zikir_session") fun zikirTotal(): Flow<Long>
    @Query("SELECT COALESCE(SUM(count),0) FROM zikir_session WHERE date = :date") fun zikirToday(date: String): Flow<Long>
    @Query("SELECT * FROM custom_zikir") fun customZikir(): Flow<List<CustomZikir>>
    @Insert suspend fun addCustomZikir(z: CustomZikir)
    @Query("DELETE FROM custom_zikir WHERE id = :id") suspend fun delCustomZikir(id: Long)
    @Query("SELECT * FROM bookmark ORDER BY ts DESC") fun bookmarks(): Flow<List<Bookmark>>
    @Insert suspend fun addBookmark(b: Bookmark)
    @Query("DELETE FROM bookmark WHERE surah = :s AND ayah = :a AND kind = :kind") suspend fun delBookmark(s: Int, a: Int, kind: String)
    @Query("SELECT * FROM saved_place") fun places(): Flow<List<SavedPlaceRow>>
    @Insert suspend fun addPlace(p: SavedPlaceRow)
    @Query("DELETE FROM saved_place WHERE id = :id") suspend fun delPlace(id: Long)
    @Query("SELECT * FROM favorite") fun favorites(): Flow<List<Favorite>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addFav(f: Favorite)
    @Query("DELETE FROM favorite WHERE kind = :kind AND `key` = :key") suspend fun delFav(kind: String, key: String)
    @Query("SELECT * FROM download ORDER BY ts DESC") fun downloads(): Flow<List<DownloadRow>>
    @Query("SELECT * FROM download WHERE id = :id") suspend fun download(id: String): DownloadRow?
    @Upsert suspend fun putDownload(d: DownloadRow)
    @Query("UPDATE download SET done = :done, total = :total, status = :status, error = :error WHERE id = :id")
    suspend fun progress(id: String, done: Long, total: Long, status: String, error: String?)
    @Query("DELETE FROM download WHERE id = :id") suspend fun delDownload(id: String)

    // dışa aktarma
    @Query("SELECT * FROM prayer_log") suspend fun allLogs(): List<PrayerLog>
    @Query("SELECT * FROM kaza") suspend fun allKaza(): List<Kaza>
    @Query("SELECT * FROM fast_log") suspend fun allFasts(): List<FastLog>
    @Query("SELECT * FROM zikir_session") suspend fun allZikir(): List<ZikirSession>
    @Query("SELECT * FROM bookmark") suspend fun allBookmarks(): List<Bookmark>
    @Query("SELECT * FROM favorite") suspend fun allFavs(): List<Favorite>
    @Query("SELECT * FROM saved_place") suspend fun allPlaces(): List<SavedPlaceRow>
}

@Database(
    entities = [PrayerLog::class, Kaza::class, FastLog::class, ZikirSession::class, CustomZikir::class, Bookmark::class,
        SavedPlaceRow::class, Favorite::class, DownloadRow::class],
    version = 1, exportSchema = true,
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "nurvakti.db").build().also { inst = it }
        }
    }
}
