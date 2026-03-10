package org.esupportail.esupsgcclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration Spring pour les beans de l'application.
 * Configuration simplifiée sans pool de connexions HTTP.
 */
@Configuration
public class SpringBeans {

    private static final Logger log = LoggerFactory.getLogger(SpringBeans.class);

    // Configuration des délais d'attente (en millisecondes)
    private static final int CONNECTION_TIMEOUT = 30000;      // 30 secondes
    private static final int READ_TIMEOUT = 300000;           // 5 minutes (pour longues opérations)

    /**
     * Crée le RestTemplate avec une configuration simple sans pool de connexions.
     * Chaque requête utilise sa propre connexion qui est fermée après utilisation.
     *
     * @return RestTemplate configuré
     */
    @Bean
    public RestTemplate getRestTemplate() {
        // Configuration simple sans pool de connexions
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECTION_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        // BufferingClientHttpRequestFactory permet de lire la réponse plusieurs fois si nécessaire
        BufferingClientHttpRequestFactory bufferingFactory =
            new BufferingClientHttpRequestFactory(requestFactory);

        RestTemplate restTemplate = new RestTemplate(bufferingFactory);
        // Pas besoin de setErrorHandler : Spring utilise DefaultResponseErrorHandler par défaut
        // qui lance HttpClientErrorException et HttpServerErrorException (attendues par le code)

        log.info("RestTemplate configuré avec :");
        log.info("  - Connection timeout: " + CONNECTION_TIMEOUT + "ms");
        log.info("  - Read timeout: " + READ_TIMEOUT + "ms");
        log.info("  - Mode: Connexions simples (sans pool)");

        return restTemplate;
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
