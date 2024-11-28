package com.example.controlward

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("posts/")
    fun getPosts(): Call<List<Map<String, Any>>>

    @POST("posts/")
    fun createPost(@Body post: Any): Call<Unit>
}

object RetrofitClient {
    private const val BASE_URL = BuildConfig.localhost

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

@Suppress("UNCHECKED_CAST")
fun getFromDB(onSuccess: (List<DisasterModel>) -> Unit) {
    RetrofitClient.apiService.getPosts().enqueue(object : Callback<List<Map<String, Any>>> {
        override fun onResponse(
            call: Call<List<Map<String, Any>>>,
            response: Response<List<Map<String, Any>>>
        ) {
            if (response.isSuccessful) {
                val disasterList = response.body()?.map { item ->
                    val data = DisasterModel(
                        id = item["id"].toString(),
                        userId = item["user_id"].toString(),
                        text = item["text"].toString(),
                        image = item["image_path"].toString(),
                        location = item["location"] as List<Double>,
                        category = item["category"].toString(),
                        accuracy = item["accuracy"].toString()
                    )
                    when(data.category){
                        "Crime" -> Value.disasterListCrime.add(data)
                        "EarthQuake" -> Value.disasterListEarthQuake.add(data)
                        "Flood" -> Value.disasterListFlood.add(data)
                        "HeavySnow" -> Value.disasterListHeavySnow.add(data)
                        "Tsunami" -> Value.disasterListTsunami.add(data)
                        else -> {}
                    }
                } as MutableList<DisasterModel>
                onSuccess(disasterList)
            } else {
                Log.d("testt", response.errorBody().toString())
            }
        }

        override fun onFailure(call: Call<List<Map<String, Any>>>, t: Throwable) {
            Log.d("testt", t.message.toString())
        }
    })
}

fun postToDB(postRequest: Any) {
    RetrofitClient.apiService.createPost(postRequest).enqueue(object : Callback<Unit> {
        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
            if (response.isSuccessful) {
                Log.d("testt", response.code().toString())
            } else {
                Log.e("testt", response.errorBody().toString())
            }
        }

        override fun onFailure(call: Call<Unit>, t: Throwable) {
            Log.d("testt", t.message.toString())
        }
    })
}