//
//import android.content.Context
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.bitmovin.player.api.Player
//import com.bitmovin.player.api.source.SourceConfig
//
//@Composable
//fun PlaybackComposableView(entryId: String, authorizationToken: String?, context: Context) {
//    var player: Player? by remember { mutableStateOf(null) }
//
//    // Initialize the player outside AndroidView
//    if (player == null) {
//        player = Player(context)
//        val sourceConfig = SourceConfig.fromUrl("https://bitdash-a.akamaihd.net/content/sintel/sintel.mpd")
//        player?.load(sourceConfig)
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        //horizontalAlignment = Arrangement.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//     //   VideoSurface(modifier = Modifier.fillMaxSize(), player = player)
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Play button
////        Button(onClick = { player?.play() }) {
////            Text(text = "Play Video")
////        }
//    }
//}
//
////@Composable
////fun VideoSurface(modifier: Modifier = Modifier, player: Player?) {
////    Surface(modifier = modifier) { /* Implement drawing logic here */ }
////}