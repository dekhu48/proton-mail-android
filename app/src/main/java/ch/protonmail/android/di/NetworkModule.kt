package ch.protonmail.android.di

import android.content.Context
import ch.protonmail.android.api.ProtonMailApiClient
import ch.protonmail.android.api.TokenSessionManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.CoreLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.data.di.ApiFactory
import me.proton.core.network.data.di.NetworkManager
import me.proton.core.network.data.di.NetworkPrefs
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkPrefs
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.util.kotlin.Logger
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCoreLogger(): Logger = CoreLogger()

    @Provides
    @Singleton
    fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager =
        NetworkManager(context)

    @Provides
    @Singleton
    fun provideNetworkPrefs(@ApplicationContext context: Context) =
        NetworkPrefs(context)

    @Provides
    @Singleton
    fun provideApiFactory(
        logger: Logger,
        apiClient: ApiClient,
        networkManager: NetworkManager,
        networkPrefs: NetworkPrefs,
        sessionProvider: SessionProvider,
        sessionListener: SessionListener
    ): ApiFactory = ApiFactory(
        Constants.ENDPOINT_URI, apiClient, logger, networkManager, networkPrefs, sessionProvider, sessionListener,
        cookieStore = null, CoroutineScope(Job() + Dispatchers.Default)
    )

    @Provides
    @Singleton
    fun provideApiProvider(apiFactory: ApiFactory, sessionProvider: SessionProvider): ApiProvider =
        ApiProvider(apiFactory, sessionProvider)
}

@Module
@InstallIn(ApplicationComponent::class)
abstract class NetworkBindsModule {
    @Binds
    abstract fun provideApiClient(apiClient: ProtonMailApiClient): ApiClient

    @Binds
    abstract fun provideSessionProvider(sessionManager: TokenSessionManager): SessionProvider

    @Binds
    abstract fun provideSessionListener(sessionManager: TokenSessionManager): SessionListener
}
