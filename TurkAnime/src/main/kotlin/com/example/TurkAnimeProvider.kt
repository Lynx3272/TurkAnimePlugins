package com.example

import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.net.URLEncoder

data class JikanSearchResponse(
    val data: List<JikanAnime>
)

data class JikanAnime(
    val mal_id: Int,
    val title: String?,
    val synopsis: String?,
    val images: JikanImages?
)

data class JikanImages(
    val jpg: JikanJpg?
)

data class JikanJpg(
    val image_url: String?
)

data class JikanEpisodesResponse(
    val data: List<JikanEpisode>
)

data class JikanEpisode(
    val mal_id: Int,
    val title: String?,
    val episode: Int?
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

        val encodedQuery = URLEncoder.encode(
            query.trim(),
            "UTF-8"
        )

        val response = app.get(
            "$mainUrl/anime?q=$encodedQuery&limit=20"
        ).text

        val result = parseJson<JikanSearchResponse>(response)

        return result.data.mapNotNull { anime ->

            val title = anime.title
                ?: return@mapNotNull null

            newAnimeSearchResponse(
                title,
                "jikan:${anime.mal_id}",
                TvType.Anime
            ) {
                this.posterUrl = anime.images?.jpg?.image_url
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {

        val malId = url.removePrefix("jikan:")

        val animeResponse = app.get(
            "$mainUrl/anime/$malId/full"
        ).text

        val anime = parseJson<JikanAnime>(animeResponse)

        val episodesResponse = app.get(
            "$mainUrl/anime/$malId/episodes?limit=100"
        ).text

        val episodeData = parseJson<JikanEpisodesResponse>(
            episodesResponse
        )

        val episodes = episodeData.data
            .sortedBy { it.episode ?: Int.MAX_VALUE }
            .mapNotNull { episode ->

                val episodeNumber =
                    episode.episode
                        ?: return@mapNotNull null

                newEpisode(
                    "jikan-episode:${episode.mal_id}"
                ) {
                    this.name = episode.title
                    this.episode = episodeNumber
                }
            }

        return newAnimeLoadResponse(
            anime.title ?: "Unknown",
            url,
            TvType.Anime
        ) {
            this.posterUrl =
                anime.images?.jpg?.image_url

            this.plot = anime.synopsis

            this.episodes = mutableMapOf(
                DubStatus.Subbed to episodes
            )
        }
    }
}