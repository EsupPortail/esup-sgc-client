ESUP-SGC-CLIENT
===============

Pour une documentation fonctionnelle de l'outil et sa mise en oeuvre, merci de vous référer 
à la documentation ici du wiki ESUP-Portail, notamment ici :
 * [projet global esup-sgc](https://www.esup-portail.org/wiki/display/SGC)
 * [page wiki ESUP spécifiique à esup-sgc-client](https://www.esup-portail.org/wiki/display/SGC/Clients+ESUP-SGC-CLIENT+v2.0)

Dans ces pages, on indique notamment que l'installation de cet outil en production est facilitée par un installateur 
qui peut lui-même être généré depuis un simple formulaire web en ligne via [esup-sgc-client-installer](https://github.com/EsupPortail/esup-sgc-client-installer)


## Technologies

esup-sgc-client est une application java/spring/javafx utilisant maven pour gérer la compilation 
et les dépendances des librairies utilisées.

Pour fonctionner, il requiert un openjdk 11 (ou supérieur) et openjfx

## Environnement de développement

Vous pouvez utiliser comme IDE Eclipse (depuis Spring Tools par exemple) ou IntelliJ IDEA pour le développement.

Vous pouvez lancer le projet via une simple commande maven ainsi : 
````
mvn javafx:run
````

Cependant, depuis votre IDE, le débogage via des points d'arrêt (breakpoint) risque de poser problème.
Il vaut mieux alors lancer le projet en tant qu'application java. Vous devrez alors spécifier les options javafx/openjfx 
en précissant les "VM Options" de ce type :
````
--module-path /usr/local/javafx-sdk-19/lib/ --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.media,javafx.graphics,javafx.swing,javafx.web
````

Suivant le certificat utilisé par vos serveurs esup-sgc/esup-nfc-tag, et la version de java utilisée, vous devrez également rajouter dans les options java
````
-Djdk.tls.client.protocols=TLSv1.2
````

Pour activer le profile/module evolis dans l'IDE, vous pouvez ajouter le "classpath of module" `-cp esupsgclient-evolis`  

En optant pour `-cp esupsgclient-core` vous aurez le client sans module/profile supplémentaire.

## SceneBuilder

L'interface homme machine (IHM / GUI) est décrite en FXML. 

Le fichier fxml donnant la scène applicative est [src/main/resources/esup-sgc-client.fxml](src/main/resources/esup-sgc-client.fxml)

Ce fichier est construit et maintenu grâce à [Scene Builder](https://gluonhq.com/products/scene-builder/)

Pour avoir un rendu proche de l'application lancée, il vous faut ajouter dans Scene Builder le fichier css 
[src/main/resources/bootstrapfx4scenebuilder.css](src/main/resources/bootstrapfx4scenebuilder.css) 
issu de [boostrapfx](https://github.com/kordamp/bootstrapfx)

boostrapfx étant en fait réellement intégré dans l'application en tant que librairie maven (comme pour toutes les librairies dont dépend esup-sgc-client)


## Dépendances techniques

esup-sgc-client fonctionne avec :
* requis
  * un serveur esup-nfc-tag 
  * un lecteur NFC USB permettant de passer des APDUs (notamment Mifare DESFIRE) via PC/SC, celui-ci peut correspondre au lecteur NFC de l'imprimante Evolis Primacy (1 ou 2)
* optionnel
  * une webcam 
  * une imprimante evolis primacy 1 ou 2 (ou plus exactement Evolis Premium Suite 1 ou 2)
  * un serveur esup-sgc


### modules maven

Des sous-modules maven peuvent être utilisés pour ajouter le support aux imprimantes evolis (et bientôt zebra).
Pour ajouter le module evolis, vous pouvez ajouter `-P evolis` à vos commandes maven.

Ainsi
````
mvn -P evolis clean package
````
permet de récupérer le client esup-sg-client avec le module esupsgcclient-evolis ici : `esupsgcclient-assembly/target/esup-sgc-client-final.jar`

De même, pour lancer directement cette application avec esupsgcclient-evolis de chargé via meven, vous pouvez lancer :
````
mvn -P evolis clean javafx:run
````

Depuis votre IDE (intellij idea par exemple), vous pouvez travailler/lancer l'application en spécifiant le module via `-cp esupsgcclient-evolis` ;
la classe principale à lancer restant `org.esupportail.esupsgcclient.EsupSgcClientApplication`.

### evolis primacy 2

Il vous faut installer le driver de votre encodeur NFC intégré à votre Primacy 2
(Si vous avez opté pour un encodeur "SpringCard CrazyWriter" par exemple, vous trouverez le driver depuis https://www.springcard.com/en/download/drivers : "PC/SC Driver for USB couplers" / fichier sd16055-2104.exe).

Pour la primacy 2 en elle-même, il vous faut installer le "Evolis Premium Suite 2" depuis https://myplace.evolis.com/s/quick-install-step-4?language=fr

Notez que "Evolis Premium Suite 2" ne fonctionne que depuis Windows, le lecteur NFC ne fonctionne que via USB : le client esup-sgc-client doit donc être installé sur ce même windows ; en phase de développement on peut se contenter de manipuler à distance.

esup-sgc-client dialogue en effet par TCP avec "Evolis Premium Suite 2" qui se charge de retransmettre les ordres à l'imprimante.

Pour la phase d'encodage, une fois l'ordre donnée de positionner la carte au niveau de l'encodeur, esup-sgc-client dialogue directement avec l'encodeur NFC en pc/sc.

"Evolis Premium Suite 2" doit être configuré pour permettre cette communication par TCP.
Dans le répertoire bin de "Evolis Premium Suite 2" (dans Program Files), il vous faut modifier evoservice.properties: 
````
RequestServer.tcpport = 18000
RequestServer.tcpenabled = true
````

Une fois ces modifications apportées, vous devez redémarrer le servie windows "Evolis Premium Suite 2" (via la gestion classique des "service windows").

Le fichier de configuration d'esup-sgc-client donné dans [src/main/resources/esupsgcclient.properties](src/main/resources/esupsgcclient.properties) doit reprendre ce même numéro de port.

### evolis primacy 1

La mise en place pour Evolis Primacy 1 (par rapport à Primacy 2) est très similaire, il vous faudra cependant installer non pas "Evolis Premium Suite 2" mais "Evolis Premium Suite".
Le fichier de configuration à modifier est C:\Program Files\Evolis Card Printer\Evolis Premium Suite\ESPFSvc.properties pour activer le support de la communication par TCP (port 18000) :
```
ESPFServerManager.enabletcpatstart = true
```

Au niveau d'esup-sgc-client, il faudra spécifier dans src/main/resources/esupsgcclient.properties :
```
printerEvolisVersion=1
```

### simulation de evolis primacy 2

Pour le développement, on peut aussi se contenter de 'simuler' l'API de "Evolis Premium Suite 2" 
Pour ce faire, il vous suffit de lancer le script python (python2 ou python3) [src/etc/dummyEvolisPrinterCenter.py](src/etc/dummyEvolisPrinterCenter.py)
````
python3 dummyEvolisPrinterCenter.py
````

Ce script, basique, ne fait que répondre ``{"id":"1","jsonrpc":"2.0","result":"OK"}`` à toute requête émanant de esup-sgc-client.

Il est donc loin de 'simuler' à proprement parler les interactions avec evolis primacy 2; mais ça reste tout à fait suffisant pour une grand part du développement.

### esup-nfc-tag et esup-sgc de démonstration

A défaut d'installer un [serveur esup-nfc-tag](https://github.com/EsupPortail/esup-nfc-tag-server) 
et un [serveur esup-sgc](https://github.com/EsupPortail/esup-sgc) et de le configurer complètement 
pour avoir un environnement de développement complet, vous pouvez opter pour simplement utiliser la [plateforme de démonstration esup-sgc](https://www.esup-portail.org/wiki/pages/viewpage.action?pageId=615547103)
accessible à tout personnel de l'ESR au travers de la fédération ESR portée par RENATER.

Pour ce faire, vous n'avez qu'à spécifier dans [esupsgcclient.properties](src/main/resources/esupsgcclient.properties) les configurations suivantes : 
````
esupSgcUrl = https://esup-sgc-demo.univ-rouen.fr
esupNfcTagServerUrl = https://esup-nfc-tag-demo.univ-rouen.f
````

Finalement, avec la plateforme de démonstration et la simulation de evolis primacy 2, vous n'avez besoin réellement 
que d'une webcam et d'un lecteur NFC USB pour disposer de l'ensemble des dépendances matérielles et logicielles
(requises comme optionnelles) pour prendre part au développement d'esup-sgc-client.

## Copie d'écran

![esup-sgc-client au 10/12/2022](src/etc/esup-sgc-client-10122022.png)

