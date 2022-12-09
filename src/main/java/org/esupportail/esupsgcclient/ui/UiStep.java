package org.esupportail.esupsgcclient.ui;

public enum UiStep {

        long_poll("Récupération carte depuis ESUP-SGC"),
        bmp_black("Récupération BMP Noir et Blanc ESUP-SGC"),
        bmp_color("Récupération BMP Couleur ESUP-SGC"),
        qrcode_read("Lecture du QRCode"),
        csn_read("Lecture du CSN"),
        sgc_select("Sélection dans le SGC"),
        printer_nfc("Positionnement de la carte sur lecteur NFC"),
        encode("Encodage/Badgeage de la carte"),
        encode_cnous("Encodage CNOUS"),
        send_csv("Envoi du CSV"),
        printer_color("Envoi Panneau Couleur"),
        printer_black("Envoi Panneau Noir et Blanc"),
        printer_overlay("Envoi Panneau Overlay"),
        printer_print("Impression"),
        sgc_ok("Noté comme encodé dans ESUP-SGC");

        private String name;

        private UiStep(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
