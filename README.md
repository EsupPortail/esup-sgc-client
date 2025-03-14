ESUP-SGC-CLIENT
===============

  * [Technologies](#technologies)
  * [Environnement de développement](#environnement-de-développement)
  * [SceneBuilder](#scenebuilder)
  * [Dépendances techniques](#dépendances-techniques)
    + [modules maven](#modules-maven)
    + [evolis](#evolis)
    + [zebra](#zebra)
      - [support sous Windows](#support-sous-windows)
      - [support sous Linux](#support-sous-linux)
    + [esup-nfc-tag et esup-sgc de démonstration](#esup-nfc-tag-et-esup-sgc-de-démonstration)
  * [Copie d'écran](#copie-décran)

Pour une documentation fonctionnelle de l'outil et sa mise en oeuvre, merci de vous référer 
à la documentation ici du wiki ESUP-Portail, notamment ici :
 * [projet global esup-sgc](https://www.esup-portail.org/wiki/display/SGC)
 * [page wiki ESUP spécifiique à esup-sgc-client](https://www.esup-portail.org/wiki/pages/viewpage.action?pageId=1221722115)

Dans ces pages, on indique notamment que l'installation de cet outil en production est facilitée par un installateur 
qui peut lui-même être généré depuis un simple formulaire web en ligne via [esup-sgc-client-installer](https://github.com/EsupPortail/esup-sgc-client-installer)


## Technologies

esup-sgc-client est une application java/spring/javafx utilisant maven pour gérer la compilation 
et les dépendances des librairies utilisées.

Pour fonctionner, il requiert un openjdk 23 (ou supérieur) et openjfx.

Il peut fonctionner sur les systèmes d'exploitation Windows, Linux et macOS.

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
--module-path /usr/local/javafx-sdk-23/lib/ --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.media,javafx.graphics,javafx.swing,javafx.web
````

Suivant le certificat utilisé par vos serveurs esup-sgc/esup-nfc-tag, et la version de java utilisée, vous devrez également rajouter dans les options java
````
-Djdk.tls.client.protocols=TLSv1.2
````

Pour activer le profile/module evolis-sdk dans l'IDE, vous pouvez ajouter le "classpath of module" `-cp esupsgclient-evolis-sdk` .

_Sous intelliJ, pour pouvoir sélectionner `-cp esupsgclient-evolis-sdk` vous devez au préalable activer le profile evolis-sdk dans la fenêtre maven (et recharger les projets maven)._

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
  * une imprimante evolis ou zebra
  * un serveur esup-sgc


### modules maven

Des sous-modules maven peuvent être utilisés pour ajouter le support aux imprimantes evolis ou zebra.
Pour ajouter le module evolis-sdk, vous pouvez ajouter `-P evolis-sdk` à vos commandes maven.
Pour zebra, ajoutez `-P zebra` à vos commandes maven

Ainsi
````
mvn -P evolis-sdk clean package
````
permet de récupérer le client esup-sg-client avec le module esupsgcclient-evolis-sdk ici : `esupsgcclient-assembly/target/esup-sgc-client-final.jar`

De même, pour lancer directement cette application avec esupsgcclient-evolis-sdk de chargé via maven, vous pouvez lancer :
````
mvn -P evolis-sdk clean javafx:run
````

Depuis votre IDE (intellij idea par exemple), vous pouvez travailler/lancer l'application en spécifiant le module via `-cp esupsgcclient-evolis-sdk` ;
la classe principale à lancer restant `org.esupportail.esupsgcclient.EsupSgcClientApplication`.

### evolis

Il vous faut installer le driver de votre encodeur NFC intégré à votre Evolis (Primacy 1 ou 2) 
(Si vous avez opté pour un encodeur "SpringCard CrazyWriter" par exemple, vous trouverez le driver depuis https://www.springcard.com/en/download/drivers : "PC/SC Driver for USB couplers" / fichier sd16055-2104.exe).

Ensuite, il vous suffit d'installer le driver de votre imprimante evolis.

Les drivers sont disponibles aussi bien sous Windows, Linux et macOS. Aussi votre imprimante Evolis (Primacy 1, Primacy 2, ...) peut fonctionner aussi bien via un esup-sgc-client depuis linux, windows ou mac.

esup-sgc-client dialogue directement avec l'imprimante via le SDK 3.
Ce SDK permet de retrouver simplement et automatiquement l'imprimante evolis connectée, le type de ruban n'est pas non plus à configurer, ...

Théoriquement, esup-sgc-client doit supporter les autres imprimantes evolis que Primacy 1 et 2 via le SDK3 toujours.

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

Le répertoire dans lequel le SDK est ainsi installé (et sa version) doit être positionné dans esupsgcclient-zebra/pom.xml 
```
    <zebra.link_os_sdk.lib>/opt/LinkOS_SDK_PC-Card_Java_v3.01.86/lib</zebra.link_os_sdk.lib>
    <zebra.link_os_sdk.version>v3.01.86</zebra.link_os_sdk.version>
```

Une fois le SDK téléchargé et configuré dans ce fichier, vous devez taper la commande suivante pour le faire connaitre à maven : 
```
mvn -P zebra initialize
```

Comme pour les evolis, le lecteur NFC ne fonctionne que via USB : le client esup-sgc-client doit donc être installé sur le poste (windows, linux ou macOS) connecté en USB à l'imprimante.

Pour la phase d'encodage, une fois l'ordre donnée de positionner la carte au niveau de l'encodeur, esup-sgc-client dialogue directement avec l'encodeur NFC en pc/sc.

Dans le fichier de propriétés src/main/resources/esupsgcclient.properties, suivant votre imprimante (et son firmware), vous devrez éventuellement adapter la propriété printerZebraEncoderType

Si la ZC300 attend a priori 'other' pour spécifier le lecteur NFC USB intégré à celle-ci, la ZXP3 attend par exemple 'MIFARE'.

Notez que la Zebra ZC 300 (avec l'encodeur cité) fonctionne aussi bien sous windows, macOS et linux.

#### zxp3 - difficultés rencontrées

Concernant la ZXP3, le SDK java fourni par Zebra pose nativement un problème avec les demi-panneaux, un contournement est proposé dans esup-sgc-client pour que cela fonctionne, 
pour ce faire il suffit de positionner printerZebraHackZxp3HalfBug à true dans esupsgcclient.properties (ou via une variable d'environnement).

printerZebraHackZxp3HalfBug à true permet également de ne pas appeler la méthode close de la DLL Zebra qui provoque un crash JVM sous windows (ZXP3). 

Mettre printerZebraRotation à true (false par défaut) permet de retourner la carte avant impression (il faut alors adapter le positionnement de la carte dans l'imprimante).
Ce paramètre est utile pour la Zebra ZXP3 avec les demi-panneaux : sur ZXP3 c'est au SDK de gérer le positionnement du demi-pannaeu (et non à l'imprimante comme sur ZXP7 ou ZC300), et celui-ci
ne le fait pas correctement ; en faisant en sorte que la partie couleur soit sur la gauche de la carte lors de l'impression (au début), cela facilite le calcul du décalage par le SDK.
-> à utiliser sur ZXP3 avec les demi-panneaux si vous avez des problèmes de décalage (et photo/logo couleur flouté). 

printerZebraHackZxpNfcPower permet quant à lui d'inverser la commande d'alimentation du lecteur NFC ; 
cela est nécessaire si vous avez mis à jour le firmware du lecteur NFC fourni avec la ZXP3 avec le firmware officiel (SDI010 d'identiv) ;
ce que l'on vous déconseille de faire ; cela permet de le faire fonctionner sous linux le lecteur NFC mais de manière instable (de nos tests, des erreurs pc/sc peuvent survenir), de plus la mise à jour du firmware est **irréversible**
car le firmware initial est celui de zebra qui est non disponible au téléchargement.

Aussi la ZXP3, qui est un modèle en fin de vie et dont les consommables vont être de plus en plus difficiles à trouver, n'est pas recommandée pour l'impression avec esup-sgc-client.
Elle peut par contre vous servir de robot de badgeage (pour noter les cartes comme détruites par exemple) ou encore comme robot d'encodage (après une impression en masse, cf la documentation esup-sgc à ce sujet).


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

Vous pouvez aussi tester esup-sgc-client depuis [la machine virtuelle de démonstration](https://www.esup-portail.org/wiki/display/SGC/VM+ESUP-SGC).

## Copie d'écran

![esup-sgc-client au 10/12/2022](src/etc/esup-sgc-client-10122022.png)

