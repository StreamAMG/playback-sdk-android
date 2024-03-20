package com.streamamg

/**
 * Sealed class representing errors related to the SDK operations.
 * Usage:
 * - SDKError and PlayBackAPIError are sealed classes that represent different types of errors that can occur during SDK and API operations respectively.
 * - SDKError has specific subclasses representing various initialization and licensing errors.
 * - PlayBackAPIError has specific subclasses representing initialization errors, network errors, and API errors.
 * - PlayBackAPIError provides a companion object with a factory method `apiError()` to create instances of `ApiError`.
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
     * Error indicating a failure to load HLS stream.
     */
    data object LoadHLSStreamError : SDKError() {
        private fun readResolve(): Any = LoadHLSStreamError
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
 * Sealed class representing errors related to the PlayBackAPI operations.
 */
sealed class PlayBackAPIError : Throwable() {

    //region Public Subclasses

    /**
     * Error indicating an issue during API initialization.
     */
    data object InitializationError : PlayBackAPIError() {
        private fun readResolve(): Any = InitializationError
    }

    /**
     * Error representing a network-related issue.
     * @property error The throwable representing the network error.
     */
    data class NetworkError(val error: Throwable) : PlayBackAPIError()

    /**
     * Error representing an API-related issue.
     * @property statusCode The HTTP status code of the error.
     * @property message The error message.
     */
    data class ApiError(val statusCode: Int, override val message: String) : PlayBackAPIError()

    companion object {
        /**
         * Factory method to create an [ApiError] instance.
         * @param statusCode The HTTP status code of the error.
         * @param message The error message.
         * @return An instance of [ApiError] representing the API error.
         */
        fun apiError(statusCode: Int, message: String): PlayBackAPIError {
            return ApiError(statusCode, message)
        }
    }

    //endregion
}

