dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

version = 2

cloudstream {
    description = "TurkAnime anime arama ve katalog eklentisi"
    authors = listOf("Lynx3272")
    status = 1
    tvTypes = listOf("Anime", "AnimeMovie")
    requiresResources = true
    language = "tr"
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
