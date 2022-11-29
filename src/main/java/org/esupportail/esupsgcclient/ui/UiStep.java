package org.esupportail.esupsgcclient.ui;

public enum UiStep {

        long_poll("Récupération carte depuis ESUP-SGC"),
        bmp_black("Récupération BMP Noir et Blanc ESUP-SGC"),
        bmp_color("Récupération BMP Couleur ESUP-SGC"),
        qrcode_read("Lecture du QRCode"),
        csn_read("Lecture du CSN"),
        sgc_select("Sélection dans le SGC"),
        printer_nfc("Positionnement de la carte sur lecteur NFC"),
        encode("Encodage de la carte"),
        encode_cnous("Encodage CNOUS"),
        send_csv("Envoi du CSV"),
        printer_insert("Insertion Carte dans l'imprimante"),
        printer_color("Impression Couleur"),
        printer_black("Impression Noir et Blanc"),
        printer_overlay("Impression Overlay"),
        printer_print("Impression"),
        printer_eject("Carte éjectée");

        private String name;

        private UiStep(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
