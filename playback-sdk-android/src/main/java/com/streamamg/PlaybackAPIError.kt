package com.streamamg

/**
 * Sealed class representing errors related to the SDK operations.
 * Usage:
 * - SDKError and PlaybackAPIError are sealed classes that represent different types of errors that can occur during SDK and API operations respectively.
 * - SDKError has specific subclasses representing various initialization and licensing errors.
 * - PlaybackAPIError has specific subclasses representing initialization errors, network errors, and API errors.
 * - PlaybackAPIError provides a companion object with a factory method `apiError()` to create instances of `ApiError`.
 *
 */
sealed class SDKError : Throwable() {

    //region Public Subclasses

    /**
     * Error indicating an issue during SDK initialization.
     */
    data object InitializationError : SDKError() {
        private fun readResolve(): Any = InitializationError
    }

    /**
     * Error indicating missing license information.
     */
    data object MissingLicense : SDKError() {
        private fun readResolve(): Any = MissingLicense
    }

    /**
     * Error indicating a failure to fetch Bitmovin license.
     */
    data object FetchBitmovinLicenseError : SDKError() {
        private fun readResolve(): Any = FetchBitmovinLicenseError
    }

    //endregion
}

/**
 * Sealed class representing errors related to the PlaybackAPI operations.
 */
sealed class PlaybackAPIError : Throwable() {

    data object InitializationError : PlaybackAPIError() {
        private fun readResolve(): Any = InitializationError
        override val message: String?
            get() = "Initialization error occurred"
    }

    data class NetworkError(val error: Throwable) : PlaybackAPIError() {
        override val message: String?
            get() = "Network error: ${error.message}"
    }

    data class ApiError(val statusCode: Int, override val message: String, val reason: String) : PlaybackAPIError()

    companion object {
        fun apiError(statusCode: Int, message: String, reason: String): PlaybackAPIError {
            return ApiError(statusCode, message, reason)
        }
    }
}


