package com.example.testgrpc

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.grpc.Channel
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class MainActivityKotlin : ComponentActivity() {

    private val uri by lazy { Uri.parse(resources.getString(R.string.server_url)) }
    private val greeterService by lazy { GreeterRCP(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                Greeter(greeterService)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        greeterService.close()
    }
}

class GreeterRCP(uri: Uri) : Closeable {
    val responseState = mutableStateOf("")
    private val originChannel = let {
        println("Connecting to ${uri.host}:${uri.port}")

        val builder = ManagedChannelBuilder.forAddress(uri.host, uri.port)
        if (uri.scheme == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        builder.executor(Dispatchers.IO.asExecutor()).build()
    }

    /***
     * Option A
     * Insert Auth token from the ClientInterceptor
     */
//    private val interceptor: ClientInterceptor = HeaderClientInterceptor()
//    private val channel: Channel = ClientInterceptors.intercept(originChannel, interceptor)
//    private val greeterKt = GreeterGrpcKt.GreeterCoroutineStub(channel)
    private val greeterKt = GreeterGrpcKt.GreeterCoroutineStub(originChannel)

    suspend fun sayHello(name: String) {
        Firebase.auth.signInAnonymously().await()
        val authToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token ?: ""


        /***
         * Option B
         * Insert Auth token directly into the generated gRPC stub
         */
        val headers = Metadata()
        headers.put(
            Metadata.Key.of("custom_client_header_key", Metadata.ASCII_STRING_MARSHALLER),
            authToken
        )

        try {
            val request = helloRequest { this.name = name }
            val response = greeterKt.sayHello(request, headers)
            responseState.value = response.message
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    override fun close() {
//        (channel as ManagedChannel).shutdownNow()
        originChannel.shutdownNow()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeter(greeterRCP: GreeterRCP) {

    val scope = rememberCoroutineScope()

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
        Text("Name:", modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(nameState.value, { nameState.value = it })

        Button({ scope.launch { greeterRCP.sayHello(nameState.value.text) } }, Modifier.padding(10.dp)) {
            Text("Send GRPC Request:")
        }

        if (greeterRCP.responseState.value.isNotEmpty()) {
            Text("Server Request:", modifier = Modifier.padding(top = 10.dp))
            Text(greeterRCP.responseState.value)
        }
    }
}