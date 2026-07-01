package com.example.albuddy.di

import com.example.albuddy.data.repository.SettingsRepository
import com.example.albuddy.network.HomeAssistantApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @DefaultOkHttpClient
    fun provideDefaultOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @AuthOkHttpClient
    fun provideAuthOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val (url, token) = runBlocking {
                val u = settingsRepository.haUrl.firstOrNull()
                val t = settingsRepository.haToken.firstOrNull()
                Pair(u, t)
            }

            var request = chain.request()
            if (!url.isNullOrBlank() && !token.isNullOrBlank()) {
                val newBaseUrl = url.toHttpUrlOrNull()
                if (newBaseUrl != null) {
                    val newUrl = request.url.newBuilder()
                        .scheme(newBaseUrl.scheme)
                        .host(newBaseUrl.host)
                        .port(newBaseUrl.port)
                        .build()

                    request = request.newBuilder()
                        .url(newUrl)
                        .header("Authorization", "Bearer $token")
                        .build()
                }
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@AuthOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost/") // Placeholder, overridden by Interceptor dynamically
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHomeAssistantApi(retrofit: Retrofit): HomeAssistantApi {
        return retrofit.create(HomeAssistantApi::class.java)
    }
}
