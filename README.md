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

Pour fonctionner, il requiert un openjdk 8 (ou supérieur) et openjfx

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

Pour activer le profile/module evolis dans l'IDE, vous pouvez ajouter le "classpath of module" `-cp esupsgclient-evolis` .

_Sous intelliJ, pour pouvoir sélectionner `-cp esupsgclient-evolis` vous devez au préalable activer le profile evolis dans la fenêtre maven (et recharger les projets maven)._

En optant pour `-cp esupsgclient-core` vous aurez le client sans module/profile supplémentaire.

Pour le profile/module zebra, vous pouvez ajouter le "classpath of module" `-cp esupsgclient-zebra` .

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
  * un lecteur NFC USB permettant de passer des APDUs (notamment Mifare DESFIRE) via PC/SC, celui-ci peut correspondre au lecteur NFC de l'imprimante Evolis Primacy (1 ou 2) ou Zebra
* optionnel
  * une webcam 
  * une imprimante evolis primacy 1 ou 2 (ou plus exactement Evolis Premium Suite 1 ou 2) ou zebra
  * un serveur esup-sgc


### modules maven

Des sous-modules maven peuvent être utilisés pour ajouter le support aux imprimantes evolis ou zebra.
Pour ajouter le module evolis, vous pouvez ajouter `-P evolis` à vos commandes maven.
Pour zebra, ajoutez `-P zebra` à vos commandes maven

Ainsi
````
mvn -P evolis clean package
````
permet de récupérer le client esup-sg-client avec le module esupsgcclient-evolis ici : `esupsgcclient-assembly/target/esup-sgc-client-final.jar`

De même, pour lancer directement cette application avec esupsgcclient-evolis de chargé via maven, vous pouvez lancer :
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

Le service et donc le PC windows hôte doit donc écouter en TCP sur le port 18000 en local, c'est par ce biais qu'esup-sgc-client dialogue avec l'imprimante.
En cas de problème d'écoute sur le port 18000, pensez à vérifier/adapter un éventuel firewall.

Le fichier de configuration d'esup-sgc-client donné dans [src/main/resources/esupsgcclient.properties](src/main/resources/esupsgcclient.properties) doit reprendre ce même numéro de port.

Notez que la configuration printerEvolisSet vous permet de configurer le type de ruban utilisé, 
par défaut on propose une configuration proposant l'usage d'un ruban couleur demi-panneau (RC_YMCKOS), pour un ruban couleur plein panneau il faudra positionner RC_YMCKO

### evolis primacy 1

La mise en place pour Evolis Primacy 1 (par rapport à Primacy 2) est très similaire, il vous faudra cependant installer non pas "Evolis Premium Suite 2" mais "Evolis Premium Suite".
Le fichier de configuration à modifier est C:\Program Files\Evolis Card Printer\Evolis Premium Suite\ESPFSvc.properties pour activer le support de la communication par TCP (port 18000) :
```
ESPFServerManager.enabletcpatstart = true
```

Au niveau d'esup-sgc-client, il faudra spécifier dans src/main/resources/esupsgcclient.properties :
```
printerDeviceName = Evolis Primacy
```

### autres imprimantes evolis

Théoriquement, esup-sgc-client doit supporter les autres imprimantes evolis supportant "Evolis Premium Suite" (1 ou 2), il faudra alors adapter la propriété printerDeviceName en conséquence.

### simulation de evolis

Pour le développement, on peut aussi se contenter de 'simuler' l'API de "Evolis Premium Suite" 
Pour ce faire, il vous suffit de lancer le script python (python2 ou python3) [src/etc/dummyEvolisPrinterCenter.py](src/etc/dummyEvolisPrinterCenter.py)
````
python3 dummyEvolisPrinterCenter.py
````

Ce script, basique, ne fait que répondre ``{"id":"1","jsonrpc":"2.0","result":"OK"}`` à toute requête émanant de esup-sgc-client.

Il est donc loin de 'simuler' à proprement parler les interactions avec evolis primacy 2; mais ça reste tout à fait suffisant pour une grand part du développement.

### zebra

En plus du driver Zebra récupérable depuis https://www.zebra.com/fr/fr/support-downloads/printers/card/zc300.html , il vous faut installer le driver de votre encodeur NFC intégré à votre Zebra.
Nous avons développé et donc validé le bon fonctionnement de l'imprimante Zebra ZC 300 dotée d'un encodeur ELATEC GmbH TWN4/B1.50/NPF4.51/S1SC1.60 (vous pouvez récupérer le nom de l'encodeur depuis le gestionnaire de périphériques windows ou plus simplement via un `lsusb` sous linux).

Pour que cet encodeur fonctionne correctement avec la Zebra ZC 300, il nous a fallu mettre à jour son firmware avec la version du firmware proposé dans le "DevPack 4.51" que l'on peut obtenir depuis https://www.elatec-rfid.com/int/twn4-dev-pack (en indiquant son mail).
Sans cette mise à jour de firmware (notre zebra ZC 300 reçue en février 2023 proposait un firmware ancien), l'encodeur était vu comme deux lecteurs NFC et n'était pas capable de lire les cartes Mifare Desfire.
Pour effectuer cette mise à jour du firmware, depuis un windows, et une fois récupéré et dézippé votre fichier TWN4DevPack45.zip, vous pouvez taper en ligne de commande : 
```
TWN4DevPack451\Tools\flash.exe --noprog USB TWN4DevPack451\Firmware\TWN4_xPx451_S1SC160_Multi_CCID_1Slot_Standard.bix
```
Cela vous permet de récupérer quelques informations, la mise à jour du firmware effective étant faite par la commande : 
```
TWN4DevPack451\Tools\flash.exe USB TWN4DevPack451\Firmware\TWN4_xPx451_S1SC160_Multi_CCID_1Slot_Standard.bix
```

En plus du driver Zebra et du firmware (du lecteur NFC, celui de la Zebra étant sans doute déjà jour) à mettre éventuellement à jour, il vous faut également télécharger le SDK Zebra depuis https://www.zebra.com/fr/fr/support-downloads/printer-software/developer-tools/card-sdk.html
Celui-ci a pour nom "LINK-OS MULTIPLATFORM SDK FOR CARD PRINTERS", nous avons utilisé la version v2.14.5198 de ce SDK.

Le répertoire dans lequel le SDK est ainsi installé (et sa version) doit être positionné dans esupsgcclient-zebra/pom.xml 
```
    <zebra.link_os_sdk.lib>/opt/link_os_sdk/PC-Card/v2.14.5198/lib</zebra.link_os_sdk.lib>
    <zebra.link_os_sdk.version>v2.14.5198</zebra.link_os_sdk.version>
```

Une fois le SDK téléchargé et configuré dans ce fichier, vous devez taper la commande suivante pour le faire connaitre à maven : 
```
mvn -P zebra initialize
```

Sous windows, dans les variables d'environnement, ajoutez également ce répertoire pointant vers la librairie (et DLL) du SDK Zebra dans le PATH.
N'oubliez pas également que sous windows, les DLL zebra actuelles ne fonctionnent que depuis un JDK-8 (test réalisé avec le JDK 1.8.0_333 d'Oracle notamment).

Notez que la Zebra ZC 300 (avec l'encodeur cité) fonctionne aussi bien sous windows que sous linux. Sous linux, le dialogue PC/SC avec l'encodeur est réalisé grâce à pcscd avec les pilotes proposés dans libccid.
Sous linux, le dialogue avec l'imprimante nécessite que l'utilisateur ait les droits de lecture/écriture sur le fichier de périphérique (sous peine d'une erreur de type "USB error 3: Unable to open device: Access denied (insufficient permissions)").
Exemple pour debian : un ls -l sur le /dev nous indique qu'il suffit pour ce faire de mettre l'utilisateur dans les groupes dialout et lp.
```
adduser vincent dialout
adduser vincent lp
```

Comme pour les evolis, le lecteur NFC ne fonctionne que via USB : le client esup-sgc-client doit donc être installé sur le poste (windows ou linux) connecté en USB à l'imprimante.

Pour la phase d'encodage, une fois l'ordre donnée de positionner la carte au niveau de l'encodeur, esup-sgc-client dialogue directement avec l'encodeur NFC en pc/sc.

Dans le fichier de propriétés src/main/resources/esupsgcclient.properties, suivant votre imprimante (et son firmware), vous devrez éventuellement adapter la propriété printerZebraEncoderType

Si la ZC300 attend a priori 'other' pour sépcifier le lecteur NFC USB intégré à celle-ci, la ZXP3 attend par exemple 'MIFARE'.

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

