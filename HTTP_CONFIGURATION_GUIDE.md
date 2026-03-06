# Configuration HTTP robuste - Guide d'utilisation

## Améliorations apportées

Ce document décrit les améliorations apportées à la gestion des pools de connexion HTTP et de RestTemplate pour éviter les erreurs de type "connection closed" et "CancellationException".

### 1. **Augmentation des limites du pool de connexions**
- **Avant** : 100 connexions totales, 10 par route
- **Après** : 200 connexions totales, 20 par route
- **Raison** : Évite les timeouts lors de l'acquisition de connexions

### 2. **Timeouts mieux configurés**
- Connection Timeout : 30 secondes
- Socket Timeout : 60 secondes (lecture)
- Read Timeout : 5 minutes (opérations longues)
- Connection Request Timeout : 15 secondes (attente depuis le pool)

### 3. **Validation des connexions inactives**
- Validation après 5 secondes d'inactivité
- Éviction des connexions expirées et inactives
- Headers "keep-alive" pour maintenir les connexions

### 4. **BufferingClientHttpRequestFactory**
- Permet la relecture des réponses
- Améliore le debugging des erreurs
- Prévient les problèmes de fermeture prématurée

### 5. **RestTemplateUtil avec retry automatique**
- Retry automatique jusqu'à 3 fois en cas d'erreur transitoire
- Backoff exponentiel (500ms, 1000ms, 2000ms, 5000ms max)
- Détecte les erreurs transitoires (timeout, connection reset, etc.)

### 6. **Gestionnaire d'erreurs personnalisé**
- Logging détaillé des erreurs
- Distinction entre erreurs client et serveur
- Meilleure traçabilité des problèmes

---

## Comment utiliser RestTemplateUtil

### Option 1 : Utiliser directement RestTemplateUtil avec retry

```java
@Component
public class MonService {
    
    @Resource
    private RestTemplateUtil restTemplateUtil;
    
    public void appelerWebService() {
        // GET avec retry automatique
        String response = restTemplateUtil.getForObjectWithRetry(
            "http://example.com/api/data",
            String.class
        );
        
        // POST avec retry automatique
        String result = restTemplateUtil.postForObjectWithRetry(
            "http://example.com/api/save",
            monObjet,
            String.class
        );
        
        // Exchange (plus flexible) avec retry
        ResponseEntity<String> response = restTemplateUtil.exchangeWithRetry(
            "http://example.com/api/upload",
            HttpMethod.POST,
            new HttpEntity<>(contenu, headers),
            String.class
        );
    }
}
```

### Option 2 : Continuer à utiliser RestTemplate directement

Le RestTemplate est maintenant configuré avec :
- Pool de connexions optimisé
- Timeouts appropriés
- Gestionnaire d'erreurs personnalisé
- BufferingClientHttpRequestFactory pour les relecures de réponses

```java
@Component
public class MonService {
    
    @Resource
    private RestTemplate restTemplate;
    
    public void appelerWebService() {
        // Utilisation normale, mais plus robuste
        String response = restTemplate.getForObject(
            "http://example.com/api/data",
            String.class
        );
    }
}
```

---

## Configuration recommandée pour EncodingService

Exemple de migration pour `checkBeforeEncoding()` :

### Avant :
```java
try {
    restTemplate.postForObject(selectUrl, httpEntity, String.class);
} catch (HttpClientErrorException e) {
    // Gestion d'erreur
}
```

### Après (avec retry) :
```java
try {
    restTemplateUtil.postForObjectWithRetry(selectUrl, httpEntity, String.class);
} catch (RestClientException e) {
    // Gestion d'erreur
    log.error("Erreur lors de checkBeforeEncoding après retries", e);
    throw new SgcCheckException("SGC select error", e);
}
```

---

## Problèmes résolus

### CancellationException
- **Cause** : Timeout lors de l'acquisition d'une connexion du pool
- **Solution** : 
  - Augmentation du CONNECTION_REQUEST_TIMEOUT (15s)
  - Augmentation de la taille du pool (200 connexions)
  - Validation automatique des connexions inactives

### "connection closed"
- **Cause** : Connexion fermée par le serveur ou expirée
- **Solution** :
  - Headers "keep-alive" activés
  - Validation après 5 secondes d'inactivité
  - Éviction automatique des connexions inactives
  - Retry automatique en cas d'erreur transitoire

### Timeouts sur longues opérations
- **Cause** : Timeout trop court sur les socket reads
- **Solution** :
  - SOCKET_TIMEOUT : 60 secondes
  - READ_TIMEOUT : 5 minutes pour opérations longues

---

## Monitoring et diagnostique

### Logs pertinents à surveiller :
- `[PoolingHttpClientConnectionManager configuré avec :]` - Configuration appliquée
- `[Appel RestTemplate (tentative X):]` - Tentatives avec RestTemplateUtil
- `[Erreur transitoire sur...]` - Erreurs qui provoquent un retry
- `[Erreur non-transitoire...]` - Erreurs fatales

### Métriques recommandées :
- Nombre de tentatives de retry
- Temps de réponse moyen
- Nombre de connexions actives du pool
- Erreurs par type (timeout, connection reset, etc.)

---

## Configuration personnalisée

Si vous devez ajuster les timeouts selon vos besoins :

```java
// Dans SpringBeans.java, modifiez les constantes :
private static final int CONNECTION_TIMEOUT = 30000;      // Ajustez selon vos besoins
private static final int SOCKET_TIMEOUT = 60000;          // Augmentez si timeouts fréquents
private static final int CONNECTION_REQUEST_TIMEOUT = 15000; // Augmentez si pool saturé
private static final int MAX_TOTAL_CONNECTIONS = 200;     // Augmentez si trop de requêtes simultanées
```

---

## Dépendances requises

Le projet utilise déjà :
- `httpclient5` (5.6)
- `spring-web`
- `spring-context`

Aucune dépendance supplémentaire n'est nécessaire.

---

## Prochaines étapes

1. ✅ Configuration robuste du RestTemplate
2. ✅ RestTemplateUtil avec retry automatique
3. ✅ Gestionnaire d'erreurs personnalisé
4. **À faire** : Migrer les appels critiques vers RestTemplateUtil
5. **À faire** : Ajouter du monitoring des retries
6. **À faire** : Tester en production avec charge élevée

