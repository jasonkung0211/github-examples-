package com.github.jasonkung.github

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.github.jasonkung.github.api.GitHubService
import com.github.jasonkung.github.ui.main.MainViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree());
        }

        startKoin {
            androidContext(this@App)
            modules(listOf(
                module {
                    single<OkHttpClient> {
                        val context: Context = get()
                        OkHttpClient.Builder()
                            .addNetworkInterceptor(StethoInterceptor())
                            //.cache(Cache(context.cacheDir, 1024 * 1024 * 2))
                            .build()
                    }

                    single<GitHubService> {
                        Retrofit.Builder()
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .addConverterFactory(MoshiConverterFactory.create())
                            .baseUrl("https://api.github.com/")
                            .client(get())
                            .build()
                            .create(GitHubService::class.java)
                    }

                    viewModel {
                        MainViewModel(get())
                    }
                }
            ))
        }
    }
}