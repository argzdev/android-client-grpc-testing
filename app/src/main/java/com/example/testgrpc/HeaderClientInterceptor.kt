package com.example.testgrpc

import com.google.common.annotations.VisibleForTesting;
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.util.logging.Logger;

class HeaderClientInterceptor : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(method: MethodDescriptor<ReqT, RespT>?, callOptions: CallOptions?, next: Channel): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>?, headers: Metadata) {

                val authToken = runBlocking {
                    return@runBlocking Firebase.auth.currentUser?.getIdToken(false)
                        ?.await<GetTokenResult?>()?.token ?: return@runBlocking ""
                }

                headers.put(
                    CUSTOM_HEADER_KEY,
                    authToken
                )

                super.start(object : SimpleForwardingClientCallListener<RespT>(responseListener) {
                    override fun onHeaders(headers: Metadata) {
                        logger.info("header received from server:$headers")
                        super.onHeaders(headers)
                    }
                }, headers)
            }
        }
    }

    companion object {
        private val logger = Logger.getLogger(
            HeaderClientInterceptor::class.java.name
        )

        @VisibleForTesting
        val CUSTOM_HEADER_KEY: Metadata.Key<String> =
            Metadata.Key.of("custom_client_header_key", Metadata.ASCII_STRING_MARSHALLER)
    }
}