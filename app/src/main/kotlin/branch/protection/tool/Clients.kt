package branch.protection.tool

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import io.github.cdimascio.dotenv.dotenv

/**
 * Class for client to access GitHub graphql api
 */
 class Clients {
    private val dotenv = dotenv()
    private val token = dotenv["TOKEN"]

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain: Interceptor.Chain ->
            val original: Request = chain.request()
            val builder: Request.Builder = original.newBuilder().method(original.method, original.body)
            builder.header("Authorization", "bearer $token")
            chain.proceed(builder.build())
        }
        .build()

    val apolloClient: ApolloClient = ApolloClient.Builder()
        .serverUrl("https://api.github.com/graphql")
        .okHttpClient(okHttpClient)
        .build()
}