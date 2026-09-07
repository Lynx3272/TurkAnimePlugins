package com.example

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

data class JikanResponse(
    val data: List<JikanAnime>
)

data class JikanAnime(
    val mal_id: Int,
    val title: String?,
    val images: JikanImages?
)

data class JikanImages(
    val jpg: JikanJpg?
)

data class JikanJpg(
    val image_url: String?
)

class TurkAnimeProvider : MainAPI() {

    override var mainUrl = "https://api.jikan.moe/v4"
    override var name = "Turk Anime"
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie
    )

    override var lang = "tr"
    override val hasMainPage = false

    override suspend fun search(query: String): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        val response = app.get(
            "$mainUrl/anime?q=${query.trim()}"
        ).text

        val result = parseJson<JikanResponse>(response)

        return result.data.mapNotNull { anime ->

            val title = anime.title ?: return@mapNotNull null
            val id = anime.mal_id.toString()
            val poster = anime.images?.jpg?.image_url

            newAnimeSearchResponse(
                title,
                "jikan:$id"
            ) {
                this.posterUrl = poster
            }
        }
    }
}
