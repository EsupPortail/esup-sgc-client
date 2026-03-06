package org.esupportail.esupsgcclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * Gestionnaire d'erreurs personnalisé pour RestTemplate.
 * Fournit une gestion robuste des erreurs HTTP et des problèmes de connexion.
 */
public class RestTemplateErrorHandler implements ResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        int status = response.getRawStatusCode();
        // Considère 5xx et 4xx comme des erreurs
        return status >= 400;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        int status = response.getRawStatusCode();
        String message;

        try {
            message = new String(response.getBody().readAllBytes());
        } catch (Exception e) {
            message = "Unable to read error message";
        }

        if (status >= 500) {
            log.error("HTTP Server Error ({}): {}", status, message);
            throw new HttpServerException("HTTP " + status + ": " + message, status, message);
        } else if (status >= 400) {
            log.warn("HTTP Client Error ({}): {}", status, message);
            throw new HttpClientException("HTTP " + status + ": " + message, status, message);
        }
    }

    /**
     * Exception pour les erreurs serveur HTTP
     */
    public static class HttpServerException extends IOException {
        private final int statusCode;
        private final String responseBody;

        public HttpServerException(String message, int statusCode, String responseBody) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }

    /**
     * Exception pour les erreurs client HTTP
     */
    public static class HttpClientException extends IOException {
        private final int statusCode;
        private final String responseBody;

        public HttpClientException(String message, int statusCode, String responseBody) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}

