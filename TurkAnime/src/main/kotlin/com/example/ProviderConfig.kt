package com.example

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

data class RemoteConfig(
    val test_api_base_url: String?
)

object ProviderConfig {

    private const val CONFIG_URL =
        "https://raw.githubusercontent.com/Lynx3272/TurkAnimePlugins/modern-template/config.json"

    var testApiBaseUrl =
        "https://api.jikan.moe/v4"

    suspend fun refresh() {
        try {
            val response = app.get(CONFIG_URL).text
            val config = parseJson<RemoteConfig>(response)

            config.test_api_base_url?.let {
                testApiBaseUrl = it.trimEnd('/')
            }

        } catch (e: Exception) {
            // Uzak config okunamazsa mevcut varsayılan adres kullanılmaya devam eder.
        }
    }
}