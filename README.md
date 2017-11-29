ESUP-SGC-CLIENT
===============

Esup-sgc-client est l'application permettant d'encoder les cartes Mifare Desfire EV1 dans le cadre du Système de gestion de carte Esup-sgc. 
Le client s'appuit aussi sur la platefome Esup-nfc-tag qui calcule les commandes (APDU) à transmettre à la carte.

L'application est packagée sous la forme d'un jar comprenant les dépendances : esupsgcclient-1.0-SNAPSHOT-jar-with-dependencies.jar
Le jar destinée à être lancé en mode java web start (jws) depuis l'application web Esup-sgc

## Fonctionalités

1 - L'application lit le QR code imprimé sur la carte à encoder qui correspond à l'identifiant du futur propriétaire de la carte.
2 - Demande la selection dans esup-sgc de l'individu à encoder
3 - L'application recupère les commandes à executer sur la carte via esup-nfc-tag-server
4 - Validation de l'encodage et activation de la carte

## Environnement

### Logiciel

L'application est prévue pour tourner sur du java 8. Elle est lancée en JWS il faudra donc autoriser l'application qui va demander touts les droits sur la machine cliente.

### Materiel

L'application nécessite :
- une webcam gérant la resolution VGA (640x480)
- un lecteur de carte compatible PC/SC

La webcam est placée pour filmer le lecteur de carte (procéder à la mise au point si besoin).
Lorsqu'une carte est posée sur le lecteur de carte, la webcam detecte le QR code et la procédure d'encodage démarre

## Compilation esup-sgc-client

Dans le dossier esup-sgc-client executer : mvn clean package

Copier le fichier esupsgcclient-1.0-SNAPSHOT-jar-with-dependencies.jar à la racine de votre webapp esup-sgc
