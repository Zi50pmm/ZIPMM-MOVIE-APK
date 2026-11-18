package com.zipmm.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF080A10)
private val Panel = Color(0xFF171B27)
private val Gold = Color(0xFFFFC857)
private val Muted = Color(0xFF9BA3B4)
private data class Movie(val title: String, val category: String, val description: String)
private val titles = listOf(
    Movie("Featured Movies", "Movie", "Movie catalogue will appear after admin uploads licensed titles."),
    Movie("India Spotlight", "India", "Indian movie collection."),
    Movie("Korea Spotlight", "Korea", "Korean movie collection."),
    Movie("Japan Spotlight", "Japan", "Japanese movie collection."),
    Movie("Korean Drama", "Korean Drama", "Drama seasons and episodes will appear after admin upload.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZipmmApp() }
    }
}

@Composable
private fun ZipmmApp() {
    var tab by remember { mutableStateOf("Home") }
    var category by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Movie?>(null) }
    var playing by remember { mutableStateOf(false) }
    val saved = remember { mutableStateListOf<String>() }
    val history = remember { mutableStateListOf<String>() }
    val cats = listOf("All", "Movie", "India", "Korea", "Japan", "Korean Drama")
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Gold)) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Search", "Watchlist", "Account").forEach { item ->
                    NavigationBarItem(tab == item, { tab = item; selected = null; playing = false },
                        icon = { Icon(when(item) { "Home" -> Icons.Default.Home; "Search" -> Icons.Default.Search; "Watchlist" -> Icons.Default.Bookmark; else -> Icons.Default.Person }, item) },
                        label = { Text(item) })
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ZIPMM", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("MOVIE  •  YOUR MOVIE WORLD", color = Muted, fontSize = 10.sp)
                    }
                    IconButton(onClick = { tab = "Search" }) { Icon(Icons.Default.Search, "Search") }
                }
                Spacer(Modifier.height(16.dp))
                if (selected != null) {
                    if (playing) {
                        TextButton(onClick = { playing = false }) { Icon(Icons.Default.ArrowBack, null); Text("Back") }
                        Box(Modifier.fillMaxWidth().height(230.dp).background(Color.Black, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayCircle, null, tint = Gold, modifier = Modifier.size(58.dp))
                                Text("Video source not connected", color = Color.White)
                                Text("Admin must upload a licensed video URL.", color = Muted)
                            }
                        }
                        Text(selected!!.title, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    } else {
                        TextButton(onClick = { selected = null }) { Icon(Icons.Default.ArrowBack, null); Text("Back") }
                        Box(Modifier.fillMaxWidth().height(180.dp).background(Panel, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Gold, modifier = Modifier.size(56.dp)) }
                        Spacer(Modifier.height(12.dp))
                        Text(selected!!.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(selected!!.category, color = Muted)
                        Text(selected!!.description, color = Muted)
                        Button(onClick = { history.add(selected!!.title); playing = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Icon(Icons.Default.PlayArrow, null, tint = Ink); Text(" Play", color = Ink) }
                        OutlinedButton(onClick = { if (saved.contains(selected!!.title)) saved.remove(selected!!.title) else saved.add(selected!!.title) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Bookmark, null); Text(if (saved.contains(selected!!.title)) " Saved" else " Add to Watchlist") }
                    }
                } else when(tab) {
                    "Search" -> {
                        Text("Search Movies", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search movie or category") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                        titles.filter { it.title.contains(search, true) || it.category.contains(search, true) }.forEach { MovieRow(it) { selected = it } }
                    }
                    "Watchlist" -> {
                        Text("My Watchlist", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        if (saved.isEmpty()) Text("Saved movies will appear here.", color = Muted)
                        saved.forEach { name -> MovieRow(Movie(name, "Saved", "Saved title")) { selected = titles.firstOrNull { it.title == name } } }
                    }
                    "Account" -> {
                        Text("User Account", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Sign-in and cloud sync require a connected backend.", color = Muted)
                        Spacer(Modifier.height(12.dp))
                        Text("KBZPay", color = Gold, fontWeight = FontWeight.Bold)
                        Text("09456707261")
                        Text("WavePay", color = Gold, fontWeight = FontWeight.Bold)
                        Text("09456707261")
                        Text("Transfer receipt review and premium activation are handled by admin. Secure receipt upload needs backend setup.", color = Muted)
                    }
                    else -> {
                        Text("Watch something great", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(cats) { c -> FilterChip(category == c, { category = c }, label = { Text(c) }) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(170.dp).background(Panel, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Movie, null, tint = Gold, modifier = Modifier.size(38.dp))
                                Text("ZIPMM MOVIE", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                Text("Your next favourite starts here", color = Muted)
                            }
                        }
                        if (history.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp)); Text("Continue Watching", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            LazyRow { items(history.distinct()) { name -> MovieRow(Movie(name, "Continue Watching", "Resume")) { selected = titles.firstOrNull { it.title == name } } } }
                        }
                        val sections = if (category == "All") cats.drop(1) else listOf(category)
                        sections.forEach { c ->
                            Spacer(Modifier.height(18.dp))
                            Text(if (c == "Korean Drama") "Korean Drama Series" else c + " Collection", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(titles.filter { it.category == c }) { m ->
                                    Column(Modifier.width(138.dp).clickable { selected = m }) {
                                        Box(Modifier.fillMaxWidth().height(150.dp).background(Panel, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Gold, modifier = Modifier.size(34.dp)) }
                                        Text(m.title, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("PREMIUM PLANS", color = Gold, fontWeight = FontWeight.Bold)
                        Text("Subscription prices and durations will be set by admin.", color = Muted)
                        Text("Payment accounts: KBZPay / WavePay 09456707261", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieRow(movie: Movie, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Movie, null, tint = Gold)
            Spacer(Modifier.width(12.dp))
            Column { Text(movie.title, fontWeight = FontWeight.SemiBold); Text(movie.category, color = Muted, fontSize = 12.sp) }
            Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
