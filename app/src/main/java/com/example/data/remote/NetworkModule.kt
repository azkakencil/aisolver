package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

  private val loggingInterceptor =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

  val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .addInterceptor(loggingInterceptor)
      .retryOnConnectionFailure(true)
      .build()
  }

  val aiApiService: AiApiService by lazy {
    Retrofit.Builder()
      .baseUrl(ApiConfig.BASE_URL)
      .client(okHttpClient)
      .build()
      .create(AiApiService::class.java)
  }

  fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }
}
