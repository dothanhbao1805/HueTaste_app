package com.example.huetaste.di

import com.example.huetaste.BuildConfig
import com.example.huetaste.data.remote.AuthApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    const val PLAIN_CLIENT = "plain"

    /** A client with no token interceptor or authenticator, used only for the sign-in endpoints. */
    @Provides
    @Singleton
    @Named(PLAIN_CLIENT)
    fun providePlainOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(@Named(PLAIN_CLIENT) client: OkHttpClient, gson: Gson): AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(AuthApi::class.java)
}
