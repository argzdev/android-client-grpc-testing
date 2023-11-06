# android-client-grpc-testing

### Summary
- This repository is an android client project that connects to a gRPC server with the given IP address and port. The button press sends a simple Message with header to the server and displays the message received from the server.

Modified template from
[quick start]: https://grpc.io/docs/languages/go/quickstart

Follow these setup to run the example:

 1. Set the IP address that the client will connect to:

    * Physical Device + Local Server:

      * From the command line:

        1. `./gradlew :app:installDebug -PserverUrl=http://YOUR_MACHINE_IP:50051/`

      OR
      
      * From Android Studio / IntelliJ:

        1. Create a `gradle.properties` file in your root project directory containing:

            ```sh
            serverUrl=http://YOUR_MACHINE_IP:50051/
            ```

 3. Run the code
 4. Send a message
 5. Message with header is displayed on the server side

Server code: (e.g. [golang-server](https://github.com/argzdev/go-server-grpc-testing))
