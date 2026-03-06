package org.esupportail.esupsgcclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Utilitaire pour les appels RestTemplate avec gestion des erreurs et retry automatique.
 * Résout les problèmes de :
 * - CancellationException
 * - Connection closed
 * - Timeouts
 * - Épuisement du pool de connexions
 */
public class RestTemplateUtil {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateUtil.class);

    // Configuration des retries
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;
    private static final long MAX_RETRY_DELAY_MS = 5000;

    private final RestTemplate restTemplate;

    public RestTemplateUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Effectue une requête GET avec retry automatique en cas d'erreur transitoire.
     *
     * @param url l'URL cible
     * @param responseType le type de réponse attendu
     * @param <T> le type de retour
     * @return la réponse
     * @throws RestClientException si l'erreur persiste après tous les retries
     */
    public <T> T getForObjectWithRetry(String url, Class<T> responseType) {
        return executeWithRetry(
            () -> restTemplate.getForObject(url, responseType),
            "GET " + url
        );
    }

    /**
     * Effectue une requête POST avec retry automatique en cas d'erreur transitoire.
     *
     * @param url l'URL cible
     * @param request le corps de la requête
     * @param responseType le type de réponse attendu
     * @param <T> le type de retour
     * @return la réponse
     * @throws RestClientException si l'erreur persiste après tous les retries
     */
    public <T> T postForObjectWithRetry(String url, Object request, Class<T> responseType) {
        return executeWithRetry(
            () -> restTemplate.postForObject(url, request, responseType),
            "POST " + url
        );
    }

    /**
     * Effectue une requête exchange avec retry automatique en cas d'erreur transitoire.
     *
     * @param url l'URL cible
     * @param method la méthode HTTP
     * @param requestEntity l'entité de requête
     * @param responseType le type de réponse attendu
     * @param <T> le type de retour
     * @return la réponse
     * @throws RestClientException si l'erreur persiste après tous les retries
     */
    public <T> ResponseEntity<T> exchangeWithRetry(String url, HttpMethod method,
                                                    HttpEntity<?> requestEntity, Class<T> responseType) {
        return executeWithRetry(
            () -> restTemplate.exchange(url, method, requestEntity, responseType),
            method.toString() + " " + url
        );
    }

    /**
     * Exécute un appel avec retry automatique.
     *
     * @param call la fonction à exécuter
     * @param description la description de l'appel (pour le logging)
     * @param <T> le type de retour
     * @return le résultat
     * @throws RestClientException si l'erreur persiste après tous les retries
     */
    private <T> T executeWithRetry(RestCall<T> call, String description) {
        int attempt = 0;
        long delay = RETRY_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                log.debug("Appel RestTemplate (tentative {}): {}", attempt + 1, description);
                return call.execute();

            } catch (ResourceAccessException e) {
                // Erreur de connexion, timeout, ou autre erreur transitoire
                attempt++;
                if (isTransientError(e)) {
                    if (attempt < MAX_RETRIES) {
                        log.warn("Erreur transitoire sur {} (tentative {}), retry dans {}ms: {}",
                            description, attempt, delay, e.getMessage());
                        sleep(delay);
                        delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
                    } else {
                        log.error("Erreur transitoire persiste après {} tentatives sur {}: {}",
                            MAX_RETRIES, description, e.getMessage(), e);
                        throw new RestClientException("Erreur de connexion après " + MAX_RETRIES +
                            " tentatives: " + e.getMessage(), e);
                    }
                } else {
                    log.error("Erreur non-transitoire sur {}: {}", description, e.getMessage(), e);
                    throw e;
                }

            } catch (Exception e) {
                // Erreur non-connectivité
                log.error("Erreur lors de l'appel {}: {}", description, e.getMessage(), e);
                throw new RestClientException("Erreur lors de " + description + ": " + e.getMessage(), e);
            }
        }

        throw new RestClientException("Impossible d'exécuter " + description + " après " + MAX_RETRIES + " tentatives");
    }

    /**
     * Détermine si une erreur est transitoire (peut être retentée).
     *
     * @param e l'exception
     * @return true si l'erreur est transitoire
     */
    private boolean isTransientError(ResourceAccessException e) {
        Throwable cause = e.getCause();

        // Erreurs de timeout
        if (cause instanceof SocketTimeoutException) {
            return true;
        }

        // Erreurs de connexion fermée
        if (cause instanceof SocketException) {
            String message = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            return message.contains("closed") || message.contains("reset") ||
                   message.contains("connection") || message.contains("timeout");
        }

        // CancellationException et autres erreurs de pool
        if (cause instanceof java.util.concurrent.CancellationException) {
            return true;
        }

        // Erreurs de connexion génériques
        String exceptionMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return exceptionMessage.contains("connection") || exceptionMessage.contains("timeout") ||
               exceptionMessage.contains("closed") || exceptionMessage.contains("reset");
    }

    /**
     * Dort pour la durée spécifiée.
     *
     * @param millis les millisecondes
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.debug("Sleep interrompu", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Interface fonctionnelle pour les appels RestTemplate.
     *
     * @param <T> le type de retour
     */
    @FunctionalInterface
    private interface RestCall<T> {
        T execute() throws Exception;
    }
}

