# ExKeyMo
Android external keyboard remapping without root https://exkeymo.herokuapp.com/ (**NOT AVAILABLE ANYMORE** - you'll have to run ExKeyMo locally).

Need more than two layouts? https://github.com/ris58h/custom-keyboard-layout

## Run locally

### Requirements
- Java.

### Get
Clone via [Git](https://git-scm.com/):
```
git clone git@github.com:ris58h/exkeymo-web.git
```
Or download [zip](https://github.com/ris58h/exkeymo-web/archive/refs/heads/master.zip).

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
