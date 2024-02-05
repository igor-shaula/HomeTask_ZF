package com.igor_shaula.api_polling

import android.app.Application
import android.os.StrictMode
import com.igor_shaula.api_polling.data_layer.DefaultVehiclesRepository
import com.igor_shaula.api_polling.data_layer.data_sources.API_BASE_URL
import com.igor_shaula.api_polling.data_layer.data_sources.FakeDataSource
import com.igor_shaula.api_polling.data_layer.data_sources.NetworkDataSource
import com.igor_shaula.api_polling.data_layer.data_sources.di.DaggerRepositoryComponent
import com.igor_shaula.api_polling.data_layer.data_sources.di.RepositoryComponent
import com.igor_shaula.api_polling.data_layer.data_sources.retrofit.VehicleRetrofitNetworkServiceImpl
import com.igor_shaula.api_polling.data_layer.data_sources.retrofit.VehiclesRetrofitNetworkService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

//val TIME_TO_SHOW_GOTO_FAKE_DIALOG = booleanPreferencesKey("timeToShowGoToFakeDialog")

//private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "localDataStore")

class ThisApp : Application() {

    override fun onCreate() {
        StrictMode.enableDefaults() // https://developer.android.com/reference/android/os/StrictMode.html
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        repositoryComponent = buildRepositoryComponent()
    }

    private fun buildRepositoryComponent(): RepositoryComponent =
        DaggerRepositoryComponent.builder()
            .build()

    fun getRepository(): DefaultVehiclesRepository = Companion.getRepository()

//    fun readNeedFakeDialogFromLocalPrefs(): Flow<Boolean> =
//        dataStore.data.map { preferences ->
//            preferences[TIME_TO_SHOW_GOTO_FAKE_DIALOG] ?: false
//        }

//    suspend fun saveNeedFakeDialogToLocalPrefs(showFakeDataNextTime: Boolean) {
//        dataStore.edit { preferences ->
//            preferences[TIME_TO_SHOW_GOTO_FAKE_DIALOG] = showFakeDataNextTime
//        }
//    }

    enum class ActiveDataSource {
        NETWORK, FAKE
    }

    companion object {

        private lateinit var repositoryComponent: RepositoryComponent

        fun getRepositoryComponent(): RepositoryComponent = repositoryComponent

        // TODO: current solution is TEMPORARY - later move all DataSource usage logic into the Repository level

        private val vrns: VehiclesRetrofitNetworkService =
            Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VehiclesRetrofitNetworkService::class.java)

        private val networkDataRepository: DefaultVehiclesRepository by lazy {
            DefaultVehiclesRepository(
                NetworkDataSource(VehicleRetrofitNetworkServiceImpl(vrns)),
                FakeDataSource(),
                ActiveDataSource.NETWORK
            )
        }

        private val fakeDataRepository: DefaultVehiclesRepository by lazy {
            DefaultVehiclesRepository(
                NetworkDataSource(VehicleRetrofitNetworkServiceImpl(vrns)),
                FakeDataSource(),
                ActiveDataSource.FAKE
            )
        }

        private lateinit var currentRepository: DefaultVehiclesRepository

        fun getRepository(): DefaultVehiclesRepository {
            if (!this::currentRepository.isInitialized) {
                currentRepository = networkDataRepository
            }
            return currentRepository
        }

        /**
         * Switches the DataSource for the DefaultVehiclesRepository between Network and Fake
         */
        fun switchActiveDataSource(type: ActiveDataSource): DefaultVehiclesRepository {
            currentRepository = when (type) {
                ActiveDataSource.FAKE -> fakeDataRepository
                ActiveDataSource.NETWORK -> networkDataRepository
            }
            return currentRepository
        }
    }
}
