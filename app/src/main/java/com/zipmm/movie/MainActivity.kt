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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B27)
private val Gold = Color(0xFFFFC857)
private val Muted = Color(0xFF9BA3B4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZipmmApp() }
    }
}

@Composable
fun ZipmmApp() {
    var tab by remember { mutableStateOf("Home") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showPlans by remember { mutableStateOf(false) }
    val categories = listOf("All", "Movies", "India", "Japan", "Korea", "Korean Drama")
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Gold,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Drama", "Plans", "Account").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = {
                        tab = item
                        if (item == "Plans") showPlans = true
                    }, icon = {}, label = { Text(item) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp)) {
                Text("ZIPMM", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("MOVIE  •  YOUR MOVIE WORLD", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(22.dp))
                if (showPlans || tab == "Plans") {
                    PlansScreen(onBack = { showPlans = false; tab = "Home" })
                } else if (tab == "Account") {
                    Text("My Account", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Sign-in and account services will be connected to the secure backend in the next setup stage.", color = Muted)
                    Button(onClick = { showPlans = true }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("View Premium Plans", color = Ink) }
                } else {
                    Text(if (tab == "Drama") "Korean Drama Series" else "Watch something great", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("FEATURED", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(180.dp).background(Panel), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ZIPMM MOVIE", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black)
                            Text("Your next favourite starts here", color = Muted)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(when(selectedCategory) { "All" -> "Popular Movies"; "Korean Drama" -> "Korean Drama"; else -> "$selectedCategory Movies" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Your movie catalogue will appear here after the owner connects the content database and uploads licensed movie files.", color = Muted)
                    Spacer(Modifier.height(20.dp))
                    Text("PREMIUM", color = Gold, fontWeight = FontWeight.Bold)
                    Text("Unlock the full ZIPMM library with a subscription.", color = Muted)
                    Button(onClick = { showPlans = true }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("See subscription plans", color = Ink) }
                }
            }
        }
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit) {
    Column {
        Text("Premium Plans", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose a duration. Prices and plan lengths will be set by the administrator.", color = Muted)
        Spacer(Modifier.height(16.dp))
        listOf("Daily Pass", "7-Day Pass", "Monthly Pass").forEach { plan ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Text(plan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Price configured by admin", color = Muted)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { /* Payment request flow requires server configuration */ }) {
                        Text("KBZPay / WavePay")
                    }
                }
            }
        }
        Text("Payment workflow: transfer to the owner's verified KBZPay or WavePay account, upload the transfer receipt, then wait for admin approval. Secure receipt upload and approval require the backend to be configured.", color = Muted)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
