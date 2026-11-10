# TP3 REST - Microservices Calculatrice avec Spring Boot

## 1. Présentation

Ce dépôt contient deux microservices Spring Boot 4.0.9 fonctionnant avec Java 21 :

| Service | Responsabilité | Port |
|---|---|---:|
| `calcul-service` | Exécuter les quatre opérations arithmétiques | 8081 |
| `historique-service` | Appeler `calcul-service` et conserver les opérations en mémoire | 8082 |

Les appels sont REST et utilisent JSON pour les objets détaillés et `text/plain` pour les résultats simples.

## 2. Construction et démarrage

Depuis la racine du dépôt :

```powershell
.\mvnw.cmd clean package
java -jar target\calcul-service-0.0.1-SNAPSHOT.jar
```

Dans un autre terminal :

```powershell
.\mvnw.cmd -f historique-service\pom.xml clean package
java -jar historique-service\target\historique-service-0.0.1-SNAPSHOT.jar
```

Le calcul-service doit être démarré avant de créer une opération dans historique-service. La configuration de ce dernier est dans `historique-service/src/main/resources/application.properties`.

## 3. API du calcul-service

| Requête | Résultat |
|---|---|
| `GET http://localhost:8081/calcul/somme?a=8&b=3` | `11` |
| `GET http://localhost:8081/calcul/soustraction?a=8&b=3` | `5` |
| `GET http://localhost:8081/calcul/produit?a=8&b=3` | `24` |
| `GET http://localhost:8081/calcul/produit/8/3` | `24` |
| `GET http://localhost:8081/calcul/division?a=8&b=2` | `4.0` |
| `GET http://localhost:8081/calcul/division?a=8&b=0` | `400 Bad Request`, `Erreur : division par 0 impossible` |
| `GET http://localhost:8081/calcul/somme/detail?a=8&b=3` | `{"operation":"somme","a":8,"b":3,"resultat":11.0}` |
| `GET http://localhost:8081/calcul/division/detail?a=8&b=2` | `{"operation":"division","a":8,"b":2,"resultat":4.0}` |
| `GET http://localhost:8081/calcul/division/detail?a=8&b=0` | `400 Bad Request` |

Les méthodes détaillées utilisent la classe `Resultat` et Jackson. La division vérifie le diviseur avant d'appeler `Calculatrice`.

## 4. Questions de conception - Partie A

### A.4.1 - Appel HTTP ou appel direct en mémoire

L'appel HTTP sépare réellement les processus : chaque service peut être développé, déployé, redémarré et dimensionné indépendamment. Il impose cependant un coût réseau (latence, sérialisation, configuration et gestion des pannes) qui n'existe pas dans un appel direct en mémoire.

### A.4.2 - Arrêt de calcul-service

Les endpoints de lecture de l'historique continuent de fonctionner car les opérations sont déjà dans la mémoire d'historique-service. Une création ou une modification nécessitant un nouveau calcul échoue. `ResourceAccessException` est convertie en `503 Service Unavailable`.

### A.4.3 - Ports

Un port TCP identifie un point d'écoute sur une adresse IP. Deux applications ne peuvent pas réserver simultanément le même couple adresse/port ; le système refuse le second démarrage avec une erreur de bind. Les services utilisent donc 8081 et 8082.

### A.4.4 - Code HTTP en cas de dépendance indisponible

Le code attendu est **503 Service Unavailable** : la requête est valide, mais le service temporairement nécessaire (`calcul-service`) n'est pas accessible.

## 5. Observations des étapes C, D et E

### C.1 - Parties d'URL disparues par rapport au TP2

Dans le TP2, l'URL pouvait contenir le contexte de l'application et le chemin de déploiement du serveur JAX-RS. Avec Spring Boot exécutable, il n'y a pas de WAR ni de contexte ajouté par un conteneur externe : on utilise directement `http://localhost:8081/calcul/...`. Le port reste une partie de l'URL.

### C.2 - POST envoyé à la place de GET

Le contrôleur ne déclare que `@GetMapping`. Un `POST` sur le même chemin ne correspond à aucune méthode HTTP autorisée et Spring répond `405 Method Not Allowed` (avec l'en-tête `Allow` indiquant généralement `GET`).

### C.3 - Correspondance Spring / JAX-RS

| Spring MVC | JAX-RS | Rôle |
|---|---|---|
| `@RestController` | `@Path` sur une ressource | Déclarer une ressource REST |
| `@RequestMapping("/calcul")` | `@Path("/calcul")` | Préfixe de chemin |
| `@GetMapping` | `@GET` | Méthode HTTP GET |
| `@PostMapping` | `@POST` | Méthode HTTP POST |
| `@PutMapping` | `@PUT` | Méthode HTTP PUT |
| `@DeleteMapping` | `@DELETE` | Méthode HTTP DELETE |
| `@RequestParam` | `@QueryParam` | Paramètre de requête |
| `@PathVariable` | `@PathParam` | Variable de chemin |
| `@RequestBody` | paramètre désérialisé par `MessageBodyReader` | Corps de requête |
| `produces` | `@Produces` | Type de réponse |
| `consumes` | `@Consumes` | Type de requête accepté |
| `ResponseEntity<T>` | `Response.status(...).entity(...)` | Statut, en-têtes et corps |

### D.1 - Paramètre absent

Avec JAX-RS, l'absence d'un `@QueryParam` primitif peut laisser la valeur Java par défaut (`0`) selon la signature et le fournisseur. Avec Spring Boot, un `@RequestParam` obligatoire absent provoque une erreur de requête (`400 Bad Request`). Ce comportement Spring est préférable pour une API REST car il signale explicitement une requête invalide au lieu de transformer une omission en valeur métier implicite.

### D.2 - Erreur Spring Boot

Le corps d'erreur Spring Boot contient normalement des champs tels que `timestamp`, `status`, `error`, `path` et éventuellement `message` selon la configuration et la version. Le contenu exact peut varier ; le contrat important ici est le statut HTTP `400`.

### D.3 - `@RequestParam` et `@PathVariable`

`@RequestParam` lit une donnée après `?`, par exemple `/calcul/somme?a=8&b=3`. `@PathVariable` lit une donnée intégrée au chemin, par exemple `/calcul/produit/8/3`. Le premier convient aux paramètres d'une requête d'opération ; le second rend une ressource ou une action identifiable dans le chemin.

### E.1 - JSON Jackson et JSON-B

Pour `Resultat`, Jackson produit un JSON équivalent à celui obtenu avec JSON-B : les propriétés Java `operation`, `a`, `b` et `resultat` deviennent les mêmes clés JSON. La bibliothèque et les conventions de configuration diffèrent, mais la représentation observée est équivalente.

### E.2 - `Accept: application/xml`

Le service ne configure qu'une représentation JSON pour les méthodes détaillées. Si le client exige seulement `Accept: application/xml` et qu'aucun convertisseur XML n'est disponible, Spring ne peut pas satisfaire la négociation et répond `406 Not Acceptable`.

## 6. Historique-service et tests F.6 à F.9

Le corps de création ou de modification est :

```json
{"operateur":"somme","a":8,"b":3}
```

Le champ `resultat` est calculé par `calcul-service`, pas fourni comme source de vérité par le client.

### F.6 - Création et lecture

| Requête | Corps | Statut | Réponse attendue |
|---|---|---:|---|
| `POST http://localhost:8082/operations` | `{"operateur":"somme","a":8,"b":3}` | 201 | `{"id":1,"operateur":"somme","a":8,"b":3,"resultat":11.0}` et `Location: /operations/1` |
| `GET http://localhost:8082/operations` | aucun | 200 | `[{"id":1,"operateur":"somme","a":8,"b":3,"resultat":11.0}]` |
| `GET http://localhost:8082/operations/1` | aucun | 200 | l'objet d'identifiant `1` |

### F.7 - Opérations CRUD

| Requête | Corps | Statut | Réponse attendue |
|---|---|---:|---|
| `POST /operations` avec `produit`, `a=4`, `b=5` | `{"operateur":"produit","a":4,"b":5}` | 201 | objet avec `resultat:20.0` |
| `PUT /operations/1` | `{"operateur":"soustraction","a":10,"b":4}` | 200 | objet id `1` avec `resultat:6.0` |
| `DELETE /operations/1` | aucun | 204 | aucun corps |
| `GET /operations/1` après suppression | aucun | 404 | aucun corps |

### F.8 - Erreurs métier

| Requête | Corps | Statut | Réponse attendue |
|---|---|---:|---|
| `POST /operations` | `{"operateur":"division","a":8,"b":0}` | 400 | erreur de division retransmise sans création |
| `POST /operations` | `{"operateur":"inconnue","a":8,"b":3}` | 400 | `Opérateur invalide` |
| `PUT /operations/999` | objet valide quelconque | 404 | aucun corps |
| `DELETE /operations/999` | aucun | 404 | aucun corps |

### F.9 - Service indisponible

Après arrêt de calcul-service :

| Requête | Corps | Statut | Réponse attendue |
|---|---|---:|---|
| `GET /operations` | aucun | 200 | historique déjà mémorisé |
| `GET /operations/1` | aucun | 200 ou 404 | opération déjà mémorisée ou absente |
| `POST /operations` | `{"operateur":"somme","a":8,"b":3}` | 503 | `calcul-service est indisponible` |
| `PUT /operations/1` | `{"operateur":"somme","a":8,"b":3}` | 503 | `calcul-service est indisponible` |

Les réponses d'erreur peuvent être enveloppées dans le format d'erreur standard de Spring Boot ; le code HTTP indiqué est le résultat contractuel.

## 7. Questions F

### F.1 - Pourquoi les lectures fonctionnent-elles service arrêté ?

`OperationService` conserve les opérations dans une `ConcurrentHashMap` locale à historique-service. `GET /operations` et `GET /operations/{id}` ne contactent donc pas `CalculClient` et peuvent lire les données déjà présentes.

### F.2 - Rôle de `ResourceAccessException`

Cette exception représente l'échec d'accès réseau au service appelé, notamment une connexion refusée lorsque le port 8081 est arrêté. Le contrôleur la distingue d'une erreur métier `400` et renvoie `503`, statut adapté à une indisponibilité temporaire d'une dépendance.

## 8. Questions de synthèse

### 8.1 - Comparatif JAX-RS / Spring Boot observé

| Aspect | JAX-RS | Spring Boot | Étape d'observation |
|---|---|---|---|
| Démarrage | serveur/runtime JAX-RS externe ou embarqué | classe `@SpringBootApplication` exécutable | C |
| Routage | `@Path`, `@GET` | `@RequestMapping`, `@GetMapping` | C.2, D |
| Paramètres | `@QueryParam`, `@PathParam` | `@RequestParam`, `@PathVariable` | D.1-D.3 |
| JSON | JSON-B/provider | Jackson/http message converters | E |
| Réponse | `Response` | `ResponseEntity` | D.4, E.4 |
| Gestion d'erreur | exceptions/providers JAX-RS | statuts Spring et `ResponseStatusException` | D.2, E.4, F.8-F.9 |
| Configuration | fichiers/runtime du serveur | `application.properties` | D.1, F.1 |
| Appel interservice | client HTTP JAX-RS | `RestClient` | F.3 |

### 8.2 - Correspondance complète des annotations

| JAX-RS | Spring |
|---|---|
| `@ApplicationPath` | `@SpringBootApplication` et configuration Spring Boot |
| `@Path` | `@RequestMapping` |
| `@GET` / `@POST` / `@PUT` / `@DELETE` | `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` |
| `@PathParam` | `@PathVariable` |
| `@QueryParam` | `@RequestParam` |
| `@HeaderParam` | `@RequestHeader` |
| `@CookieParam` | `@CookieValue` |
| `@FormParam` | `@RequestParam` avec formulaire |
| `@Consumes` | `consumes` sur mapping |
| `@Produces` | `produces` sur mapping |
| `@Context UriInfo` | `UriComponentsBuilder`, `ServletUriComponentsBuilder` |
| `Response` | `ResponseEntity` |
| `ExceptionMapper` | `@ControllerAdvice` / `@ExceptionHandler` |

### 8.3 - Construction et lancement

```powershell
.\mvnw.cmd -f pom.xml clean package
java -jar target\calcul-service-0.0.1-SNAPSHOT.jar

.\mvnw.cmd -f historique-service\pom.xml clean package
java -jar historique-service\target\historique-service-0.0.1-SNAPSHOT.jar
```

### 8.4 - Service Web et microservice

Un service Web est une fonctionnalité accessible par un protocole réseau standardisé, par exemple HTTP/REST. Un microservice est une unité autonome de déploiement et d'évolution, centrée sur une capacité métier, qui peut exposer un service Web mais inclut aussi son cycle de vie, sa configuration et son isolation. Tout microservice peut donc exposer un service Web, mais tout service Web n'est pas nécessairement un microservice.

### 8.5 - URL en dur et scalabilité

`http://localhost:8081` convient au TP local mais devient fragile lorsque plusieurs instances, conteneurs ou machines sont utilisés. L'adresse change selon l'environnement et ne fournit ni découverte ni équilibrage. Au TP4, un Service Discovery permettra de trouver les instances et un Load Balancer de répartir les appels et de gérer leur disponibilité.

### 8.6 - Mémoire et principe Stateless

Le principe REST Stateless signifie que chaque requête contient les informations nécessaires à son traitement et que le serveur ne conserve pas de contexte de session client entre deux requêtes. Stocker une collection métier en mémoire n'est pas une session HTTP : chaque GET identifie sa ressource par son URI et peut être traité sans dépendre d'un historique de conversation. Cette mémoire reste toutefois volatile et ne fournit pas la durabilité attendue en production.

### 8.7 - Idempotence

`POST /operations` n'est pas idempotent : répéter la même requête crée normalement une nouvelle opération et un nouvel identifiant. `PUT /operations/1` est conçu pour être idempotent : répéter la même représentation remplace la ressource `1` par le même état, sans créer une nouvelle ressource.
