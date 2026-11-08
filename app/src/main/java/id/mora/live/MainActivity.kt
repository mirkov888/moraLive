package id.mora.live

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

private val android.content.Context.moraDataStore by preferencesDataStore(name = "mora_live_settings")
private val favoritePlatformsKey = stringSetPreferencesKey("favorite_platforms")

private data class Platform(
    val name: String,
    val subtitle: String,
    val url: String,
    val region: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoraLiveApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoraLiveApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val platforms = remember {
        listOf(
            Platform("IDN Live", "Live streaming Indonesia", "https://www.idn.app/", "INDONESIA"),
            Platform("SHOWROOM", "Japanese live streaming", "https://www.showroom-live.com/", "JAPAN")
        )
    }
    val favoriteFlow = remember {
        context.moraDataStore.data.map { preferences ->
            preferences[favoritePlatformsKey] ?: emptySet()
        }
    }
    val favorites by favoriteFlow.collectAsState(initial = emptySet())
    var selected by remember { mutableStateOf("Beranda") }
    var searchQuery by remember { mutableStateOf("") }
    val activePlatform = platforms.firstOrNull { it.name == selected }

    fun toggleFavorite(platformName: String) {
        scope.launch {
            context.moraDataStore.edit { preferences ->
                val updated = (preferences[favoritePlatformsKey] ?: emptySet()).toMutableSet()
                if (!updated.add(platformName)) updated.remove(platformName)
                preferences[favoritePlatformsKey] = updated
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFB7A5FF),
            secondary = androidx.compose.ui.graphics.Color(0xFF83D9D0),
            background = androidx.compose.ui.graphics.Color(0xFF0B0D12),
            surface = androidx.compose.ui.graphics.Color(0xFF151923)
        )
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("MORA LIVE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("One place. Two live worlds.", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        if (selected != "Beranda") {
                            IconButton(onClick = { selected = if (activePlatform != null) "Streamer" else "Beranda" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { selected = "Notifikasi" }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Beranda", "Search", "Favorit").forEach { item ->
                        NavigationBarItem(
                            selected = selected == item,
                            onClick = { selected = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        "Beranda" -> Icons.Default.LiveTv
                                        "Search" -> Icons.Default.Search
                                        else -> Icons.Default.Favorite
                                    },
                                    contentDescription = item
                                )
                            },
                            label = { Text(item) }
                        )
                    }
                }
            }
        ) { padding ->
            when {
                activePlatform != null -> PlatformWebView(
                    platform = activePlatform,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    onClose = { selected = "Beranda" }
                )

                selected == "Search" -> LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Cari platform live", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Cari dan pilih platform live. Platform baru bisa ditambahkan di sini tanpa mengubah menu utama.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Cari platform live...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    val searchablePlatforms = platforms.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.subtitle.contains(searchQuery, ignoreCase = true)
                    }
                    if (searchablePlatforms.isEmpty()) {
                        item { EmptyState("Platform tidak ditemukan", "Coba kata kunci lain.") }
                    }
                    items(searchablePlatforms, key = { it.name }) { platform ->
                        PlatformCard(
                            platform = platform,
                            isFavorite = platform.name in favorites,
                            onFavorite = { toggleFavorite(platform.name) },
                            onOpen = { selected = platform.name },
                            actionLabel = "Buka platform"
                        )
                    }
                    item {
                        Text(
                            "Daftar akun dan status live dimuat dari platform resmi. Mora Live tidak menampilkan data streamer palsu.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                selected == "Favorit" -> LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Favorit kamu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Platform yang kamu simpan akan tetap ada setelah aplikasi ditutup.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val saved = platforms.filter { it.name in favorites }
                    if (saved.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Belum ada favorit",
                                message = "Tekan ikon hati pada kartu platform untuk menyimpannya di sini."
                            )
                        }
                    }
                    items(saved, key = { it.name }) { platform ->
                        PlatformCard(
                            platform = platform,
                            isFavorite = true,
                            onFavorite = { toggleFavorite(platform.name) },
                            onOpen = { selected = platform.name }
                        )
                    }
                }

                selected == "Notifikasi" -> Column(
                    Modifier.padding(padding).fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Notifikasi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    EmptyState(
                        title = "Notifikasi live belum aktif",
                        message = "Pemberitahuan saat streamer mulai live memerlukan API, webhook, atau feed resmi dari IDN dan SHOWROOM. Pengingat lokal bisa ditambahkan pada tahap berikutnya."
                    )
                    OutlinedButton(onClick = { selected = "Beranda" }) { Text("Kembali ke beranda") }
                }

                else -> LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("LIVE, YOUR WAY", color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("Temukan siaran\nfavoritmu.", style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold)
                                Text("Akses IDN Live dan SHOWROOM dari satu aplikasi.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Cari platform...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Platform", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${platforms.size} tersedia", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    val filtered = platforms.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.subtitle.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        item { EmptyState("Tidak ditemukan", "Coba kata kunci lain.") }
                    }
                    items(filtered, key = { it.name }) { platform ->
                        PlatformCard(
                            platform = platform,
                            isFavorite = platform.name in favorites,
                            onFavorite = { toggleFavorite(platform.name) },
                            onOpen = { selected = "Streamer" },
                            actionLabel = "Lihat streamer"
                        )
                    }
                    item {
                        Text("Pemutar hybrid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Mora Live membuka situs resmi melalui WebView. Pemutar native akan diaktifkan jika tersedia integrasi stream/API resmi.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(
    platform: Platform,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    actionLabel: String = "Buka platform"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                ) {
                    Icon(Icons.Default.LiveTv, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(platform.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(platform.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus favorit" else "Tambah favorit",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(onClick = {}, label = { Text(platform.region) })
                Spacer(Modifier.weight(1f))
                Button(onClick = onOpen, shape = RoundedCornerShape(14.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PlatformWebView(
    platform: Platform,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    var webView by remember(platform.url) { mutableStateOf<WebView?>(null) }
    var progress by remember(platform.url) { mutableIntStateOf(0) }
    var hasError by remember(platform.url) { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler(enabled = true) {
        val current = webView
        if (current != null && current.canGoBack()) current.goBack() else onClose()
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(platform.name, fontWeight = FontWeight.SemiBold)
                Text("Situs resmi", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { webView?.reload(); hasError = false }) { Text("Muat ulang") }
            TextButton(onClick = onClose) { Text("Tutup") }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        if (hasError) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmptyState(
                    "Situs tidak bisa dibuka di dalam aplikasi",
                    "Platform mungkin membatasi WebView atau memerlukan browser resminya. Coba buka lewat browser eksternal."
                )
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(platform.url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Buka di browser") }
            }
        }
        key(platform.url) {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadsImagesAutomatically = true
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                hasError = false
                                progress = 5
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                progress = 100
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    progress = 100
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true &&
                                    (errorResponse?.statusCode ?: 200) >= 400
                                ) {
                                    hasError = true
                                    progress = 100
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        loadUrl(platform.url)
                    }
                },
                update = { webView = it },
                onRelease = {
                    if (webView === it) webView = null
                    it.stopLoading()
                    it.webChromeClient = null
                    it.webViewClient = WebViewClient()
                    it.destroy()
                }
            )
        }
    }
}
