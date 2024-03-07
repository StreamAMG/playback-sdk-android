//import android.view.View
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.viewinterop.AndroidView
//import com.bitmovin.player.PlayerView
//import com.bitmovin.player.api.source.Source
//import com.bitmovin.player.api.source.SourceConfig
//import com.bitmovin.player.api.source.SourceType
//import com.streamamg.playback_sdk_android.R
//
//@Composable
//fun BitmovinPlayerViewWithSource(
//    modifier: Modifier = Modifier,
//    sourceUrl: String
//) {
//    Box(modifier = modifier.fillMaxSize()) {
//        AndroidView(
//            factory = { context ->
//                View.inflate(context, R.layout.player_view, null)
//            },
//            modifier = Modifier.fillMaxSize(),
//            update = { view ->
//                val playerView = view.findViewById<PlayerView>(R.id.playerView)
//                initPlayer(playerView, sourceUrl)
//            }
//        )
//    }
//}
//
//fun initPlayer(playerView: PlayerView, sourceUrl: String) {
//    val sourceConfig = SourceConfig(sourceUrl, SourceType.Dash)
//    val source = Source(sourceConfig)
//    playerView.player?.load(source)
//}
