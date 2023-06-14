# ExKeyMo
Android external keyboard remapping without root https://exkeymo.herokuapp.com/ (**NOT AVAILABLE ANYMORE** - you'll have to run ExKeyMo locally).

Need more than two layouts? https://github.com/ris58h/custom-keyboard-layout

## Run locally

### Clone
```
git clone git@github.com:ris58h/exkeymo-web.git
cd exkeymo-web
```

### Build
```
./mvnw clean install
```

### Run
```
java -Dkeystore.password=exkeymo -cp 'target/classes:target/dependency/*' ris58h.exkeymo.web.Main
```

### Use
Visit [http://localhost/](http://localhost/).
