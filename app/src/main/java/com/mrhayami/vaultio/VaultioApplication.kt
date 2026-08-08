package com.mrhayami.vaultio

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.imageLoader
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.VaultioDatabase
import com.mrhayami.vaultio.data.remote.GitHubReleasesApi
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.TcgDexApi
import com.mrhayami.vaultio.data.repository.AppUpdateRepository
import com.mrhayami.vaultio.data.repository.GeminiNanoClientImpl
import com.mrhayami.vaultio.data.repository.GradingRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.data.update.UpdateNotificationHelper
import com.mrhayami.vaultio.data.workers.AppUpdateWorker
import com.mrhayami.vaultio.data.workers.CollectionSnapshotWorker
import com.mrhayami.vaultio.data.workers.PriceUpdateWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class VaultioApplication : Application() {

    lateinit var repository: VaultioRepository
    lateinit var gradingRepository: GradingRepository
    lateinit var userPreferencesRepository: UserPreferencesRepository
    lateinit var appUpdateRepository: AppUpdateRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        val database = VaultioDatabase.getDatabase(this)
        userPreferencesRepository = UserPreferencesRepository(this)

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
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

        val gitHubRetrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val gitHubReleasesApi = gitHubRetrofit.create(GitHubReleasesApi::class.java)

        repository = VaultioRepository(
            context = this,
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
            userPreferencesRepository = userPreferencesRepository,
            imageLoader = this.imageLoader
        )

        gradingRepository = GradingRepository(
            context = this,
            cardGradeDao = database.cardGradeDao(),
            geminiNanoClient = GeminiNanoClientImpl()
        )

        appUpdateRepository = AppUpdateRepository(
            context = this,
            gitHubReleasesApi = gitHubReleasesApi,
            okHttpClient = okHttpClient,
            userPreferencesRepository = userPreferencesRepository
        )

        UpdateNotificationHelper.ensureChannel(this)

        scheduleDailySnapshot()
        scheduleDailyPriceUpdate()
        applicationScope.launch {
            if (userPreferencesRepository.autoUpdateEnabled.first()) {
                scheduleAppUpdateChecks()
            }
        }
    }

    fun scheduleAppUpdateChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("app_update")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AppUpdateRepository.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelAppUpdateChecks() {
        WorkManager.getInstance(this).cancelUniqueWork(AppUpdateRepository.WORK_NAME)
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

    private fun scheduleDailyPriceUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<PriceUpdateWorker>(24, TimeUnit.HOURS)
            .addTag("price_update")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_price_update",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
