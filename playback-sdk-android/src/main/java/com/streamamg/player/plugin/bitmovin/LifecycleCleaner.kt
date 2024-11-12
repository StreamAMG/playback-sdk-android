package com.streamamg.player.plugin.bitmovin

import android.content.Context

internal interface LifecycleCleaner {
    fun clean(context: Context)
}