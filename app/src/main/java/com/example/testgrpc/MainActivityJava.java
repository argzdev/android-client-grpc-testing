package com.example.testgrpc;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.Closeable;
import java.io.IOException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class MainActivityJava extends ComponentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sayHello(View view) {
        Uri uri = Uri.parse(getResources().getString(R.string.server_url));
        GreeterRCP greeterService = new GreeterRCP(uri);

        String name = ((EditText) findViewById(R.id.et_name)).getText().toString();
        TextView result = ((TextView) findViewById(R.id.tv_result));

        FirebaseAuth.getInstance().signInAnonymously();

        greeterService.sayHello(name, result);
    }

    class GreeterRCP implements Closeable {

        private ManagedChannel channel;
        private GreeterGrpc.GreeterBlockingStub greeterJava;

        GreeterRCP(Uri uri) {
            ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(uri.getHost(), uri.getPort());
            if (uri.getScheme() == "https") {
                builder.useTransportSecurity();
            } else {
                builder.usePlaintext();
            }

            channel = builder.build();
            greeterJava = GreeterGrpc.newBlockingStub(channel);
        }

        public void sayHello(String name, TextView view) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            user.getIdToken(false).addOnCompleteListener(idTask -> {
                if (!idTask.isSuccessful()) return;

                Metadata headers = new Metadata();
                headers.put(
                    Metadata.Key.of("custom_client_header_key", Metadata.ASCII_STRING_MARSHALLER),
                    idTask.getResult().getToken()
                );

                try {
                    HelloRequest request = HelloRequest.newBuilder().setName(name).build();
                    HelloReply response = greeterJava.withInterceptors(
                            MetadataUtils.newAttachHeadersInterceptor(headers)).sayHello(request);
                    view.setText(response.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void close() throws IOException {
            channel.shutdownNow();
        }
    }
}
