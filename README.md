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
Or download it as [zip](https://github.com/ris58h/exkeymo-web/archive/refs/heads/master.zip).

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
