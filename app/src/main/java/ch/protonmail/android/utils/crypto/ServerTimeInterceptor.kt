/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.utils.crypto

import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.utils.ServerTime
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ServerTimeInterceptor(
    var openPgp: OpenPGP,
    var queueNetworkUtil: QueueNetworkUtil
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        try {
            response = chain.proceed(request)
            handleResponse(response)
        } catch (exception: IOException) {
            Timber.d(exception, "IOException ${request.url()}")
            queueNetworkUtil.setCurrentlyHasConnectivity(false)
        }

        if (response == null) {
            return chain.proceed(request)
        }
        return response
    }

    private fun handleResponse(response: Response) {
        val dateString = response.header("date", null) ?: return
        try {
            val date = RFC_1123_FORMAT.parse(dateString)
            openPgp.updateTime(date.time / 1000)
            ServerTime.updateServerTime(date.time)
        } catch (exception: ParseException) {
            Timber.w(exception, "Date parse exception")
        }

    }

    companion object {
        private val RFC_1123_FORMAT: SimpleDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)

        init {
            RFC_1123_FORMAT.timeZone = TimeZone.getTimeZone("GMT")
        }
    }
}
