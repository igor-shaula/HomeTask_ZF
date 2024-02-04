package com.igor_shaula.api_polling.data_layer.data_sources.di

import com.igor_shaula.api_polling.ThisApp
import com.igor_shaula.api_polling.data_layer.DefaultVehiclesRepository
import com.igor_shaula.api_polling.data_layer.VehiclesRepository
import com.igor_shaula.api_polling.data_layer.data_sources.API_BASE_URL
import com.igor_shaula.api_polling.data_layer.data_sources.FakeDataSource
import com.igor_shaula.api_polling.data_layer.data_sources.NetworkDataSource
import com.igor_shaula.api_polling.data_layer.data_sources.retrofit.VehicleRetrofitNetworkServiceImpl
import com.igor_shaula.api_polling.data_layer.data_sources.retrofit.VehiclesRetrofitNetworkService
import com.igor_shaula.api_polling.data_layer.polling_engines.JavaTPEBasedPollingEngine
import com.igor_shaula.api_polling.data_layer.polling_engines.PollingEngine
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
class RepositoryModule {

    @[Provides RepositoryScope]
    fun provideActiveDataStore(): ThisApp.ActiveDataSource = ThisApp.ActiveDataSource.FAKE

    @[Provides RepositoryScope]
    fun providePollingEngine(): PollingEngine =
        JavaTPEBasedPollingEngine(5)

    @[Provides RepositoryScope]
    fun provideRepository(): VehiclesRepository {
        val vrns: VehiclesRetrofitNetworkService =
            Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VehiclesRetrofitNetworkService::class.java)
        return DefaultVehiclesRepository(
            NetworkDataSource(VehicleRetrofitNetworkServiceImpl(vrns)),
            FakeDataSource(),
            ThisApp.ActiveDataSource.FAKE
        )
    }
}

@Module
class RetrofitModule {

    @[Provides RepositoryScope] // replace RepositoryScope with RetrofitScope later
    fun provideRetrofitService(): VehiclesRetrofitNetworkService =
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VehiclesRetrofitNetworkService::class.java)
}