package com.zipmm.movie

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

private val Bg=Color(0xFF070711)
private val Panel=Color(0xFF151729)
private val Purple=Color(0xFF9B20FF)
private val Purple2=Color(0xFFC05BFF)
private val Muted=Color(0xFF9CA0B7)
private val Txt=Color(0xFFF8F7FC)

private data class Movie(val id:String,val title:String,val category:String,val year:String,val rating:String,val poster:String,val description:String,val videoUrl:String?=null)

private val catalog=listOf(
 Movie("avengers","Avengers: Endgame","Movies","2019","8.4","https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg","The heroes unite for one final mission."),
 Movie("spiderman","Spider-Man: No Way Home","Movies","2021","8.7","https://image.tmdb.org/t/p/w500/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg","An unexpected journey changes everything."),
 Movie("frozen2","Frozen II","Cartoon","2019","7.0","https://image.tmdb.org/t/p/w500/mINJaa34MtknCYl5AjtNJzWj8cD.jpg","A magical adventure beyond the kingdom."),
 Movie("batman","The Batman","Movies","2022","7.8","https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg","A dark mystery unfolds in Gotham."),
 Movie("rrr","RRR","India","2022","7.8","https://image.tmdb.org/t/p/w500/nEcjiK8s8A4J5D7d9Q9z6z9z9z9.jpg","An Indian action drama."),
 Movie("kgf2","KGF: Chapter 2","India","2022","8.2","https://image.tmdb.org/t/p/w500/khNVygolU0TxLIDWff5tQlAhZ23.jpg","A powerful Indian action story."),
 Movie("jawan","Jawan","India","2023","7.0","https://image.tmdb.org/t/p/w500/jFt1gS4BG5xKrtj7a3z9otvXf1J.jpg","An action thriller from India."),
 Movie("dangal","Dangal","India","2016","8.3","https://image.tmdb.org/t/p/w500/whvGX6g3W4J5k8v8e9v8e9v8e9v.jpg","A sports drama from India."),
 Movie("squid","Squid Game","Korea","2021","8.0","https://image.tmdb.org/t/p/w500/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg","A survival game with impossible choices."),
 Movie("kingdom","Kingdom","Korea","2019","8.3","https://image.tmdb.org/t/p/w500/3Xw1tA7z5n0J6w7v8Q8x9x9x9x9.jpg","A historical thriller."),
 Movie("alice","Alice in Borderland","Japan","2020","7.7","https://image.tmdb.org/t/p/w500/20mOwAAPwZ1vLQkw0fvuQHiG7bO.jpg","A mysterious city becomes a deadly game."),
 Movie("demon","Demon Slayer","Anime","2019","8.6","https://image.tmdb.org/t/p/w500/xUfRaju2zB2q7s0k6N8G5Q7w5d.jpg","A boy fights demons to protect his sister."),
 Movie("onepiece","One Piece","Anime","1999","8.7","https://image.tmdb.org/t/p/w500/e3NBGiAifW9Xt8xD5tpARskjccO.jpg","A crew sets sail for the ultimate treasure."),
 Movie("kungfu","Kung Fu Panda","Cartoon","2008","7.6","https://image.tmdb.org/t/p/w500/wWt4JYXTg5Wr3xBW2phBrMKgp3x.jpg","A panda discovers his destiny.")
)
private val allCategories=listOf("Movies","India","Korea","Japan","China","Anime","Cartoon","Series")

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{ZipmmApp()}}
}

@Composable private fun ZipmmApp(){
 val context= LocalContext.current
 val prefs=remember{context.getSharedPreferences("zipmm",Context.MODE_PRIVATE)}
 val scope= rememberCoroutineScope()
 var page by remember{mutableStateOf("Home")}
 var category by remember{mutableStateOf("Movies")}
 var query by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf<Movie?>(null)}
 var playing by remember{mutableStateOf<String?>(null)}
 var email by remember{mutableStateOf(prefs.getString("email","")?:"")}
 var token by remember{mutableStateOf(prefs.getString("token","")?:"")}
 var saved by remember{mutableStateOf(prefs.getStringSet("saved",emptySet())!!.toSet())}
 var dark by remember{mutableStateOf(prefs.getBoolean("dark",true))}
 var wifi by remember{mutableStateOf(prefs.getBoolean("wifi",false))}
 var busy by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("")}
 fun saveMovie(m:Movie){saved=if(m.id in saved)saved-m.id else saved+m.id;prefs.edit().putStringSet("saved",saved).apply()}
 fun toast(s:String)=Toast.makeText(context,s,Toast.LENGTH_LONG).show()
 MaterialTheme(colorScheme=darkColorScheme(primary=Purple,background=Bg,surface=Panel,onBackground=Txt,onSurface=Txt)){
  if(playing!=null) PlayerScreen(playing!!,{playing=null})
  else if(selected!=null) DetailScreen(selected!!,selected!!.id in saved,{selected=null},{saveMovie(selected!!)},{
   val u=selected!!.videoUrl
   if(u.isNullOrBlank())toast("ဒီ Movie အတွက် Admin က Video URL မထည့်ရသေးပါ။") else playing=u
  },{
   val m=selected!!
   if(m.videoUrl.isNullOrBlank())toast("Download လုပ်ရန် Video URL မရှိသေးပါ။ Admin က Video URL ထည့်ပေးရန်လိုပါတယ်။")
   else downloadMovie(context,prefs,m,::toast)
  })
  else Scaffold(containerColor=Bg,bottomBar={
   NavigationBar(containerColor=Color(0xFF0C0D19),tonalElevation=0.dp){
    listOf("Home","Search","My List","Downloads","Account").forEach{p->
     val ico=when(p){"Home"->Icons.Default.Home;"Search"->Icons.Default.Search;"My List"->Icons.Default.BookmarkBorder;"Downloads"->Icons.Default.Download;else->Icons.Default.Person}
     NavigationBarItem(selected=page==p,onClick={page=p},icon={Icon(ico,null)},label={Text(p,fontSize=10.sp)},
      colors=NavigationBarItemDefaults.colors(selectedIconColor=Purple2,selectedTextColor=Purple2,indicatorColor=Color(0xFF29133D),unselectedIconColor=Muted,unselectedTextColor=Muted))
    }
   }
  }){pad->
   when(page){
    "Home"->HomePage(pad,{page="Search"},{category=it;page="Category"},{selected=it})
    "Search"->SearchPage(pad,query,{query=it},{selected=it})
    "Category"->CategoryPage(pad,category,{category=it},{selected=it})
    "My List"->ListPage(pad,"My List",catalog.filter{it.id in saved},{selected=it})
    "Downloads"->DownloadsPage(pad,prefs,{playing=it},{id->prefs.edit().remove("download_"+id).remove("download_title_"+id).apply()})
    "Account"->AccountPage(pad,email,token,busy,message,{mail,pass,signup->
     scope.launch{busy=true;message="";val res=withContext(Dispatchers.IO){authRequest(mail,pass,signup)};busy=false
      if(res.first!=null){token=res.first!!;email=mail;prefs.edit().putString("token",token).putString("email",email).apply();message="အကောင့်ဝင်ပြီးပါပြီ။"}
      else message=res.second}
    },{token="";email="";prefs.edit().remove("token").remove("email").apply();message="အကောင့်မှ ထွက်ပြီးပါပြီ။"})
    "Settings"->SettingsPage(pad,dark,wifi,{dark=it;prefs.edit().putBoolean("dark",it).apply()},{wifi=it;prefs.edit().putBoolean("wifi",it).apply()},{page="Account"})
    else->HomePage(pad,{page="Search"},{category=it;page="Category"},{selected=it})
   }
  }
 }
}

@Composable private fun HomePage(p:PaddingValues,onSearch:()->Unit,onCategory:(String)->Unit,onMovie:(Movie)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(bottom=18.dp)){
  item{Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Purple2,Purple))),contentAlignment=Alignment.Center){Icon(Icons.Default.PlayArrow,null,tint=Txt)}
   Column(Modifier.weight(1f).padding(start=10.dp)){Text("ZIPMM MOVIE",fontSize=21.sp,fontWeight=FontWeight.Black,color=Txt);Text("Watch • Download • Enjoy",fontSize=11.sp,color=Muted)}
   IconButton(onClick=onSearch){Icon(Icons.Default.Search,null,tint=Txt)}
   IconButton(onClick={onCategory("Settings")}){Icon(Icons.Default.Settings,null,tint=Txt)}
  }}
  item{Hero{onMovie(catalog.first())}}
  item{LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.padding(top=15.dp)){
   items(listOf("Movies","Series","Anime","Cartoon","Favorites","VIP")){name->
    Column(Modifier.width(66.dp).clickable{onCategory(if(name=="Favorites")"My List" else name)},horizontalAlignment=Alignment.CenterHorizontally){
     Box(Modifier.size(55.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFF39206B),Panel))),contentAlignment=Alignment.Center){
      Icon(when(name){"Movies"->Icons.Default.Movie;"Series"->Icons.Default.LiveTv;"Anime"->Icons.Default.AutoAwesome;"Cartoon"->Icons.Default.EmojiEmotions;"Favorites"->Icons.Default.Star;else->Icons.Default.WorkspacePremium},null,tint=Purple2)
     };Text(name,color=Txt,fontSize=10.sp,maxLines=1)
    }
   }
  }}
  item{Section("Trending Movies"){onCategory("Trending Movies")}}
  item{Rail(catalog.take(4),onMovie)}
  item{Section("Indian Movies"){onCategory("India")}}
  item{Rail(catalog.filter{it.category=="India"},onMovie)}
  item{Section("Korean Movies"){onCategory("Korea")}}
  item{Rail(catalog.filter{it.category=="Korea"},onMovie)}
  item{Section("Japanese Movies"){onCategory("Japan")}}
  item{Rail(catalog.filter{it.category=="Japan"},onMovie)}
  item{Section("Chinese Movies"){onCategory("China")}}
  item{EmptyHint("Chinese Movies များကို Admin က ထည့်သွင်းသည့်အခါ ဒီနေရာတွင် ပေါ်ပါမည်။")}
  item{Section("Anime"){onCategory("Anime")}}
  item{Rail(catalog.filter{it.category=="Anime"},onMovie)}
  item{Section("Cartoon"){onCategory("Cartoon")}}
  item{Rail(catalog.filter{it.category=="Cartoon"},onMovie)}
  item{Section("Latest Series"){onCategory("Series")}}
  item{Rail(catalog.filter{it.category=="Korea"||it.category=="Japan"},onMovie)}
 }
}
@Composable private fun Hero(onClick:()->Unit){
 Box(Modifier.fillMaxWidth().height(300.dp).clickable(onClick=onClick)){
  AsyncImage("https://image.tmdb.org/t/p/w780/xUfRaju2zB2q7s0k6N8G5Q7w5d.jpg",null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
  Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg.copy(alpha=.96f),Bg.copy(alpha=.35f),Color.Transparent))))
  Column(Modifier.align(Alignment.BottomStart).padding(20.dp)){
   Text("🔥 TRENDING",color=Txt,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Red).padding(horizontal=12.dp,vertical=5.dp))
   Text("Demon Slayer",color=Txt,fontSize=29.sp,fontWeight=FontWeight.Black);Text("Kimetsu no Yaiba",color=Txt,fontSize=17.sp)
   Text("A boy who fights demons to protect his sister...",color=Txt,fontSize=12.sp,modifier=Modifier.width(220.dp))
   Button(onClick=onClick,colors=ButtonDefaults.buttonColors(containerColor=Purple),shape=RoundedCornerShape(30.dp)){Icon(Icons.Default.PlayArrow,null);Text(" Watch Now")}
  }
 }
}
@Composable private fun Section(title:String,onSeeAll:()->Unit){
 Row(Modifier.fillMaxWidth().padding(start=17.dp,end=17.dp,top=22.dp,bottom=11.dp),verticalAlignment=Alignment.CenterVertically){
  Box(Modifier.width(4.dp).height(26.dp).clip(CircleShape).background(Purple))
  Text(title,color=Txt,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f).padding(start=10.dp))
  Text("See All →",color=Purple2,fontWeight=FontWeight.Bold,modifier=Modifier.clickable(onClick=onSeeAll))
 }
}
@Composable private fun Rail(list:List<Movie>,onMovie:(Movie)->Unit){
 if(list.isEmpty()){EmptyHint("ဒီ Category မှာ Movie မရှိသေးပါ။");return}
 LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){items(list){Poster(it,onMovie)}}
}
@Composable private fun Poster(m:Movie,onMovie:(Movie)->Unit){
 Column(Modifier.width(132.dp).clickable{onMovie(m)}){
  AsyncImage(m.poster,m.title,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(11.dp)))
  Text(m.title,color=Txt,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis)
  Text(m.year+"  ★ "+m.rating,color=Muted,fontSize=11.sp)
 }
}
@Composable private fun EmptyHint(s:String){Text(s,color=Muted,fontSize=12.sp,modifier=Modifier.padding(horizontal=18.dp,vertical=10.dp))}

@Composable private fun CategoryPage(p:PaddingValues,category:String,onCategory:(String)->Unit,onMovie:(Movie)->Unit){
 val list=when(category){"Trending Movies"->catalog.take(4);"Movies"->catalog.filter{it.category=="Movies"};"Series"->catalog.filter{it.category=="Korea"||it.category=="Japan"};else->catalog.filter{it.category.equals(category,true)}}
 Column(Modifier.fillMaxSize().padding(p)){
  Text(category,color=Txt,fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(16.dp))
  LazyRow(contentPadding=PaddingValues(horizontal=10.dp)){items(allCategories){c->FilterChip(selected=category==c,onClick={onCategory(c)},label={Text(c)},modifier=Modifier.padding(horizontal=3.dp))}}
  if(list.isEmpty())EmptyHint("$category ထဲမှာ Movie မရှိသေးပါ။ Admin က သက်ဆိုင်ရာ Category ထဲ Movie ထည့်ပေးရပါမယ်။")
  LazyColumn(contentPadding=PaddingValues(14.dp)){items(list.chunked(2)){row->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{Poster(it,onMovie)};if(row.size==1)Spacer(Modifier.width(132.dp))}
   Spacer(Modifier.height(14.dp))
  }}
 }
}
@Composable private fun SearchPage(p:PaddingValues,q:String,onQ:(String)->Unit,onMovie:(Movie)->Unit){
 Column(Modifier.fillMaxSize().padding(p).padding(16.dp)){Text("Search Movies",color=Txt,fontSize=24.sp,fontWeight=FontWeight.Bold)
  OutlinedTextField(q,onQ,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Movie, country or category")},leadingIcon={Icon(Icons.Default.Search,null)})
  LazyColumn{items(catalog.filter{it.title.contains(q,true)||it.category.contains(q,true)}){MovieRow(it,onMovie)}}
 }
}
@Composable private fun ListPage(p:PaddingValues,title:String,list:List<Movie>,onMovie:(Movie)->Unit){
 Column(Modifier.fillMaxSize().padding(p).padding(16.dp)){Text(title,color=Txt,fontSize=24.sp,fontWeight=FontWeight.Bold);if(list.isEmpty())EmptyHint("သိမ်းထားသော Movie မရှိသေးပါ။");LazyColumn{items(list){MovieRow(it,onMovie)}}}
}
@Composable private fun MovieRow(m:Movie,onMovie:(Movie)->Unit){
 Row(Modifier.fillMaxWidth().clickable{onMovie(m)}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
  AsyncImage(m.poster,m.title,contentScale=ContentScale.Crop,modifier=Modifier.size(72.dp,94.dp).clip(RoundedCornerShape(8.dp)))
  Column(Modifier.padding(start=12.dp)){Text(m.title,color=Txt,fontWeight=FontWeight.Bold);Text(m.year+" • "+m.category+" • ★ "+m.rating,color=Muted,fontSize=12.sp);Text(m.description,color=Muted,fontSize=11.sp,maxLines=2)}
 }
}
@Composable private fun DetailScreen(m:Movie,saved:Boolean,back:()->Unit,save:()->Unit,play:()->Unit,download:()->Unit){
 Column(Modifier.fillMaxSize().background(Bg)){
  Box(Modifier.fillMaxWidth().height(350.dp)){AsyncImage(m.poster,m.title,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize());Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Bg))));IconButton(onClick=back,modifier=Modifier.align(Alignment.TopStart).padding(12.dp).background(Color(0x99000000),CircleShape)){Icon(Icons.Default.ArrowBack,null,tint=Txt)}}
  Column(Modifier.padding(18.dp)){
   Text(m.title,color=Txt,fontSize=25.sp,fontWeight=FontWeight.Bold);Text(m.year+" • "+m.category+" • ★ "+m.rating,color=Muted,modifier=Modifier.padding(top=7.dp));Text(m.description,color=Txt,modifier=Modifier.padding(top=12.dp))
   Button(onClick=play,modifier=Modifier.fillMaxWidth().padding(top=18.dp),colors=ButtonDefaults.buttonColors(containerColor=Purple)){Icon(Icons.Default.PlayArrow,null);Text(" Play Now")}
   OutlinedButton(onClick=download,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Download,null);Text(" Download for Offline")}
   OutlinedButton(onClick=save,modifier=Modifier.fillMaxWidth()){Icon(if(saved)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,null);Text(if(saved)" Saved to My List" else " Add to My List")}
  }
 }
}
@Composable private fun PlayerScreen(url:String,back:()->Unit){
 val ctx=LocalContext.current
 val player= remember(url){ExoPlayer.Builder(ctx).build().apply{setMediaItem(MediaItem.fromUri(url));prepare();playWhenReady=true}}
 DisposableEffect(player){onDispose{player.release()}}
 Column(Modifier.fillMaxSize().background(Color.Black)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null,tint=Txt)};Text("ZIPMM MOVIE Player",color=Txt)}
  AndroidView(factory={ctx->PlayerView(ctx).apply{this.player=player;useController=true}},modifier=Modifier.fillMaxWidth().weight(1f))
 }
}
private fun downloadMovie(ctx:Context,prefs:android.content.SharedPreferences,m:Movie,toast:(String)->Unit){
 try{
  val name=m.id+".mp4"
  val req=DownloadManager.Request(Uri.parse(m.videoUrl)).setTitle(m.title).setDescription("ZIPMM MOVIE offline download")
   .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
   .setDestinationInExternalFilesDir(ctx,Environment.DIRECTORY_MOVIES,name).setAllowedOverMetered(!prefs.getBoolean("wifi",false)).setAllowedOverRoaming(false)
  val id=(ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
  val path=File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES),name).absolutePath
  prefs.edit().putString("download_"+m.id,path).putString("download_title_"+m.id,m.title).putLong("download_id_"+m.id,id).apply()
  toast("Download စတင်ပါပြီ။ ပြီးသွားလျှင် Downloads မှ Offline ကြည့်နိုင်ပါတယ်။")
 }catch(e:Exception){toast("Download မအောင်မြင်ပါ: "+e.localizedMessage)}
}
@Composable private fun DownloadsPage(p:PaddingValues,prefs:android.content.SharedPreferences,play:(String)->Unit,remove:(String)->Unit){
 val keys=remember{prefs.all.keys.filter{it.startsWith("download_")&&!it.startsWith("download_id_")&&!it.startsWith("download_title_")}}
 Column(Modifier.fillMaxSize().padding(p).padding(16.dp)){Text("Offline Downloads",color=Txt,fontSize=24.sp,fontWeight=FontWeight.Bold)
  if(keys.isEmpty())EmptyHint("Download လုပ်ထားသော Movie မရှိသေးပါ။")
  LazyColumn{items(keys){key->
   val id=key.removePrefix("download_");val path=prefs.getString(key,"")?:"";val title=prefs.getString("download_title_"+id,id)?:id;val file=File(path)
   Card(Modifier.fillMaxWidth().padding(vertical=6.dp),colors=CardDefaults.cardColors(containerColor=Panel)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Default.Movie,null,tint=Purple2);Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(title,color=Txt,fontWeight=FontWeight.Bold);Text(if(file.exists())"Ready for offline playback" else "Download in progress / not ready",color=Muted,fontSize=11.sp)}
    IconButton(onClick={if(file.exists())play(Uri.fromFile(file).toString())}){Icon(Icons.Default.PlayArrow,null,tint=Purple2)}
    IconButton(onClick={remove(id)}){Icon(Icons.Default.Delete,null,tint=Muted)}
   }}
  }}
 }
}
@Composable private fun AccountPage(p:PaddingValues,email:String,token:String,busy:Boolean,message:String,login:(String,String,Boolean)->Unit,logout:()->Unit){
 var mail by remember(email){mutableStateOf(email)};var pass by remember{mutableStateOf("")};var signup by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().padding(p).padding(20.dp)){Text("Account",color=Txt,fontSize=25.sp,fontWeight=FontWeight.Bold)
  if(token.isNotBlank()){Icon(Icons.Default.AccountCircle,null,tint=Purple2,modifier=Modifier.size(65.dp));Text(email,color=Txt,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("ZIPMM Member",color=Muted);Button(onClick=logout,modifier=Modifier.fillMaxWidth().padding(top=20.dp)){Text("Log Out")}}
  else{Text(if(signup)"Create ZIPMM Account" else "Sign In",color=Txt,fontSize=19.sp,modifier=Modifier.padding(top=16.dp))
   OutlinedTextField(mail,{mail=it},modifier=Modifier.fillMaxWidth(),label={Text("Email")},singleLine=true)
   OutlinedTextField(pass,{pass=it},modifier=Modifier.fillMaxWidth().padding(top=10.dp),label={Text("Password")},singleLine=true)
   Button(onClick={if(mail.isNotBlank()&&pass.length>=6)login(mail,pass,signup)},enabled=!busy,modifier=Modifier.fillMaxWidth().padding(top=16.dp),colors=ButtonDefaults.buttonColors(containerColor=Purple)){Text(if(busy)"Please wait..." else if(signup)"Create Account" else "Sign In")}
   TextButton(onClick={signup=!signup}){Text(if(signup)"Already registered? Sign In" else "Create a new account")}
   if(message.isNotBlank())Text(message,color=Purple2,modifier=Modifier.padding(top=10.dp))
   Text("အကောင့်စနစ်အတွက် Internet နှင့် Supabase Auth ဖွင့်ထားရန်လိုပါတယ်။",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=14.dp))
  }
 }
}
@Composable private fun SettingsPage(p:PaddingValues,dark:Boolean,wifi:Boolean,onDark:(Boolean)->Unit,onWifi:(Boolean)->Unit,account:()->Unit){
 Column(Modifier.fillMaxSize().padding(p).padding(18.dp)){Text("Settings",color=Txt,fontSize=25.sp,fontWeight=FontWeight.Bold)
  Setting("Dark Theme","ZIPMM dark appearance",dark,onDark);Setting("Wi-Fi only downloads","Restrict downloads on mobile data",wifi,onWifi)
  Row(Modifier.fillMaxWidth().clickable(onClick=account).padding(vertical=18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Person,null,tint=Purple2);Text("Account",color=Txt,modifier=Modifier.weight(1f).padding(start=12.dp));Icon(Icons.Default.ChevronRight,null,tint=Muted)}
  Text("ZIPMM MOVIE • Version 1.3",color=Muted,fontSize=12.sp)
 }
}
@Composable private fun Setting(title:String,sub:String,value:Boolean,onChange:(Boolean)->Unit){
 Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Txt,fontWeight=FontWeight.SemiBold);Text(sub,color=Muted,fontSize=11.sp)};Switch(value,onChange)}
}
private suspend fun authRequest(email:String,password:String,signup:Boolean):Pair<String?,String> = withContext(Dispatchers.IO){
 val key=BuildConfig.SUPABASE_PUBLISHABLE_KEY
 if(key.isBlank())return@withContext null to "Supabase Publishable Key ကို GitHub Secret ထဲမှာ ထည့်ရန်လိုပါတယ်။"
 try{
  val path=if(signup)"/auth/v1/signup" else "/auth/v1/token?grant_type=password"
  val c=URL(BuildConfig.SUPABASE_URL+path).openConnection() as HttpURLConnection
  c.requestMethod="POST";c.connectTimeout=15000;c.readTimeout=15000;c.doOutput=true
  c.setRequestProperty("apikey",key);c.setRequestProperty("Content-Type","application/json")
  c.outputStream.use{it.write(JSONObject().put("email",email).put("password",password).toString().toByteArray())}
  val code=c.responseCode;val body=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}?:"{}"
  val j=JSONObject(body)
  if(code in 200..299){val access=j.optString("access_token","");if(access.isNotBlank())access to "OK" else null to "Email confirmation လိုအပ်နိုင်ပါတယ်။ Email ကိုအတည်ပြုပြီး Login ဝင်ပါ။"}
  else null to j.optString("msg",j.optString("message",j.optString("error_description","Login မအောင်မြင်ပါ။")))
 }catch(e:Exception){null to "Connection error: "+e.localizedMessage}
}
