# TP XML : Modélisation d'un cinéma, validation XSD et requêtes XPath

## 1. Domaine choisi
Gestion d'un cinéma (**CineCarthage**) : catalogue de films, salles, utilisateurs inscrits et séances programmées. Ce domaine permet d'illustrer les trois requêtes demandées (titres de films, utilisateurs actifs, éléments en promotion).

## 2. Entités et structure XML

| Entité | Élément | Attributs | Sous-éléments |
|---|---|---|---|
| Cinéma | `cinema` (racine) | `nom` | `films`, `salles`, `utilisateurs`, `seances` |
| Film | `film` | `id` (F001…), `statut` (affiche / prochainement) | `titre`, `realisateur`, `genre`, `annee`, `duree`, `note` |
| Salle | `salle` | `id` (S1…) | `nom`, `capacite` |
| Utilisateur | `utilisateur` | `id` (U001…), `actif` (booléen) | `nom`, `email`, `dateInscription` |
| Séance | `seance` | `id` (SE01…), `filmRef`, `salleRef`, `promotion` (booléen) | `dateHeure`, `prix` (attribut `devise`) |

### Choix de modélisation
- **Attributs** pour les identifiants et les indicateurs (`id`, `actif`, `promotion`, `statut`) : ce sont des métadonnées courtes, faciles à filtrer en XPath (`[@actif='true']`).
- **Sous-éléments** pour les données descriptives (titre, durée, note…) : elles peuvent s'enrichir (sous-structure) plus tard.
- **Références** : une séance ne duplique pas le film ni la salle, elle pointe vers eux via `filmRef` et `salleRef` (modèle relationnel, pas de redondance).
- **Regroupement** (`films`, `salles`…) : un conteneur par entité, pour une structure lisible et des chemins XPath clairs.

## 3. Schéma XSD (`cinema.xsd`)
- **Types simples avec restrictions** : `noteType` (décimal entre 0 et 10), `anneeType` (1895–2100), `genreType` et `statutType` (énumérations), `emailType` (pattern), identifiants (pattern `F\d{3}`, `U\d{3}`…).
- **Types complexes nommés et réutilisables** : `filmType`, `salleType`, `utilisateurType`, `seanceType`.
- **`prixType`** : contenu simple (`simpleContent`) avec un attribut `devise`.
- **Intégrité référentielle** : `xs:key` sur les identifiants de films et de salles, et `xs:keyref` pour obliger `filmRef` / `salleRef` à désigner un film / une salle existants.
- `maxOccurs="unbounded"` pour les collections, `use="required"` pour les attributs obligatoires.

## 4. Projet Maven
- Java 1.8 minimum, encodage UTF-8.
- JAXP (`javax.xml.validation`, `javax.xml.xpath`) est **inclus dans le JDK** : aucune dépendance externe n'est nécessaire. JUnit 4 est déclaré (scope `test`) pour d'éventuels tests, et `exec-maven-plugin` permet `mvn compile exec:java`.
- Les fichiers `cinema.xml` et `cinema.xsd` sont dans `src/main/resources` et chargés via le classpath.

## 5. Implémentation (`tp.Main`)
1. **Validation** : `SchemaFactory` → `Schema` → `Validator`. Un `ErrorHandler` collecte toutes les erreurs avec leur numéro de ligne. Le programme valide `cinema.xml` (valide) puis `cinema-invalide.xml` (une note de 12 et une référence vers un film F999 inexistant) pour montrer la détection d'erreurs.
2. **XPath** : le document est parsé en DOM, puis `XPathFactory` + `XPathExpression` évaluent les requêtes (`NODESET` pour des nœuds, `NUMBER` pour `count`/`sum`).

## 6. Requêtes XPath et résultats

| Objectif | Expression | Résultat |
|---|---|---|
| Tous les titres de films | `//film/titre` | 5 titres |
| Utilisateurs actifs | `//utilisateur[@actif='true']/nom` | Amine Ben Salah, Yassine Gharbi |
| Séances en promotion | `//seance[@promotion='true']/@id` | SE01, SE03 |
| SF notés > 8.6 | `//film[genre='Science-fiction' and note > 8.6]/titre` | Inception, Interstellar |
| Films projetés en promotion | `//film[@id = //seance[@promotion='true']/@filmRef]/titre` | Inception, Interstellar |
| Films à venir | `//film[@statut='prochainement']/titre` | Dune : Deuxième partie |
| Nombre de films | `count(//film)` | 5 |
| Prix moyen d'une séance | `sum(//seance/prix) div count(//seance)` | 12.13 TND |

## 7. Exécution
```
mvn compile exec:java
```
Dans IntelliJ : clic droit sur `Main` > Run. Si les accents s'affichent mal dans la console, ajouter `-Dfile.encoding=UTF-8` aux VM options.
