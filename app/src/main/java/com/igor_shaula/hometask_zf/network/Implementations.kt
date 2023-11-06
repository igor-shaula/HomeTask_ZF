package com.igor_shaula.hometask_zf.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CarDataNetworksServiceImpl : CarDataNetworksService {

    override suspend fun getCarList(): Response<List<CarModel>> {
        val retrofit = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(CarDataNetworksService::class.java)
        return service.getCarList()
    }
}