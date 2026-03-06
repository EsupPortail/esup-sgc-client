package org.esupportail.esupsgcclient.service;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration Spring pour les beans de l'application.
 * Gère la création du RestTemplate avec une gestion robuste des pools de connexion HTTP.
 *
 * Cette configuration résout les problèmes de :
 * - CancellationException lors de l'acquisition de connexions
 * - "connection closed" et timeouts
 * - Épuisement du pool de connexions
 */
@Configuration
public class SpringBeans {

    private static final Logger log = LoggerFactory.getLogger(SpringBeans.class);

    // Configuration des délais d'attente (en millisecondes)
    private static final int CONNECTION_TIMEOUT = 30000;      // 30 secondes
    private static final int SOCKET_TIMEOUT = 60000;          // 60 secondes (socket read)
    private static final int READ_TIMEOUT = 300000;           // 5 minutes (pour longues opérations)
    private static final int CONNECTION_REQUEST_TIMEOUT = 15000; // 15 secondes (attente d'une connexion du pool)

    // Configuration du pool de connexions
    private static final int MAX_TOTAL_CONNECTIONS = 200;     // Augmenté pour gérer la charge
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;  // Augmenté pour les routes critiques
    private static final long CONNECTION_IDLE_TIMEOUT = 60000; // 60 secondes
    private static final long CONNECTION_IDLE_EVICTION = 30000; // Éviction tous les 30 secondes

    // Validation des connexions
    private static final long VALIDATE_AFTER_INACTIVITY = 5000; // Validation après 5 secondes d'inactivité

    /**
     * Crée un gestionnaire de pool de connexions HTTP optimisé pour éviter les erreurs
     * de connexion fermée ("connection closed").
     *
     * @return PoolingHttpClientConnectionManager configuré
     */
    @Bean
    public PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager() {
        PoolingHttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager();

        // Configuration du nombre maximum de connexions
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // Configuration des timeouts au niveau du gestionnaire de connexions
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setSocketTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
            .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
            .setValidateAfterInactivity(Timeout.ofMilliseconds(VALIDATE_AFTER_INACTIVITY))
            .build();

        connectionManager.setDefaultConnectionConfig(connectionConfig);

        log.info("PoolingHttpClientConnectionManager configuré avec :");
        log.info("  - Max total: " + MAX_TOTAL_CONNECTIONS);
        log.info("  - Max per route: " + MAX_CONNECTIONS_PER_ROUTE);
        log.info("  - Socket timeout: " + SOCKET_TIMEOUT + "ms");
        log.info("  - Connection timeout: " + CONNECTION_TIMEOUT + "ms");
        log.info("  - Validate after inactivity: " + VALIDATE_AFTER_INACTIVITY + "ms");

        return connectionManager;
    }

    /**
     * Crée un client HTTP avec gestion des connexions et éviction des connexions inactives.
     * Configure également les headers HTTP pour éviter les fermetures de connexion prématurées.
     *
     * @param connectionManager le gestionnaire de pool de connexions
     * @return HttpClient configuré
     */
    @Bean
    public HttpClient getHttpClient(PoolingHttpClientConnectionManager connectionManager) {
        // Configuration des requêtes avec timeouts
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
            .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_REQUEST_TIMEOUT))
            .build();

        // Headers pour maintenir la connexion
        Header[] defaultHeaders = new Header[]{
            new BasicHeader("Connection", "keep-alive"),
            new BasicHeader("User-Agent", "esup-sgc-client/5.5")
        };

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setDefaultHeaders(Arrays.asList(defaultHeaders))
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofMilliseconds(CONNECTION_IDLE_TIMEOUT))
            .disableAutomaticRetries()
            .build();
    }

    /**
     * Crée la factory de requêtes HTTP avec timeouts appropriés.
     * Utilise BufferingClientHttpRequestFactory pour permettre la lecture multiple de la réponse.
     *
     * @param httpClient le client HTTP configuré
     * @return HttpComponentsClientHttpRequestFactory enveloppée dans BufferingClientHttpRequestFactory
     */
    @Bean
    public BufferingClientHttpRequestFactory getClientHttpRequestFactory(HttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory httpComponentsFactory =
            new HttpComponentsClientHttpRequestFactory(httpClient);

        // Timeouts de fallback (bien que le HttpClient aura la priorité)
        httpComponentsFactory.setConnectTimeout(CONNECTION_TIMEOUT);
        httpComponentsFactory.setReadTimeout(READ_TIMEOUT);

        // BufferingClientHttpRequestFactory permet de lire la réponse plusieurs fois
        return new BufferingClientHttpRequestFactory(httpComponentsFactory);
    }

    /**
     * Crée le RestTemplate avec la factory de requêtes HTTP configurée.
     *
     * @param httpRequestFactory la factory de requêtes HTTP
     * @return RestTemplate configuré
     */
    @Bean
    public RestTemplate getRestTemplate(BufferingClientHttpRequestFactory httpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        restTemplate.setErrorHandler(new RestTemplateErrorHandler());
        return restTemplate;
    }

    /**
     * Crée l'utilitaire RestTemplate avec gestion automatique des retries.
     *
     * @param restTemplate le RestTemplate
     * @return RestTemplateUtil configuré
     */
    @Bean
    public RestTemplateUtil getRestTemplateUtil(RestTemplate restTemplate) {
        return new RestTemplateUtil(restTemplate);
    }

    /**
     * Crée un pool de threads pour les tâches SGC.
     *
     * @return ThreadPoolExecutor configuré
     */
    @Bean
    public ThreadPoolExecutor getSgcTaskExecutor() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }

}
