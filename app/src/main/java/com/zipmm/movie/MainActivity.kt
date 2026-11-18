package com.zipmm.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val Background = Color(0xFF070711)
private val SurfaceDark = Color(0xFF111322)
private val SurfaceSoft = Color(0xFF1A1D31)
private val Purple = Color(0xFF9B20FF)
private val PurpleLight = Color(0xFFC05BFF)
private val Muted = Color(0xFF9CA0B7)
private val White = Color(0xFFF8F7FC)

private data class Movie(
    val title: String,
    val subtitle: String,
    val category: String,
    val year: String,
    val rating: String,
    val poster: String,
    val description: String
)

private val movies = listOf(
    Movie("Avengers: Endgame", "The ultimate battle", "Movie", "2019", "8.4", "https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg", "The heroes unite for one final mission."),
    Movie("Spider-Man: No Way Home", "A new world begins", "Movie", "2021", "8.7", "https://image.tmdb.org/t/p/w500/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg", "An unexpected journey changes everything."),
    Movie("Frozen II", "Into the unknown", "Cartoon", "2019", "7.0", "https://image.tmdb.org/t/p/w500/mINJaa34MtknCYl5AjtNJzWj8cD.jpg", "A magical adventure beyond the kingdom."),
    Movie("The Batman", "Unmask the truth", "Movie", "2022", "7.8", "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg", "A dark mystery unfolds in Gotham."),
    Movie("Squid Game", "The game begins", "Korea", "2021", "8.0", "https://image.tmdb.org/t/p/w500/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg", "A survival game with impossible choices."),
    Movie("Alice in Borderland", "Survive the game", "Japan", "2020", "7.7", "https://image.tmdb.org/t/p/w500/20mOwAAPwZ1vLQkw0fvuQHiG7bO.jpg", "A mysterious city becomes a deadly game."),
    Movie("Kingdom", "A kingdom in chaos", "Korea", "2019", "8.3", "https://image.tmdb.org/t/p/w500/3Xw1tA7z5n0J6w7v8Q8x9x9x9x9.jpg", "A historical thriller in a troubled kingdom.")
)

private val heroImage = "https://image.tmdb.org/t/p/original/2u7zbn8EudG6kWIW7m4Q5f6s7sP.jpg"

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
    val saved = remember { mutableStateListOf<String>() }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Background,
        surface = SurfaceDark,
        primary = Purple,
        onBackground = White,
        onSurface = White
    )) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0C0D19),
                    contentColor = Muted,
                    tonalElevation = 0.dp
                ) {
                    val navItems = listOf(
                        Triple("Home", Icons.Default.Home, "Home"),
                        Triple("My List", Icons.Default.BookmarkBorder, "My List"),
                        Triple("Downloads", Icons.Default.Download, "Downloads"),
                        Triple("Category", Icons.Default.GridView, "Category"),
                        Triple("Settings", Icons.Default.Settings, "Settings")
                    )
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item.first,
                            onClick = { tab = item.first; selected = null },
                            icon = { Icon(item.second, contentDescription = item.third) },
                            label = { Text(item.first, fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurpleLight,
                                selectedTextColor = PurpleLight,
                                indicatorColor = Color(0xFF29133D),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            if (selected != null) {
                MovieDetails(selected!!, saved.contains(selected!!.title),
                    onBack = { selected = null },
                    onSave = {
                        if (saved.contains(selected!!.title)) saved.remove(selected!!.title)
                        else saved.add(selected!!.title)
                    })
            } else {
                when (tab) {
                    "Home" -> HomeScreen(
                        padding = padding,
                        category = category,
                        onCategory = { category = it },
                        onMovie = { selected = it },
                        onSearch = { tab = "Search" }
                    )
                    "My List" -> SimpleMovieList("My List", movies.filter { saved.contains(it.title) }, padding) { selected = it }
                    "Downloads" -> EmptyScreen("Downloads", "Your downloaded movies will appear here.", padding)
                    "Category" -> CategoryScreen(padding, onSelect = { category = it; tab = "Home" })
                    "Settings" -> EmptyScreen("Settings", "Account and app settings", padding)
                    "Search" -> SearchScreen(search, { search = it }, padding, onMovie = { selected = it })
                    else -> HomeScreen(padding, category, { category = it }, { selected = it }, { tab = "Search" })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    category: String,
    onCategory: (String) -> Unit,
    onMovie: (Movie) -> Unit,
    onSearch: () -> Unit
) {
    val categories = listOf("All", "Movies", "India", "Korea", "Japan", "Anime", "Cartoon")
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 22.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 13.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(43.dp).clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(PurpleLight, Purple, Color(0xFF4C16B8)))),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(31.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ZIPMM ", fontSize = 22.sp, fontWeight = FontWeight.Black, color = White)
                        Text("MOVIE", fontSize = 22.sp, fontWeight = FontWeight.Black, color = PurpleLight)
                    }
                    Text("Watch  •  Download  •  Enjoy", color = Muted, fontSize = 11.sp)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = White, modifier = Modifier.size(27.dp)) }
                IconButton(onClick = {}) {
                    BadgedBox(badge = { Badge(containerColor = Color.Red) { Text("3") } }) {
                        Icon(Icons.Default.NotificationsNone, "Notifications", tint = White, modifier = Modifier.size(25.dp))
                    }
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = PurpleLight)
                }
                Spacer(Modifier.width(7.dp))
            }
        }
        item {
            HeroBanner(onClick = { onMovie(movies[0]) })
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 17.dp, bottom = 5.dp),
                contentPadding = PaddingValues(horizontal = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shortcuts = listOf(
                    Triple("Movies", Icons.Default.Movie, "Movies"),
                    Triple("Series", Icons.Default.LiveTv, "Series"),
                    Triple("Anime", Icons.Default.AutoAwesome, "Anime"),
                    Triple("Cartoon", Icons.Default.EmojiEmotions, "Cartoon"),
                    Triple("Favorites", Icons.Default.Star, "Favorites"),
                    Triple("VIP", Icons.Default.WorkspacePremium, "VIP")
                )
                items(shortcuts) { item ->
                    Column(
                        modifier = Modifier.width(65.dp).clickable { onCategory(item.first) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(57.dp).clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color(0xFF34206D), Color(0xFF17132C)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(item.second, null, tint = PurpleLight, modifier = Modifier.size(29.dp)) }
                        Spacer(Modifier.height(5.dp))
                        Text(item.first, color = White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(4.dp).height(27.dp).clip(CircleShape).background(Purple))
                Spacer(Modifier.width(10.dp))
                Text("Trending Movies", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White, modifier = Modifier.weight(1f))
                Text("See All  →", color = PurpleLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clickable { onCategory("Movies") })
            }
        }
        item {
            MovieCarousel(movies.take(4), onMovie)
        }
        item {
            SectionTitle("Latest Series", onSeeAll = { onCategory("Series") })
        }
        item {
            MovieCarousel(movies.filter { it.category == "Korea" || it.category == "Japan" }, onMovie)
        }
        item {
            SectionTitle("Indian Movies", onSeeAll = { onCategory("India") })
        }
        item {
            MovieCarousel(movies.filter { it.category == "India" }, onMovie)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun HeroBanner(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(340.dp).clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w780/2u7zbn8EudG6kWIW7m4Q5f6s7sP.jpg",
            contentDescription = "Featured movie",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color(0xF5070711),
                    0.48f to Color(0x99070711),
                    1f to Color.Transparent
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.72f to Color(0x22070711), 1f to Background)
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 19.dp, end = 18.dp, bottom = 27.dp)
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(30.dp)).background(Color(0xFFE91B2D)).padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Trending", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Demon Slayer", color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Kimetsu no Yaiba", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("A boy who fights demons to protect his sister...", color = Color(0xFFE5E3EF), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.width(205.dp))
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = White)
                Spacer(Modifier.width(5.dp))
                Text("Watch Now", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(4) { index ->
                Box(Modifier.width(if (index == 0) 18.dp else 8.dp).height(8.dp).clip(CircleShape).background(if (index == 0) PurpleLight else Color.LightGray.copy(alpha = .65f)))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, onSeeAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(26.dp).clip(CircleShape).background(Purple))
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White, modifier = Modifier.weight(1f))
        Text("See All  →", color = PurpleLight, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onSeeAll))
    }
}

@Composable
private fun MovieCarousel(list: List<Movie>, onMovie: (Movie) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { movie ->
            Column(Modifier.width(137.dp).clickable { onMovie(movie) }) {
                Box(Modifier.fillMaxWidth().height(195.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(
                        model = movie.poster,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xDD080A10)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text("HD", color = White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(movie.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(movie.year, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC928), modifier = Modifier.size(13.dp))
                    Text(movie.rating, color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MovieDetails(movie: Movie, isSaved: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        Box(Modifier.fillMaxWidth().height(390.dp)) {
            AsyncImage(movie.poster, movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Background))))
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color(0x88000000), CircleShape)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = White)
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(movie.title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("${movie.year}  •  ${movie.category}  •  ★ ${movie.rating}", color = Muted, modifier = Modifier.padding(top = 7.dp))
            Text(movie.description, color = Muted, modifier = Modifier.padding(top = 14.dp), lineHeight = 22.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                Icon(Icons.Default.PlayArrow, null); Text("  Play Now")
            }
            OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null)
                Text(if (isSaved) "  Added to My List" else "  Add to My List")
            }
        }
    }
}

@Composable
private fun SearchScreen(query: String, onQuery: (String) -> Unit, padding: PaddingValues, onMovie: (Movie) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(17.dp)) {
        Text("Search", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search movies and series") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        val results = movies.filter { it.title.contains(query, true) || it.category.contains(query, true) }
        LazyColumn { items(results) { movie ->
            Row(Modifier.fillMaxWidth().clickable { onMovie(movie) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(movie.poster, movie.title, contentScale = ContentScale.Crop, modifier = Modifier.size(65.dp, 88.dp).clip(RoundedCornerShape(8.dp)))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(movie.title, color = White, fontWeight = FontWeight.SemiBold)
                    Text("${movie.year} • ${movie.category}", color = Muted, fontSize = 12.sp)
                }
            }
        } }
    }
}

@Composable
private fun SimpleMovieList(title: String, list: List<Movie>, padding: PaddingValues, onMovie: (Movie) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(17.dp)) {
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        if (list.isEmpty()) Text("Your list is empty. Add movies to watch later.", color = Muted, modifier = Modifier.padding(top = 18.dp))
        LazyColumn { items(list) { movie ->
            Row(Modifier.fillMaxWidth().clickable { onMovie(movie) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(movie.poster, movie.title, contentScale = ContentScale.Crop, modifier = Modifier.size(70.dp, 95.dp).clip(RoundedCornerShape(8.dp)))
                Text(movie.title, color = White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
            }
        } }
    }
}

@Composable
private fun CategoryScreen(padding: PaddingValues, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
        Text("Categories", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        listOf("Movies", "Series", "India", "Korea", "Japan", "Anime", "Cartoon").forEach { item ->
            Card(Modifier.fillMaxWidth().padding(top = 10.dp).clickable { onSelect(item) }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, null, tint = PurpleLight)
                    Text(item, color = White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun EmptyScreen(title: String, message: String, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(message, color = Muted, modifier = Modifier.padding(top = 15.dp))
    }
}
