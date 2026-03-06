package org.esupportail.esupsgcclient.service;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration Spring pour les beans de l'application.
 * Gère la création du RestTemplate avec une gestion robuste des pools de connexion HTTP.
 */
@Configuration
public class SpringBeans {

    // Configuration des délais d'attente (en millisecondes)
    private static final int CONNECTION_TIMEOUT = 30000;      // 30 secondes
    private static final int READ_TIMEOUT = 300000;           // 5 minutes

    // Configuration du pool de connexions
    private static final int MAX_TOTAL_CONNECTIONS = 100;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 10;
    private static final long CONNECTION_IDLE_TIMEOUT = 60000; // 60 secondes

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
            .setSocketTimeout(Timeout.ofMilliseconds(READ_TIMEOUT))
            .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
            .setValidateAfterInactivity(Timeout.ofSeconds(10))
            .build();

        connectionManager.setDefaultConnectionConfig(connectionConfig);

        return connectionManager;
    }

    /**
     * Crée un client HTTP avec gestion des connexions et éviction des connexions inactives.
     *
     * @param connectionManager le gestionnaire de pool de connexions
     * @return HttpClient configuré
     */
    @Bean
    public HttpClient getHttpClient(PoolingHttpClientConnectionManager connectionManager) {
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofMilliseconds(CONNECTION_IDLE_TIMEOUT))
            .build();
    }

    /**
     * Crée la factory de requêtes HTTP avec timeouts appropriés.
     *
     * @param httpClient le client HTTP configuré
     * @return HttpComponentsClientHttpRequestFactory
     */
    @Bean
    public HttpComponentsClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * Crée le RestTemplate avec la factory de requêtes HTTP configurée.
     *
     * @param httpRequestFactory la factory de requêtes HTTP
     * @return RestTemplate configuré
     */
    @Bean
    public RestTemplate getRestTemplate(HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return new RestTemplate(httpRequestFactory);
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
