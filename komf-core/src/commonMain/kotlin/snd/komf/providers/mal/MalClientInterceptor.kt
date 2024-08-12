package snd.komf.providers.mal

//import okhttp3.Interceptor
//import okhttp3.Response
//
//class MalClientInterceptor(private val clientId: String) : Interceptor {
//
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val request = chain.request()
//        val builder = request.newBuilder().header("X-MAL-CLIENT-ID", clientId)
//        return chain.proceed(builder.build())
//    }
//}
//
