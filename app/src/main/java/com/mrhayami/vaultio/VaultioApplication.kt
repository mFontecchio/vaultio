package com.mrhayami.vaultio

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.VaultioDatabase
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.TcgDexApi
import com.mrhayami.vaultio.data.repository.GeminiNanoClientImpl
import com.mrhayami.vaultio.data.repository.GradingRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.data.workers.CollectionSnapshotWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class VaultioApplication : Application() {

    lateinit var repository: VaultioRepository
    lateinit var gradingRepository: GradingRepository
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()

        val database = VaultioDatabase.getDatabase(this)
        userPreferencesRepository = UserPreferencesRepository(this)

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val tcgDexRetrofit = Retrofit.Builder()
            .baseUrl("https://api.tcgdex.net/v2/en/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val tcgDexApi = tcgDexRetrofit.create(TcgDexApi::class.java)

        val justTcgRetrofit = Retrofit.Builder()
            .baseUrl("https://api.justtcg.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val justTcgApi = justTcgRetrofit.create(JustTcgApi::class.java)

        repository = VaultioRepository(
            setDao = database.setDao(),
            cardDao = database.cardDao(),
            userCardDao = database.userCardDao(),
            folderDao = database.folderDao(),
            priceDao = database.priceDao(),
            apiUsageDao = database.apiUsageDao(),
            telemetryDao = database.telemetryDao(),
            collectionSnapshotDao = database.collectionSnapshotDao(),
            wishlistDao = database.wishlistDao(),
            tcgDexApi = tcgDexApi,
            justTcgApi = justTcgApi,
            userPreferencesRepository = userPreferencesRepository
        )

        gradingRepository = GradingRepository(
            context = this,
            cardGradeDao = database.cardGradeDao(),
            geminiNanoClient = GeminiNanoClientImpl()
        )

        scheduleDailySnapshot()
    }

    private fun scheduleDailySnapshot() {
        val workRequest = PeriodicWorkRequestBuilder<CollectionSnapshotWorker>(24, TimeUnit.HOURS)
            .addTag("collection_snapshot")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_collection_snapshot",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}