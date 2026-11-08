package id.mora.live

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Platform(val name: String, val subtitle: String, val url: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoraLiveApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoraLiveApp() {
    val platforms = listOf(
        Platform("IDN Live", "Live streaming Indonesia", "https://www.idn.app/"),
        Platform("SHOWROOM", "Japanese live streaming", "https://www.showroom-live.com/")
    )
    var selected by remember { mutableStateOf("Beranda") }
    val favorites = remember { mutableStateListOf<String>() }

    val activePlatform = platforms.firstOrNull { it.name == selected }\n\n    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mora Live") },
                    actions = { IconButton(onClick = { selected = "Notifikasi" }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
                    } }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("Beranda", "Favorit").forEach { item ->
                        NavigationBarItem(
                            selected = selected == item,
                            onClick = { selected = item },
                            icon = { Icon(if (item == "Beranda") Icons.Default.LiveTv else Icons.Default.Favorite, null) },
                            label = { Text(item) }
                        )
                    }
                }
            }
        ) { padding ->
            when {
                activePlatform != null -> Column(Modifier.padding(padding).fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(activePlatform.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { selected = "Beranda" }) { Text("Tutup") }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                loadUrl(activePlatform.url)
                            }
                        },
                        update = { it.loadUrl(activePlatform.url) }
                    )
                }
                selected == "Favorit" -> LazyColumn(Modifier.padding(padding).padding(16.dp)) {
                    item { Text("Streamer favorit", style = MaterialTheme.typography.headlineSmall) }
                    item { Spacer(Modifier.height(12.dp)) }
                    if (favorites.isEmpty()) item { Text("Belum ada favorit. Tambahkan dari beranda.") }
                    items(favorites) { Text(it, Modifier.padding(vertical = 8.dp)) }
                }
                selected == "Notifikasi" -> Column(Modifier.padding(padding).padding(20.dp)) {
                    Text("Notifikasi", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Pusat notifikasi siap dikembangkan. Status live otomatis memerlukan API resmi platform.")
                    Button(onClick = { selected = "Beranda" }) { Text("Kembali") }
                }
                else -> LazyColumn(Modifier.padding(padding).padding(16.dp)) {
                    item {
                        Text("Temukan live streaming", style = MaterialTheme.typography.headlineSmall)
                        Text("Pilih platform untuk mulai menonton.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                    }
                    items(platforms) { platform ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(platform.name, style = MaterialTheme.typography.titleLarge)
                                    Text(platform.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    if (platform.name in favorites) favorites.remove(platform.name)
                                    else favorites.add(platform.name)
                                }) {
                                    Icon(
                                        if (platform.name in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorit"
                                    )
                                }
                                Button(onClick = { selected = platform.name }) { Text("Buka") }
                            }
                        }
                    }
                    item {
                        Text("Hybrid player", style = MaterialTheme.typography.titleMedium)
                        Text("Fondasi UI tersedia. Integrasi WebView/native playback akan diaktifkan setelah alur resmi dan URL streaming yang didukung tiap platform diverifikasi.")
                    }
                }
            }
        }
    }
}
