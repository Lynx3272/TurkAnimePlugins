package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class AniListResponse(
    val data: AniListData
)

data class AniListData(
    @JsonProperty("Page")
    val page: AniListPage? = null,

    @JsonProperty("Media")
    val media: AniListMedia? = null
)

data class AniListPage(
    val media: List<AniListMedia> = emptyList()
)

data class AniListMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCover?,
    val bannerImage: String?,
    val description: String?,
    val episodes: Int?,
    val duration: Int?,
    val seasonYear: Int?,
    val format: String?,
    val genres: List<String> = emptyList(),
    val relations: AniListRelations?
)

data class AniListTitle(
    val romaji: String?,
    val english: String?,
    val native: String?
)

data class AniListCover(
    val large: String?
)

data class AniListRelations(
    val edges: List<AniListRelationEdge> = emptyList()
)

data class AniListRelationEdge(
    val relationType: String?,
    val node: AniListRelatedMedia?
)

data class AniListRelatedMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCover?,
    val episodes: Int?,
    val format: String?
)

data class AnimeEpisodeData(
    val mediaId: Int,
    val episodeNumber: Int,
    val category: String
)

class TurkAnimeProvider : MainAPI() {

    override var mainUrl = "https://graphql.anilist.co"
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

        val graphqlQuery = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 20) {
                    media(
                        search: ${'$'}search,
                        type: ANIME
                    ) {
                        id
                        title {
                            romaji
                            english
                            native
                        }
                        coverImage {
                            large
                        }
                    }
                }
            }
        """.trimIndent()

        val body = mapOf(
            "query" to graphqlQuery,
            "variables" to mapOf(
                "search" to query
            )
        ).toJson().toRequestBody(
            "application/json".toMediaType()
        )

        val response = app.post(
            mainUrl,
            requestBody = body
        ).text

        val result = parseJson<AniListResponse>(response)

        return result.data.page?.media?.mapNotNull { anime ->
            val title =
                anime.title.english
                    ?: anime.title.romaji
                    ?: anime.title.native
                    ?: return@mapNotNull null

            newAnimeSearchResponse(
                title,
                "anilist:${anime.id}",
                TvType.Anime
            ) {
                this.posterUrl = anime.coverImage?.large
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): com.lagradost.cloudstream3.LoadResponse? {
        val id = url.removePrefix("anilist:").toIntOrNull()
            ?: return null

        val graphqlQuery = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: ANIME) {
                    id
                    title {
                        romaji
                        english
                        native
                    }
                    coverImage {
                        large
                    }
                    bannerImage
                    description
                    episodes
                    duration
                    seasonYear
                    format
                    genres
                    relations {
                        edges {
                            relationType
                            node {
                                id
                                title {
                                    romaji
                                    english
                                    native
                                }
                                coverImage {
                                    large
                                }
                                episodes
                                format
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val body = mapOf(
            "query" to graphqlQuery,
            "variables" to mapOf(
                "id" to id
            )
        ).toJson().toRequestBody(
            "application/json".toMediaType()
        )

        val response = app.post(
            mainUrl,
            requestBody = body
        ).text

        val result = parseJson<AniListResponse>(response)
        val media = result.data.media ?: return null

        val title =
            media.title.english
                ?: media.title.romaji
                ?: media.title.native
                ?: return null

        val mainEpisodes = mutableListOf<com.lagradost.cloudstream3.Episode>()
        val ovaEpisodes = mutableListOf<com.lagradost.cloudstream3.Episode>()
        val specialEpisodes = mutableListOf<com.lagradost.cloudstream3.Episode>()

        val episodeCount = media.episodes ?: 0

        for (episodeNumber in 1..episodeCount) {
            mainEpisodes.add(
                newEpisode(
                    AnimeEpisodeData(
                        mediaId = media.id,
                        episodeNumber = episodeNumber,
                        category = "main"
                    )
                ) {
                    name = "Bölüm $episodeNumber"
                    season = 1
                    episode = episodeNumber
                }
            )
        }

        val ovaMedia = media.relations?.edges
            ?.mapNotNull { it.node }
            ?.filter { it.format.equals("OVA", ignoreCase = true) }
            ?.distinctBy { it.id }
            ?: emptyList()

        var ovaIndex = 1

        for (ova in ovaMedia) {
            val count = ova.episodes ?: 0

            for (episodeNumber in 1..count) {
                ovaEpisodes.add(
                    newEpisode(
                        AnimeEpisodeData(
                            mediaId = ova.id,
                            episodeNumber = episodeNumber,
                            category = "ova"
                        )
                    ) {
                        name = "OVA $ovaIndex • Bölüm $episodeNumber"
                        season = 2
                        episode = episodeNumber
                    }
                )
            }

            ovaIndex++
        }

        val specialMedia = media.relations?.edges
            ?.mapNotNull { it.node }
            ?.filter { it.format.equals("SPECIAL", ignoreCase = true) }
            ?.distinctBy { it.id }
            ?: emptyList()

        var specialIndex = 1

        for (special in specialMedia) {
            val count = special.episodes ?: 0

            for (episodeNumber in 1..count) {
                specialEpisodes.add(
                    newEpisode(
                        AnimeEpisodeData(
                            mediaId = special.id,
                            episodeNumber = episodeNumber,
                            category = "special"
                        )
                    ) {
                        name = "Special $specialIndex • Bölüm $episodeNumber"
                        season = 3
                        episode = episodeNumber
                    }
                )
            }

            specialIndex++
        }

        return newAnimeLoadResponse(
            title,
            url,
            TvType.Anime,
            comingSoonIfNone = false
        ) {
            posterUrl = media.coverImage?.large
            backgroundPosterUrl = media.bannerImage
            plot = media.description
                ?.replace(Regex("<[^>]*>"), "")
                ?.replace("&quot;", "\"")
                ?.replace("&#39;", "'")
                ?.trim()

            year = media.seasonYear
            tags = media.genres

            seasonNames = listOf(
                com.lagradost.cloudstream3.SeasonData(
                    season = 1,
                    name = "Ana Bölümler",
                    displaySeason = 1
                ),
                com.lagradost.cloudstream3.SeasonData(
                    season = 2,
                    name = "OVA",
                    displaySeason = 2
                ),
                com.lagradost.cloudstream3.SeasonData(
                    season = 3,
                    name = "Special",
                    displaySeason = 3
                )
            )

            addEpisodes(
                com.lagradost.cloudstream3.DubStatus.None,
                mainEpisodes + ovaEpisodes + specialEpisodes
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (com.lagradost.cloudstream3.SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        // Oynatma kaynağını bir sonraki aşamada ekleyeceğiz.
        return false
    }
}