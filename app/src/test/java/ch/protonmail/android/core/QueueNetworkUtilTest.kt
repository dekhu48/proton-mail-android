/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.testutils.extensions.asArray
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal open class QueueNetworkUtilTest {

    protected val networkInfoMock = mockk<NetworkInfo>()
    private val connectivityManagerMock = mockk<ConnectivityManager>(relaxUnitFun = true) {
        every { activeNetworkInfo } returns networkInfoMock
    }
    private val contextMock = mockk<Context>(relaxUnitFun = true) {
        every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManagerMock
        every { applicationContext } returns this
        every { registerReceiver(any(), any()) } returns mockk()
    }
    private val networkConfiguratorMock = mockk<NetworkConfigurator>(relaxUnitFun = true)
    protected val queueNetworkUtil = QueueNetworkUtil(contextMock, networkConfiguratorMock)

    @RunWith(Parameterized::class)
    internal class UpdatingRealConnectivityShouldCallListener(
        private val testInput: TestInput
    ) : QueueNetworkUtilTest() {

        private val listenerMock = mockk<NetworkEventProvider.Listener>(relaxUnitFun = true)

        @Test
        fun `should call the listener as soon as the real connectivity is updated`() = with(testInput) {
            // given
            every { networkInfoMock.isConnectedOrConnecting } returns isConnectedOrConnecting
            queueNetworkUtil.setListener(listenerMock)

            // when
            queueNetworkUtil.updateRealConnectivity(serverAccessible, connectionState)

            // then
            listenerMock.onNetworkChange(expectedNetworkStatus)
        }

        companion object {

            private val pingNeededStateInputs = listOf(
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.PING_NEEDED,
                    expectedNetworkStatus = NetworkUtil.METERED
                ),
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.PING_NEEDED,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.PING_NEEDED,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.PING_NEEDED,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                )
            )

            private val connectedStateInputs = listOf(
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.CONNECTED,
                    expectedNetworkStatus = NetworkUtil.METERED
                ),
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.CONNECTED,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.CONNECTED,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.CONNECTED,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                )
            )

            private val noInternetStateInputs = listOf(
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.NO_INTERNET,
                    expectedNetworkStatus = NetworkUtil.METERED
                ),
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.NO_INTERNET,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.NO_INTERNET,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.NO_INTERNET,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                )
            )

            private val cantReachServerInputs = listOf(
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.CANT_REACH_SERVER,
                    expectedNetworkStatus = NetworkUtil.METERED
                ),
                TestInput(
                    serverAccessible = true,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.CANT_REACH_SERVER,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = true,
                    connectionState = Constants.ConnectionState.CANT_REACH_SERVER,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                ),
                TestInput(
                    serverAccessible = false,
                    isConnectedOrConnecting = false,
                    connectionState = Constants.ConnectionState.CANT_REACH_SERVER,
                    expectedNetworkStatus = NetworkUtil.DISCONNECTED
                )
            )

            @JvmStatic
            @Parameterized.Parameters
            fun data(): Collection<Array<Any>> {
                return (pingNeededStateInputs + connectedStateInputs + noInternetStateInputs + cantReachServerInputs)
                    .map { it.asArray() }
            }
        }

        internal data class TestInput(
            val serverAccessible: Boolean,
            val isConnectedOrConnecting: Boolean,
            val connectionState: Constants.ConnectionState,
            val expectedNetworkStatus: Int
        )
    }
}
