# Playback SDK Android

---

[![](https://jitpack.io/v/StreamAMG/playback-sdk-android.svg)](https://jitpack.io/#StreamAMG/playback-sdk-android)

## Getting Started

### Prerequisites

Before you begin, ensure you have the following prerequisites installed:

- Android Studio
- Kotlin

### Installation

To integrate the `PlaybackSDKManager` into your Android application, follow these steps:

1. Add a link to Jitpack and Bitmovin release repository to your application's `settings.gradle.kts` file:

   ```groovy
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
           maven {
               url = uri("https://artifacts.bitmovin.com/artifactory/public-releases")
           }
           maven {
               url = uri("https://jitpack.io")
           }
       }
   }
   ```

2. Add the following dependency to your app `build.gradle.kts` file:

   ```groovy
   dependencies {
       ...
       implementation 'com.github.StreamAMG:playback-sdk-android:x.x.x'
       implementation 'com.bitmovin.player:player:x.x.x'
   }
   ```

   - This is the latest version of the Playback SDK [![](https://jitpack.io/v/StreamAMG/playback-sdk-android.svg)](https://jitpack.io/#StreamAMG/playback-sdk-android)
   - You can check the latest Bitmovin player version [HERE](https://developer.bitmovin.com/playback/docs/release-notes-android)

3. Add the following plugins on your project `build.gradle.kts` file:

   ```groovy
   plugins {
       ...
       id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
       id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply true
   }
   ```

4. Sync your project to ensure the new dependency is downloaded and added to your project.

### Installation from local path

If you want to debug and integrate Playback SDK from your local path, follow these steps:

1. Add the path of the local SDK to your application's `settings.gradle.kts` file:

   ```groovy
   include(":app")
   include (":playback-sdk-android")
   project(":playback-sdk-android").projectDir = File("../playback-sdk-android/playback-sdk-android")
   ```

   Change the File path based on your local SDK path

2. Add the following dependency to your app `build.gradle.kts` file:

   ```groovy
   dependencies {
       ...
       implementation(project(":playback-sdk-android"))
       implementation 'com.bitmovin.player:player:x.x.x'
   }
   ```

   - You can check the latest Bitmovin player version [HERE](https://developer.bitmovin.com/playback/docs/release-notes-android)

3. Add the following plugins on your project `build.gradle.kts` file:

   ```groovy
   plugins {
       ...
       id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
       id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply true
   }
   ```

4. Sync your project to ensure the new dependency is downloaded and added to your project.

## PlaybackSDKManager

The `PlaybackSDKManager` is a singleton object designed to manage the functionalities of the playback SDK. It provides methods for initialization, loading player UI, and loading HLS streams. This README provides documentation on how to get started with using the PlaybackSDKManager in your application.

## Initialization

To initialize the playback SDK, use the `initialize` method of the `PlaybackSDKManager` singleton object. This method requires an API key for authentication. Optionally, you can specify a base URL for the playback API.

Example:

```kotlin
    // Initialize SDK with the settings
    PlaybackSDKManager.initialize("<API_KEY>") { license, error ->
        // Register default layer plugin 
        val customPlugin = BitmovinVideoPlayerPlugin()
        VideoPlayerPluginManager.registerPlugin(customPlugin)
    }
```

## Loading Player UI

To load the player UI in your application, use the `loadPlayer` method of the `PlaybackSDKManager` singleton object. This method is a Composable function that you can use to load and render the player UI.

Example:

```kotlin
PlaybackSDKManager.loadPlayer(entryID, authorizationToken) { error ->
    // Handle player UI error
}
```

## Loading a Playlist

To load a sequential list of videos into the player UI, use the `loadPlaylist` method of the `PlaybackSDKManager` singleton object. This method is a Composable function that you can use to load and render the player UI.
`entryIDs`: An array of Strings containing the unique identifiers of all the videos in the playlist.
`entryIDToPlay`: (Optional) Specifies the unique video identifier that will be played first in the playlist. If not provided, the first video in the `entryIDs` array will be played.

Example:

```kotlin
PlaybackSDKManager.loadPlaylist(
    entryIDs,
    entryIDToPlay
) { errors ->
    // Handle player UI playlist errors
}
```

### Controlling Playlist Playback

To control playlist playback, you can access to the VideoPlayerPluginManager singleton instance. This allows you to access various control functions and retrieve information about the current playback state.

Here are some of the key functions you can utilize:

`playFirst()`: Plays the first video in the playlist.
`playPrevious()`: Plays the previous video in the playlist.
`playNext()`: Plays the next video in the playlist.
`playLast()`: Plays the last video in the playlist.
`seek(entryIdToSeek)`: Seek a specific video Id
`activeEntryId()`: Returns the unique identifier of the currently playing video.

By effectively leveraging these functions, you can create dynamic and interactive video player experiences.

Example:

```kotlin
// You can use the following playlist controls
VideoPlayerPluginManager.getSelectedPlugin()?.playFirst() // Play the first video
VideoPlayerPluginManager.getSelectedPlugin()?.playPrevious() // Play the previous video
VideoPlayerPluginManager.getSelectedPlugin()?.playNext() // Play the next video
VideoPlayerPluginManager.getSelectedPlugin()?.playLast() // Play the last video
VideoPlayerPluginManager.getSelectedPlugin()?.seek(entryIdToSeek) { success -> // Seek a specific video
    if (!success) {
        val errorMessage = "Unable to seek video Id $newEntryId"
    }
}
VideoPlayerPluginManager.getSelectedPlugin()?.activeEntryId() // Get the active video Id
```

### Receiving Playlist Events

To receive playlist events, you can access to the VideoPlayerPluginManager singleton instance, similar to how you did in the Controlling Playlist Playback section.
Utilize the SharedFlow `events` to listen for player events, such as the `PlaylistTransition` event. This event provides information about the transition from one video to another.

Example:

```kotlin
VideoPlayerPluginManager.getSelectedPlugin()?.events?.collect { event ->
   // Handle the event and update UI
   when (event) {
      is PlayerEvent.PlaylistTransition -> {
         Log.d("Playback", "Playlist transition ${event.from.config.metadata?.get("entryId")} -> ${event.to.config.metadata?.get("entryId")}")
      }
      is PlayerEvent.Ready -> {
         // Player is ready
      }
   }
}
```

## Playing Access-Controlled Content

To play on-demand and live videos that require authorization, at some point before loading the player your app must call CloudPay to start session, passing the authorization token:

```kotlin
"$baseURL/sso/start?token=$authorizationToken"
```

Then the same token should be passed into the `loadPlayer(entryID, authorizationToken, onError)` method of `PlaybackSDkManager`. For the free videos that user should be able to watch without logging in, starting the session is not required and `authorizationToken` can be set to an empty string or `null`.

> \[!NOTE]
> If the user is authenticated, has enough access level to watch a video, the session was started and the same token was passed to the player but the videos still throw a 401 error, it might be related to these requests having different user-agent headers.

## Configure user-agent

Sometimes a custom `user-agent` header is automatically set for the requests on Android when creating a token and starting a session. `OkHttp` and other 3rd party networking frameworks can modify this header to include information about themselves. In such cases they should either be configured to not modify the header, or the custom header should be passed to the player as well.

Example:

```kotlin
PlaybackSDKManager.initialize(
    apiKey = apiKey,
    baseUrl = baseUrl,
    userAgent = customUserAgent
) { error ->
    // Handle player UI error
}
```

## Playing Free Content

If you want to allow users to access free content or if you're implementing a guest mode, you can pass an empty string or `null`
value as the `authorizationToken` parameter when calling the `loadPlayer` function. This will bypass the need for authentication, enabling unrestricted access to the specified content.

## Error Handling

The `PlaybackSDKManager` provides error handling through sealed classes `SDKError` and `PlaybackAPIError`. These classes represent various errors that can occur during SDK and API operations respectively.

- `SDKError` includes subclasses for initialization errors, missing license, and HLS stream loading errors.
- `PlaybackAPIError` includes subclasses for initialization errors, network errors, and API errors.

Handle errors based on these classes to provide appropriate feedback to users.

## Video Player Plugin Manager

Additionally, the package includes a singleton object `VideoPlayerPluginManager` for managing video player plugins. This object allows you to register, remove, and retrieve the currently selected video player plugin.

For further details on how to use the `VideoPlayerPluginManager`, refer to the inline documentation provided in the code.

## Chromecasting

To use the Google Chromecast support, use the `updateCastContext` method of the `PlaybackSDKManager` singleton object, passing the context of the Activity otherwise the Casting will be disabled. Each Activity that uses Cast related API's has to call the following function before using any cast related API, e.g. in the `Activity.onCreate` function:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlaybackSDKManager.updateCastContext(this)
        ...
    }
```
## Bitmovin analytics

Currently SDK support tracking analytics on Bitmovin service. In case you have a logged-in user and want to track Bitmovin analytics for the current session, you need to pass the user's ID in the `analyticsViewerId` parameter.

```kotlin
    setContent {
        entryId = "..."
        authorizationToken = "..."
        viewerAnalyticsId = "user id or empty string"
        PlaybackSDKManager.loadPlayer(entryId, authorizationToken, viewerAnalyticsId) { error ->
            onPlayerError(error)
        }
    }
```

## Playlist and Analytics

To track analytics while utilizing the playlist functionality, you can provide the user's ID via the `analyticsViewerId` parameter.
Below is an example implementation in Kotlin using the Playback SDK:

```kotlin
   var entryIDs: Array<String> by remember { mutableStateOf(arrayOf("ENTRY_ID1", "ENTRY_ID_2", "ENTRY_ID_3")) }
   val entryIDToPlay = "ENTRY_ID_2" // Optional parameter
   val authorizationToken = "JWT_TOKEN"
   val analyticsViewerId = "user id or empty string"

   PlaybackSDKManager.loadPlaylist(
      entryIDs,
      entryIDToPlay, 
      authorizationToken, 
      analyticsViewerId
   ) { errors ->
      // Handle player UI playlist errors
   }
```

## Resources

- **Tutorial:** [Tutorial](https://streamamg.github.io/playback-sdk-android/tutorials/playbacksdk/getstarted)
- **Demo app:** [GitHub Repository](https://github.com/StreamAMG/playback-demo-android)
