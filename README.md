## Getting Started

### Prerequisites

Before you begin, ensure you have the following prerequisites installed:

- Android Studio
- Kotlin

### Installation (not published now, please add the SDK locally)

To integrate the `PlayBackSDKManager` into your Android application, follow these steps:

1. Add the following dependency to your `build.gradle` file:

    ```groovy
    dependencies {
        implementation 'com.streamamg:playback-sdk:1.0.0'
    }
    ```

2. Sync your project to ensure the new dependency is downloaded and added to your project.

# PlayBackSDKManager

The `PlayBackSDKManager` is a singleton object designed to manage the functionalities of the playback SDK. It provides methods for initialization, loading player UI, and loading HLS streams. This README provides documentation on how to get started with using the PlayBackSDKManager in your application.

# Initialization

To initialize the playback SDK, use the `initialize` method of the `PlayBackSDKManager` singleton object. This method requires an API key for authentication. Optionally, you can specify a base URL for the playback API.

Example:

```kotlin
    // Initialize SDK with the settings
    PlayBackSDKManager.initialize("<API_KEY>") { license, error ->
        // Register default layer plugin 
        val customPlugin = BitmovinVideoPlayerPlugin()
        VideoPlayerPluginManager.registerPlugin(customPlugin)
    }
```


# Loading Player UI

To load the player UI in your application, use the `loadPlayer` method of the `PlayBackSDKManager` singleton object. This method returns a Composable function that you can use to render the player UI.

Example:

```
val playerUI = PlayBackSDKManager.loadPlayer(entryID, authorizationToken) { error -> 
// Handle player UI error 
} 
```

# Error Handling

The `PlayBackSDKManager` provides error handling through sealed classes `SDKError` and `PlayBackAPIError`. These classes represent various errors that can occur during SDK and API operations respectively.

- `SDKError` includes subclasses for initialization errors, missing license, and HLS stream loading errors.
- `PlayBackAPIError` includes subclasses for initialization errors, network errors, and API errors.

Handle errors based on these classes to provide appropriate feedback to users.

# Video Player Plugin Manager

Additionally, the package includes a singleton object `VideoPlayerPluginManager` for managing video player plugins. This object allows you to register, remove, and retrieve the currently selected video player plugin.

For further details on how to use the `VideoPlayerPluginManager`, refer to the inline documentation provided in the code.
