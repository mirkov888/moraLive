package id.mora.live

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

private val android.content.Context.moraDataStore by preferencesDataStore(name = "mora_live_settings")
private val favoritePlatformsKey = stringSetPreferencesKey("favorite_platforms")
private val watchHistoryKey = stringSetPreferencesKey("watch_history")

private data class Platform(
    val name: String,
    val subtitle: String,
    val url: String,
    val region: String,
    val category: String
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
            Platform("IDN Live", "Live streaming Indonesia", "https://www.idn.app/", "INDONESIA", "Variety"),
            Platform("SHOWROOM", "Japanese live streaming", "https://www.showroom-live.com/", "JAPAN", "Idol")
        )
    }
    val favoriteFlow = remember {
        context.moraDataStore.data.map { preferences ->
            preferences[favoritePlatformsKey] ?: emptySet()
        }
    }
    val favorites by favoriteFlow.collectAsState(initial = emptySet())
    val historyFlow = remember {
        context.moraDataStore.data.map { preferences -> preferences[watchHistoryKey] ?: emptySet() }
    }
    val watchHistory by historyFlow.collectAsState(initial = emptySet())
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selected by remember { mutableStateOf("Beranda") }
    var searchQuery by remember { mutableStateOf("") }
    var liveUrlInput by remember { mutableStateOf("https://www.idn.app/") }
    val detailPlatform = platforms.firstOrNull { it.name == selected }
    val activePlatform = platforms.firstOrNull { selected == "web:${it.name}" }

    fun recordWatch(platformName: String) {
        scope.launch {
            context.moraDataStore.edit { preferences ->
                val updated = (preferences[watchHistoryKey] ?: emptySet()).toMutableSet()
                updated.remove(platformName)
                updated.add(platformName)
                preferences[watchHistoryKey] = updated
            }
        }
    }

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
                if (activePlatform == null) TopAppBar(
                    title = {
                        Column {
                            Text("MORA LIVE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("One place. Two live worlds.", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        if (selected != "Beranda") {
                            IconButton(onClick = { selected = if (activePlatform != null) "Search" else "Beranda" }) {
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
                if (activePlatform == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Beranda", "Search", "Favorit", "Account").forEach { item ->
                        NavigationBarItem(
                            selected = selected == item,
                            onClick = { selected = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        "Beranda" -> Icons.Default.LiveTv
                                        "Search" -> Icons.Default.Search
                                        "Favorit" -> Icons.Default.Favorite
                                        else -> Icons.Default.AccountCircle
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
                    initialUrl = liveUrlInput,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    onClose = { selected = activePlatform.name }
                )

                detailPlatform != null -> Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(detailPlatform.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Text(detailPlatform.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(onClick = {}, label = { Text(detailPlatform.region) })
                        SuggestionChip(onClick = {}, label = { Text(detailPlatform.category) })
                    }
                    if (detailPlatform.name == "IDN Live") {
                        OutlinedTextField(value = liveUrlInput, onValueChange = { liveUrlInput = it },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("URL siaran langsung IDN") },
                            placeholder = { Text("https://www.idn.app/nama-streamer/live/...") })
                        Text("Tempel tautan live IDN agar langsung membuka siaran, bukan homepage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    PlatformCard(
                        platform = detailPlatform,
                        isFavorite = detailPlatform.name in favorites,
                        onFavorite = { toggleFavorite(detailPlatform.name) },
                        onOpen = {
                            val candidate = if (detailPlatform.name == "IDN Live") liveUrlInput.trim() else detailPlatform.url
                            val uri = Uri.parse(candidate)
                            val validIdn = uri.scheme == "https" && (uri.host == "idn.app" || (uri.host?.endsWith(".idn.app") == true))
                            if (detailPlatform.name != "IDN Live" || validIdn) {
                                recordWatch(detailPlatform.name)
                                liveUrlInput = candidate
                                selected = "web:${detailPlatform.name}"
                            }
                        },
                        actionLabel = "Tonton siaran"
                    )
                    EmptyState(
                        "Direktori streamer belum tersedia",
                        "Halaman ini sudah native Mora Live. Profil, foto, bio, kategori streamer, dan status LIVE akan ditampilkan setelah sumber data resmi platform tersedia. Tidak ada data streamer contoh yang dibuat seolah-olah live."
                    )
                    OutlinedButton(onClick = { selected = "Search" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Jelajahi platform lain")
                    }
                }

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
                        OutlinedButton(onClick = { selected = "Riwayat" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Buka riwayat tontonan")
                        }
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
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Semua", "Variety", "Idol").forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }
                    val searchablePlatforms = platforms.filter {
                        (selectedCategory == "Semua" || it.category == selectedCategory) &&
                            (it.name.contains(searchQuery, ignoreCase = true) ||
                            it.subtitle.contains(searchQuery, ignoreCase = true))
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
                            actionLabel = "Lihat detail"
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

                selected == "Riwayat" -> LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Riwayat tontonan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Platform yang terakhir kamu buka tersimpan di perangkat ini.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val recent = platforms.filter { it.name in watchHistory }.reversed()
                    if (recent.isEmpty()) {
                        item { EmptyState("Belum ada riwayat", "Buka platform dari Beranda atau Search untuk mencatatnya.") }
                    }
                    items(recent, key = { it.name }) { platform ->
                        PlatformCard(
                            platform = platform,
                            isFavorite = platform.name in favorites,
                            onFavorite = { toggleFavorite(platform.name) },
                            onOpen = { selected = platform.name }
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

                selected == "Account" -> LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Account Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Kelola akses akun live streaming dari satu tempat.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        EmptyState(
                            "Login aman melalui platform resmi",
                            "Tombol di bawah membuka halaman login resmi di browser. Status Connected baru tersedia setelah OAuth resmi dan callback aplikasi dikonfigurasi. Mora Live tidak menyimpan password."
                        )
                    }
                    item {
                        AccountProviderCard("IDN Live", "Login akun IDN. Integrasi OAuth pihak ketiga dan status akun masih menunggu dukungan resmi.", "Web login", "https://www.idn.app/")
                    }
                    item {
                        AccountProviderCard("SHOWROOM", "Login akun SHOWROOM melalui situs resmi. Sinkronisasi sesi ke Mora Live belum tersedia.", "Web login", "https://www.showroom-live.com/")
                    }
                    item {
                        AccountProviderCard("Twitch", "Siap untuk integrasi OAuth setelah Client ID dan redirect URI aplikasi didaftarkan.", "OAuth planned", "https://www.twitch.tv/login")
                    }
                    item {
                        AccountProviderCard("Kick", "Siap untuk integrasi OAuth 2.1 + PKCE setelah aplikasi didaftarkan dan callback dikonfigurasi.", "OAuth planned", "https://kick.com/login")
                    }
                    item {
                        Text(
                            "Membuka halaman login belum berarti akun sudah terhubung ke Mora Live. Chat, gift, dan data akun memerlukan API serta izin resmi masing-masing platform.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
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
                                Text("DISCOVER", color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("Temukan siaran\nfavoritmu.", style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold)
                                Text("Jelajahi platform secara native. Buka situs resmi hanya saat kamu memilih menonton.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).background(androidx.compose.ui.graphics.Color.Red, RoundedCornerShape(50)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("LIVE SEKARANG", fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.weight(1f))
                                    Text("IDN Live", color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                                Text("Jelajahi siaran yang sedang berlangsung dari direktori resmi IDN.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        liveUrlInput = "https://www.idn.app/"
                                        selected = "web:IDN Live"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Jelajahi LIVE IDN") }
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
                            Text("Discover platform", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                            onOpen = { selected = platform.name },
                            actionLabel = "Lihat detail"
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
private fun AccountProviderCard(name: String, detail: String, status: String, loginUrl: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(status, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl)))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Buka login resmi") }
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
    initialUrl: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    var webView by remember(initialUrl) { mutableStateOf<WebView?>(null) }
    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var progress by remember(initialUrl) { mutableIntStateOf(0) }
    var hasError by remember(initialUrl) { mutableStateOf(false) }
    var errorDetail by remember(initialUrl) { mutableStateOf("Koneksi ke server ditolak.") }
    val context = LocalContext.current

    BackHandler(enabled = true) {
        if (fullScreenView != null) {
            fullScreenView = null
            fullScreenCallback?.onCustomViewHidden()
            fullScreenCallback = null
            webView?.visibility = View.VISIBLE
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else onClose()
    }

    Box(modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        if (!hasError) {
            key(initialUrl) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        WebView(viewContext).apply {
                            webView = this
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.userAgentString = settings.userAgentString
                                .replace("; wv", "").replace("Version/4.0 ", "")
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(false)
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    hasError = false
                                    progress = 5
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    progress = 100
                                    // Remove IDN promotional/footer blocks and make the actual HTML5
                                    // player use the available viewport. Cross-origin iframe internals
                                    // remain controlled by IDN and cannot be modified by this page.
                                    val playerCssJs = """
                                        (function() {
                                          try {
                                            document.documentElement.style.background = '#000';
                                            document.body.style.background = '#000';
                                            document.querySelectorAll('video').forEach(function(v) {
                                              v.setAttribute('playsinline','');
                                              v.setAttribute('webkit-playsinline','');
                                              v.controls = true;
                                              v.style.setProperty('display','block','important');
                                              v.style.setProperty('width','100vw','important');
                                              v.style.setProperty('height','100vh','important');
                                              v.style.setProperty('max-width','100vw','important');
                                              v.style.setProperty('max-height','100vh','important');
                                              v.style.setProperty('object-fit','contain','important');
                                              v.style.setProperty('background','#000','important');
                                            });
                                            var words = ['Dapatkan informasi ter-update','App Store','Google Play',
                                              'Tentang Kami','Syarat & Ketentuan','Ketentuan Layanan',
                                              'Community Guideline','Kontak Kami','Artikel'];
                                            document.querySelectorAll('footer, [class*=footer], [id*=footer], a, p, h1, h2, h3, section, div').forEach(function(el) {
                                              var t = (el.innerText || '').trim();
                                              if (t && t.length < 500 && words.some(function(w){return t.toLowerCase().includes(w.toLowerCase());})) {
                                                var target = el.closest('footer') || el.closest('[class*=footer]') || el;
                                                target.style.setProperty('display','none','important');
                                              }
                                            });
                                            var v = document.querySelector('video');
                                            if (v) { v.scrollIntoView({block:'center', inline:'nearest'}); }
                                          } catch(e) {}
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(playerCssJs, null)
                                }
                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    if (request?.isForMainFrame == true) {
                                        errorDetail = error?.description?.toString() ?: "net::ERR_CONNECTION_FAILED"
                                        hasError = true
                                        progress = 100
                                    }
                                }
                                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                    if (request?.isForMainFrame == true) {
                                        val status = errorResponse?.statusCode ?: 0
                                        if (status >= 400) errorDetail = "Server HTTP $status. Halaman live ditolak atau tidak tersedia."
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    if (view != null) {
                                        fullScreenView = view
                                        fullScreenCallback = callback
                                        this@apply.visibility = View.INVISIBLE
                                    }
                                }
                                override fun onHideCustomView() {
                                    fullScreenView = null
                                    fullScreenCallback?.onCustomViewHidden()
                                    fullScreenCallback = null
                                    this@apply.visibility = View.VISIBLE
                                }
                            }
                            loadUrl(initialUrl.ifBlank { platform.url })
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
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Siaran tidak dapat dimuat", color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(errorDetail, color = androidx.compose.ui.graphics.Color.LightGray)
                Button(onClick = { hasError = false; progress = 0; webView?.reload() }) { Text("Coba lagi") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url ?: initialUrl)))
                }) { Text("Buka di browser") }
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
            )
        }

        // Minimal overlay; the stream itself gets the full screen instead of a promo/info panel.
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            color = androidx.compose.ui.graphics.Color(0x99000000),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali",
                        tint = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Text("MORA LIVE • ${platform.name}", color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        fullScreenView?.let { videoView ->
            Dialog(
                onDismissRequest = {
                    fullScreenView = null
                    fullScreenCallback?.onCustomViewHidden()
                    fullScreenCallback = null
                    webView?.visibility = View.VISIBLE
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { viewContext ->
                    FrameLayout(viewContext).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        addView(videoView, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                        ))
                    }
                })
            }
        }
    }
}
