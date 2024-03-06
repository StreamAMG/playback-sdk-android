package com.streamamg

sealed class SDKError : Throwable() {
    object InitializationError : SDKError()
    object MissingLicense : SDKError()
    object LoadHLSStreamError : SDKError()
}

sealed class PlayBackAPIError : Throwable() {
    object InvalidResponsePlaybackData : PlayBackAPIError()
    object InvalidPlaybackDataURL : PlayBackAPIError()
    object InvalidPlayerInformationURL : PlayBackAPIError()
    object InitializationError : PlayBackAPIError()
    object LoadHLSStreamError : PlayBackAPIError()
    data class NetworkError(val error: Throwable) : PlayBackAPIError()
    data class ApiError(val statusCode: Int, override val message: String) : PlayBackAPIError()

    companion object {
        fun apiError(statusCode: Int, message: String): PlayBackAPIError {
            return ApiError(statusCode, message)
        }
    }
}
