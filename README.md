# ExKeyMo
Android external keyboard remapping without root https://exkeymo.herokuapp.com/ (**NOT AVAILABLE ANYMORE** - you'll have to run ExKeyMo locally to create an APK with your custom layout or use a prebuilt APK).

Need more than two layouts? https://github.com/ris58h/custom-keyboard-layout

## Prebuilt APKs
- CapsLock to Ctrl [ExKeyMo-caps2ctrl.zip](https://github.com/ris58h/exkeymo-web/files/12775514/ExKeyMo-caps2ctrl.zip)
- CapsLock to Ctrl and vice versa [ExKeyMo-swap-caps-and-ctrl.zip](https://github.com/ris58h/exkeymo-web/files/12775516/ExKeyMo-swap-caps-and-ctrl.zip)
- CapsLock to Esc [ExKeyMo-caps2esc.zip](https://github.com/ris58h/exkeymo-web/files/12775515/ExKeyMo-caps2esc.zip)
- CapsLock to Esc and vice versa [ExKeyMo-swap-caps-and-esc.zip](https://github.com/ris58h/exkeymo-web/files/12775517/ExKeyMo-swap-caps-and-esc.zip)

## Run locally

### Requirements
- Java (17 or higher).

### Get
Clone the source code via Git:
```
git clone git@github.com:ris58h/exkeymo-web.git
```
Or [download](https://github.com/ris58h/exkeymo-web/archive/refs/heads/master.zip) it as zip.

### Build
```
./mvnw clean install
```

### Run
```
java -jar target/exkeymo-web-*-jar-with-dependencies.jar
```
To run on a specific port use `server.port` system property:
```
java -Dserver.port=PORT_NUMBER -jar target/exkeymo-web-*-jar-with-dependencies.jar
```

### Use
Visit [http://localhost/](http://localhost/) and don't forget to __RTFM__ ([http://localhost/docs.html](http://localhost/docs.html)).

### Run with Docker
You can run the application using Docker. There are two ways to do this: using Docker Compose or just Docker.

#### Using Docker
Build the Docker image:
```bash
docker build -t exkeymo-web .
```

Run the Docker container:
```bash
docker run -p 80:80 exkeymo-web
```
This will start the container and expose the application on port 80.

#### Using Docker Compose

Make sure you have Docker and Docker Compose installed. Then, run the following command in the directory containing the `docker-compose.yml` file:
```bash
docker-compose up
```
This will build the Docker image and start the container, exposing the application on port 80.
