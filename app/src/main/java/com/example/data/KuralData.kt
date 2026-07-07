package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
@Entity(tableName = "kurals")
data class Kural(
    @PrimaryKey
    @Json(name = "Number") val number: Int,
    @Json(name = "Line1") val line1: String,
    @Json(name = "Line2") val line2: String,
    @Json(name = "Translation") val translation: String,
    @Json(name = "mv") val mv: String?,
    @Json(name = "sp") val sp: String?,
    @Json(name = "mk") val mk: String?,
    @Json(name = "chapter") val chapter: String? = "",
    @Json(name = "section") val section: String? = "",
    @Json(name = "couplet") val couplet: String?,
    val isFavorite: Boolean = false
)

@Dao
interface KuralDao {
    @Query("SELECT * FROM kurals ORDER BY number ASC")
    fun getAllKurals(): Flow<List<Kural>>

    @Query("SELECT * FROM kurals WHERE number = :number LIMIT 1")
    suspend fun getKuralByNumber(number: Int): Kural?

    @Query("SELECT * FROM kurals WHERE isFavorite = 1 ORDER BY number ASC")
    fun getFavoriteKurals(): Flow<List<Kural>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKurals(kurals: List<Kural>)

    @Query("UPDATE kurals SET isFavorite = :isFavorite WHERE number = :number")
    suspend fun updateFavoriteStatus(number: Int, isFavorite: Boolean)

    @Query("UPDATE kurals SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Query("SELECT COUNT(*) FROM kurals")
    suspend fun getKuralCount(): Int
}

@Database(entities = [Kural::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kuralDao(): KuralDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thirukkural_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class ThirukkuralResponse(
    @Json(name = "Thirukkural") val thirukkural: List<Kural>
)

interface ThirukkuralApiService {
    @GET("Katheesh/Thirukkural/master/thirukkural.json")
    suspend fun getThirukkural(): ThirukkuralResponse
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val apiService: ThirukkuralApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ThirukkuralApiService::class.java)
    }
}
