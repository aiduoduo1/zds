package com.jj.zds.app

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.jj.core.JiangCore
import com.jj.core.config.JiangConfig
import com.jj.network.JiangNetwork
import com.jj.network.config.JiangNetworkConfig
import com.jj.network.config.JiangNetworkLogLevel
import com.jj.network.header.JiangNetworkHeaders
import com.jj.storage.JiangStorage
import com.jj.storage.config.JiangStorageConfig
import com.jj.ui.toast.JToast
import com.jj.zds.BuildConfig
import com.jj.zds.data.local.ZdsStorageKeys
import okhttp3.Interceptor

class ZdsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        JiangCore.init(
            application = this,
            config = JiangConfig(
                debug = BuildConfig.DEBUG,
                appName = "zds",
                baseUrl = BASE_URL,
            ),
        )
        JiangStorage.init(
            application = this,
            config = JiangStorageConfig(
                defaultKvId = "zds_default",
                debug = BuildConfig.DEBUG,
            ),
        )
        JiangNetwork.init(
            config = JiangNetworkConfig(
                baseUrl = BASE_URL,
                dynamicHeadersProvider = {
                    mapOf(
                        "MES-UP-TOKEN" to JiangStorage.kv.getString(
                            ZdsStorageKeys.MES_UP_TOKEN,
                            "",
                        ).orEmpty(),
                        "User-Agent" to "zds-android",
                    )
                },
                debug = BuildConfig.DEBUG,
                interceptors = createNetworkInterceptors(),
                logLevel = if (BuildConfig.DEBUG) {
                    JiangNetworkLogLevel.BODY
                } else {
                    JiangNetworkLogLevel.NONE
                },
            ),
        )
        JToast.init(this)
    }

    private fun createNetworkInterceptors(): List<Interceptor> {
        if (!BuildConfig.DEBUG) {
            return emptyList()
        }
        return listOf(
            ChuckerInterceptor.Builder(this)
                .maxContentLength(250_000L)
                .redactHeaders(
                    JiangNetworkHeaders.MES_UP_TOKEN,
                    JiangNetworkHeaders.AUTHORIZATION,
                    "Authorization",
                    "Cookie",
                    "token",
                    "password",
                    "pwd",
                )
                .alwaysReadResponseBody(true)
                .build(),
        )
    }

    private companion object {
        const val BASE_URL = "http://192.168.1.216:81/"
    }
}
