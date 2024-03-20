package com.streamamg.player.plugin

/**
 * Singleton object responsible for managing video player plugins.
 * Documentation:
 * - This singleton object `VideoPlayerPluginManager` manages video player plugins.
 * - It provides methods to register, remove, and retrieve the currently selected video player plugin.
 * - The `registerPlugin()` method registers a video player plugin.
 * - The `removePlugin()` method removes the currently registered video player plugin.
 * - The `getSelectedPlugin()` method retrieves the currently selected video player plugin.
 * - The selected plugin is stored in the internal property `selectedPlugin`.
 */
object VideoPlayerPluginManager {

    //region Properties

    //region Internal Properties

    /**
     * Currently selected video player plugin.
     */
    internal var selectedPlugin: VideoPlayerPlugin? = null

    //endregion

    //endregion

    //region Public methods

    /**
     * Registers a video player plugin.
     * @param plugin The video player plugin to register.
     */
    fun registerPlugin(plugin: VideoPlayerPlugin) {
        selectedPlugin = plugin
    }

    /**
     * Removes the currently registered video player plugin.
     */
    fun removePlugin() {
        selectedPlugin = null
    }

    /**
     * Retrieves the currently selected video player plugin.
     * @return The currently selected video player plugin, or null if none is registered.
     */
    fun getSelectedPlugin(): VideoPlayerPlugin? {
        return selectedPlugin
    }

    //endregion

}
