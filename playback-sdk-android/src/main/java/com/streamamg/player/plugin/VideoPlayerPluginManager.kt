package com.streamamg.player.plugin

object VideoPlayerPluginManager {
    internal var selectedPlugin: VideoPlayerPlugin? = null

    fun registerPlugin(plugin: VideoPlayerPlugin) {
        selectedPlugin = plugin
    }

    fun removePlugin() {
        selectedPlugin = null
    }

    fun getSelectedPlugin(): VideoPlayerPlugin? {
        return selectedPlugin
    }
}
