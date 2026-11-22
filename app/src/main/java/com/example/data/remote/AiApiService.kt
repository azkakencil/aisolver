package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AiApiService {
  @Streaming
  @GET(ApiConfig.AI_ENDPOINT)
  suspend fun askAi(
    @Query(ApiConfig.QUERY_PARAM) text: String,
  ): Response<ResponseBody>
}
