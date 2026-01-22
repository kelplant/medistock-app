# Cahier de Recette - MediStock

## Introduction

Ce document décrit les tests à effectuer pour valider le bon fonctionnement de l'application MediStock sur iOS et Android.

**Légende:**
- **[iOS]** Test spécifique iOS
- **[Android]** Test spécifique Android
- **[Both]** Test à effectuer sur les deux plateformes

---

## 1. Authentification

### 1.1 Connexion locale (sans Supabase)

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 1.1.1 | Connexion admin réussie | 1. Lancer l'app<br>2. Entrer identifiants admin valides<br>3. Appuyer "Connexion" | Redirection vers l'accueil, menu complet visible | Vérifier que tous les modules sont accessibles |
| 1.1.2 | Connexion utilisateur standard | 1. Entrer identifiants utilisateur non-admin<br>2. Appuyer "Connexion" | Redirection vers l'accueil, menu filtré selon permissions | Vérifier que seuls les modules autorisés sont visibles |
| 1.1.3 | Mauvais mot de passe | 1. Entrer nom d'utilisateur valide<br>2. Entrer mauvais mot de passe | Message d'erreur "Identifiants invalides" | Message affiché en rouge |
| 1.1.4 | Utilisateur inexistant | 1. Entrer nom d'utilisateur inexistant | Message d'erreur "Utilisateur non trouvé" | Message affiché en rouge |
| 1.1.5 | Utilisateur inactif | 1. Entrer identifiants d'un utilisateur avec isActive=false | Message d'erreur "Compte désactivé" | Message affiché en rouge |
| 1.1.6 | Champs vides | 1. Laisser les champs vides<br>2. Appuyer "Connexion" | Bouton désactivé ou message d'erreur | Impossible de soumettre |

### 1.2 Connexion avec Supabase

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 1.2.1 | Configuration Supabase | 1. Aller dans Admin > Configuration Supabase<br>2. Entrer URL et clé anon<br>3. Sauvegarder | Configuration sauvegardée | Indicateur "En ligne" visible |
| 1.2.2 | Connexion en ligne | 1. Configurer Supabase<br>2. Se connecter avec utilisateur distant | Connexion réussie, données synchronisées | Vérifier dans les logs: "User synced to local" |
| 1.2.3 | Connexion hors ligne (fallback) | 1. Désactiver le réseau<br>2. Se connecter avec utilisateur local existant | Connexion réussie en mode local | Message "Mode hors ligne" visible |

### 1.3 Déconnexion

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 1.3.1 | Déconnexion | 1. Appuyer sur Profil/Déconnexion<br>2. Confirmer | Retour à l'écran de connexion | Session effacée |

---

## 2. Gestion des Sites

### 2.1 Liste des sites

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 2.1.1 | Affichage liste | 1. Aller dans Sites | Liste des sites affichée avec compteur | Compteur correspond au nombre de sites |
| 2.1.2 | Pull-to-refresh | 1. Tirer vers le bas sur la liste | Rechargement des données + sync si en ligne | Indicateur de chargement visible |

### 2.2 Création de site

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 2.2.1 | Créer un site | 1. Appuyer "+"<br>2. Entrer nom "Site Test"<br>3. Sauvegarder | Site créé, retour à la liste | Site visible dans la liste |
| 2.2.2 | Nom vide | 1. Appuyer "+"<br>2. Laisser nom vide<br>3. Sauvegarder | Bouton désactivé ou erreur | Impossible de sauvegarder |

### 2.3 Modification de site

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 2.3.1 | Modifier un site | 1. Taper sur un site<br>2. Modifier le nom<br>3. Sauvegarder | Site modifié | Nouveau nom visible dans la liste |

### 2.4 Suppression de site

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 2.4.1 | Supprimer un site | 1. Swipe gauche sur un site (iOS) / Appui long (Android)<br>2. Confirmer suppression | Site supprimé | Site absent de la liste |

---

## 3. Gestion des Catégories

### 3.1 CRUD Catégories

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 3.1.1 | Créer catégorie | 1. Aller dans Catégories<br>2. Appuyer "+"<br>3. Entrer "Médicaments"<br>4. Sauvegarder | Catégorie créée | Visible dans la liste |
| 3.1.2 | Modifier catégorie | 1. Taper sur une catégorie<br>2. Modifier le nom<br>3. Sauvegarder | Catégorie modifiée | Nouveau nom affiché |
| 3.1.3 | Supprimer catégorie | 1. Swipe/appui long<br>2. Confirmer | Catégorie supprimée | Absente de la liste |

---

## 4. Gestion des Produits

### 4.1 Liste des produits

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 4.1.1 | Affichage liste | 1. Aller dans Produits | Liste avec nom, site, catégorie, unité | Informations complètes affichées |
| 4.1.2 | Recherche | 1. Utiliser la barre de recherche<br>2. Taper "Para" | Filtrage des produits contenant "Para" | Seuls les produits correspondants affichés |

### 4.2 Création de produit

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 4.2.1 | Créer produit complet | 1. Appuyer "+"<br>2. Nom: "Paracétamol 500mg"<br>3. Unité: "comprimé"<br>4. Volume: 1<br>5. Site: sélectionner<br>6. Catégorie: "Médicaments"<br>7. Sauvegarder | Produit créé | Visible dans la liste avec toutes les infos |
| 4.2.2 | Produit sans site | 1. Créer produit sans sélectionner de site | Erreur ou bouton désactivé | Site obligatoire |
| 4.2.3 | Produit sans catégorie | 1. Créer produit sans catégorie | Produit créé avec "Aucune" catégorie | Catégorie optionnelle |

### 4.3 Modification de produit

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 4.3.1 | Modifier produit | 1. Taper sur un produit<br>2. Changer le nom et l'unité<br>3. Sauvegarder | Produit modifié | Nouvelles valeurs affichées |

---

## 5. Gestion des Clients

### 5.1 CRUD Clients

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 5.1.1 | Créer client | 1. Aller dans Clients<br>2. Appuyer "+"<br>3. Nom: "Pharmacie Centrale"<br>4. Téléphone: "0123456789"<br>5. Email: "contact@pharma.com"<br>6. Sauvegarder | Client créé | Visible avec toutes les infos |
| 5.1.2 | Client nom seul | 1. Créer client avec uniquement le nom | Client créé | Téléphone/email optionnels |
| 5.1.3 | Recherche client | 1. Rechercher "Pharmacie" | Filtrage par nom | Client trouvé |

---

## 6. Achats (Purchases) - CRITIQUE

### 6.1 Création d'achat

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 6.1.1 | Créer lot d'achat | 1. Aller dans Achats<br>2. Appuyer "+"<br>3. Produit: sélectionner<br>4. Site: sélectionner<br>5. Quantité: 100<br>6. Prix d'achat: 5.00<br>7. Fournisseur: "Grossiste A"<br>8. Sauvegarder | Lot créé | Visible dans la liste avec statut "actif" |
| 6.1.2 | Vérifier stock augmenté | Après 6.1.1 | Stock du produit = +100 | Aller dans Stock, vérifier quantité |
| 6.1.3 | Vérifier mouvement créé | Après 6.1.1 | Mouvement de type PURCHASE créé | Aller dans Mouvements, vérifier entrée |
| 6.1.4 | Achat avec date d'expiration | 1. Créer achat avec date d'expiration dans 20 jours | Achat créé avec warning "Expire bientôt" | Avertissement affiché |
| 6.1.5 | Quantité négative | 1. Entrer quantité -10 | Erreur de validation | Impossible de sauvegarder |

### 6.2 Calcul du prix de vente (marge)

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 6.2.1 | Marge fixe | 1. Produit avec marginType="fixed", marginValue=2<br>2. Créer achat prix=10 | Prix de vente calculé = 12 | Vérifier dans les logs ou le détail |
| 6.2.2 | Marge pourcentage | 1. Produit avec marginType="percentage", marginValue=20<br>2. Créer achat prix=10 | Prix de vente calculé = 12 (10 × 1.2) | Vérifier dans les logs |

---

## 7. Ventes (Sales) - CRITIQUE (FIFO)

### 7.1 Préparation des tests FIFO

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 7.1.1 | Créer plusieurs lots | 1. Créer Lot A: 50 unités, prix 5€, date J-30<br>2. Créer Lot B: 30 unités, prix 6€, date J-15<br>3. Créer Lot C: 20 unités, prix 7€, date J-5 | 3 lots créés, stock total = 100 | Vérifier dans Achats |

### 7.2 Création de vente avec FIFO

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 7.2.1 | Vente simple (1 lot) | 1. Créer vente<br>2. Client: "Test"<br>3. Ajouter produit: 30 unités<br>4. Prix: 10€<br>5. Valider | Vente créée, Lot A: reste 20 | Vérifier Lot A remaining=20 |
| 7.2.2 | Vente multi-lots | 1. Vendre 60 unités du même produit | Lot A épuisé (0), Lot B partiellement utilisé | Lot A: isExhausted=true, Lot B: remaining=20 |
| 7.2.3 | Vente totale stock | 1. Vendre 100 unités (tout le stock) | Tous les lots épuisés | 3 lots avec isExhausted=true |
| 7.2.4 | Vente stock insuffisant | 1. Vendre 150 unités (stock=100) | Vente autorisée avec WARNING | Warning "Stock insuffisant" affiché, stock devient négatif |
| 7.2.5 | Vérifier allocations FIFO | Après 7.2.2 | SaleBatchAllocations créées | Vérifier que Lot A (le plus ancien) est utilisé en premier |

### 7.3 Détail de vente

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 7.3.1 | Voir détail vente | 1. Taper sur une vente | Détail avec articles, quantités, prix, total | Toutes les infos affichées |

---

## 8. Transferts - CRITIQUE (FIFO)

### 8.1 Préparation

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 8.1.1 | Créer 2 sites | 1. Site A: "Entrepôt"<br>2. Site B: "Boutique" | 2 sites créés | Visibles dans la liste |
| 8.1.2 | Créer stock Site A | 1. Acheter 100 unités produit X sur Site A | Stock Site A = 100 | Vérifier dans Stock |

### 8.2 Transfert entre sites

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 8.2.1 | Transfert simple | 1. Aller dans Transferts<br>2. Appuyer "+"<br>3. Produit: X<br>4. De: Site A<br>5. Vers: Site B<br>6. Quantité: 30<br>7. Valider | Transfert créé, status=completed | Stock A=-30, Stock B=+30 |
| 8.2.2 | Vérifier lots transférés | Après 8.2.1 | Nouveau lot créé sur Site B | Lot avec batchNumber contenant "-TRANSFER" |
| 8.2.3 | Transfert même site | 1. Essayer de transférer de Site A vers Site A | Erreur bloquante | Impossible de valider |
| 8.2.4 | Transfert stock insuffisant | 1. Transférer 200 unités (stock=100) | Transfert autorisé avec WARNING | Warning affiché, stock négatif |

### 8.3 Vérification FIFO sur transfert

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 8.3.1 | Transfert préserve dates | 1. Créer 2 lots dates différentes<br>2. Transférer | Lots destination gardent dates originales | Vérifier purchaseDate des lots destination |
| 8.3.2 | Transfert préserve prix | 1. Lots avec prix différents<br>2. Transférer | Prix d'achat préservé | Vérifier purchasePrice des lots |

---

## 9. Inventaire - CRITIQUE

### 9.1 Création d'inventaire

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 9.1.1 | Inventaire sans écart | 1. Stock théorique = 100<br>2. Créer inventaire<br>3. Compter 100 | Inventaire OK, pas d'ajustement | Aucun mouvement d'inventaire créé |
| 9.1.2 | Inventaire surplus | 1. Stock théorique = 100<br>2. Compter 120 | Écart +20, nouveau lot créé | Lot "INV-xxx" créé avec qty=20 |
| 9.1.3 | Inventaire manque | 1. Stock théorique = 100<br>2. Compter 80 | Écart -20, lots FIFO réduits | Oldest lots reduced by 20 |
| 9.1.4 | Inventaire multi-produits | 1. Compter plusieurs produits | Tous les écarts calculés | Ajustements individuels par produit |

### 9.2 Vérifications inventaire

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 9.2.1 | Mouvement type INVENTORY | Après ajustement | StockMovement type=INVENTORY | Visible dans Mouvements |
| 9.2.2 | Stock corrigé | Après inventaire | Stock = quantité comptée | Vérifier dans Stock |

---

## 10. Stock et Mouvements

### 10.1 Visualisation stock

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 10.1.1 | Affichage stock | 1. Aller dans Stock | Liste produits avec quantités | Quantités correspondent aux calculs |
| 10.1.2 | Stock négatif | 1. Créer stock négatif (vente > stock) | Quantité négative affichée en rouge | Indicateur visuel |
| 10.1.3 | Rupture de stock | 1. Vider complètement un produit | Stock = 0, indicateur "Rupture" | Badge ou couleur spéciale |
| 10.1.4 | Recherche stock | 1. Rechercher un produit | Filtrage par nom | Produit trouvé |

### 10.2 Historique mouvements

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 10.2.1 | Liste mouvements | 1. Aller dans Mouvements | Historique avec type, quantité, date | Tous les mouvements listés |
| 10.2.2 | Types de mouvements | Vérifier présence de | PURCHASE (+), SALE (-), TRANSFER_IN (+), TRANSFER_OUT (-), INVENTORY (±) | Chaque type avec signe correct |

---

## 11. Utilisateurs et Permissions

### 11.1 Gestion utilisateurs

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 11.1.1 | Créer utilisateur | 1. Aller dans Utilisateurs<br>2. Appuyer "+"<br>3. Username: "vendeur1"<br>4. Password: "test123"<br>5. Nom: "Jean Vendeur"<br>6. Admin: Non<br>7. Sauvegarder | Utilisateur créé | Visible dans la liste |
| 11.1.2 | Désactiver utilisateur | 1. Modifier utilisateur<br>2. Désactiver "Actif"<br>3. Sauvegarder | Utilisateur inactif | Badge "Inactif" visible |
| 11.1.3 | Changer mot de passe | 1. Modifier utilisateur<br>2. Entrer nouveau mot de passe<br>3. Sauvegarder | Mot de passe changé | Connexion avec nouveau MDP fonctionne |

### 11.2 Permissions

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 11.2.1 | Configurer permissions | 1. Utilisateur non-admin<br>2. Appuyer "Droits"<br>3. Activer: Ventes (voir, créer)<br>4. Sauvegarder | Permissions sauvegardées | Se connecter avec cet utilisateur, vérifier accès |
| 11.2.2 | Tester restriction | 1. Connecter utilisateur avec permissions limitées | Seuls modules autorisés visibles | Modules non autorisés cachés |
| 11.2.3 | Permission view-only | 1. Donner uniquement "Voir" sur un module | Pas de bouton "+" ni modification | CRUD désactivé |

---

## 12. Synchronisation

### 12.1 Sync manuelle

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 12.1.1 | Pull-to-refresh | 1. Tirer vers le bas sur une liste | Sync déclenchée | Indicateur de sync visible |
| 12.1.2 | Sync complète | 1. Menu > Synchroniser | Toutes les tables synchronisées | Message "Synchronisation terminée" |

### 12.2 Sync automatique

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 12.2.1 | Sync au démarrage | 1. Lancer l'app en ligne | Sync automatique | Données à jour |
| 12.2.2 | Indicateur en ligne | 1. Vérifier barre d'état | Indicateur "En ligne" vert | Visible en permanence |

### 12.3 Mode hors ligne

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 12.3.1 | Créer données offline | 1. Désactiver réseau<br>2. Créer un produit<br>3. Créer une vente | Données créées localement | Visibles dans l'app |
| 12.3.2 | Sync après reconnexion | 1. Réactiver réseau<br>2. Attendre ou forcer sync | Données envoyées à Supabase | Vérifier sur autre device ou Supabase |
| 12.3.3 | Indicateur hors ligne | 1. Désactiver réseau | Indicateur "Hors ligne" rouge | Visible dans l'app |

### 12.4 Conflits de sync

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 12.4.1 | Modification concurrente | 1. Device A: modifier produit<br>2. Device B: modifier même produit (offline)<br>3. Sync Device B | Dernier timestamp gagne | Vérifier quelle version est conservée |

---

## 13. Audit Trail

### 13.1 Historique des modifications

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 13.1.1 | Voir audit | 1. Aller dans Admin > Historique | Liste des modifications | Toutes les actions listées |
| 13.1.2 | Filtrer par table | 1. Filtrer par "products" | Seules les modifications produits | Filtre fonctionnel |
| 13.1.3 | Détail modification | 1. Taper sur une entrée | Anciennes et nouvelles valeurs | JSON old/new visible |

---

## 14. Tests Multi-Plateformes [Both]

### 14.1 Cohérence iOS/Android

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 14.1.1 | Données identiques | 1. Créer données sur iOS<br>2. Sync<br>3. Vérifier sur Android | Données identiques | Comparer les deux apps |
| 14.1.2 | FIFO cohérent | 1. Créer lots sur iOS<br>2. Vendre sur Android | Même lots épuisés | Vérifier allocations |
| 14.1.3 | Sync bidirectionnelle | 1. Modifier sur iOS<br>2. Modifier autre entité sur Android<br>3. Sync les deux | Toutes les modifications présentes | Données complètes des deux côtés |

---

## 15. Tests de Performance

### 15.1 Volume de données

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 15.1.1 | 1000+ produits | 1. Charger 1000 produits | Liste fluide, recherche rapide | Pas de lag visible |
| 15.1.2 | 100+ lots FIFO | 1. Produit avec 100 lots<br>2. Vendre | Allocation rapide | < 2 secondes |
| 15.1.3 | Sync 10000 mouvements | 1. Sync avec historique conséquent | Sync complète | < 30 secondes |

---

## 16. Cas Limites et Erreurs

### 16.1 Validations

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 16.1.1 | Champs obligatoires vides | 1. Soumettre formulaire avec champs vides | Erreur de validation | Message d'erreur clair |
| 16.1.2 | Valeurs numériques invalides | 1. Entrer texte dans champ numérique | Erreur ou filtrage | Clavier numérique, validation |
| 16.1.3 | Caractères spéciaux | 1. Entrer "Test <script>" dans un nom | Données sauvegardées sans exécution | Pas de XSS |

### 16.2 Cas d'erreur réseau

| # | Test | Étapes | Résultat attendu | Vérification |
|---|------|--------|------------------|--------------|
| 16.2.1 | Perte connexion pendant sync | 1. Lancer sync<br>2. Couper réseau | Sync interrompue proprement | Pas de crash, message d'erreur |
| 16.2.2 | Supabase indisponible | 1. Configurer mauvaise URL | Fallback local | App fonctionne en local |

---

## Annexe A: Checklist Rapide

### Avant Release

- [ ] Tous les tests d'authentification passent
- [ ] CRUD fonctionne pour toutes les entités
- [ ] FIFO vérifié sur ventes
- [ ] FIFO vérifié sur transferts
- [ ] Inventaire crée les bons ajustements
- [ ] Sync fonctionne iOS ↔ Supabase ↔ Android
- [ ] Mode hors ligne fonctionne
- [ ] Permissions respectées
- [ ] Audit trail complet
- [ ] Pas de crash sur les cas limites

---

## Annexe B: Données de Test Recommandées

### Utilisateurs
| Username | Password | Admin | Permissions |
|----------|----------|-------|-------------|
| admin | admin123 | Oui | Toutes |
| vendeur | vendeur123 | Non | Ventes (CRUD), Stock (View) |
| manager | manager123 | Non | Toutes sauf Admin |

### Sites
| Nom |
|-----|
| Entrepôt Central |
| Boutique Paris |
| Boutique Lyon |

### Produits
| Nom | Unité | Catégorie |
|-----|-------|-----------|
| Paracétamol 500mg | comprimé | Médicaments |
| Ibuprofène 400mg | comprimé | Médicaments |
| Sirop Toux | flacon | Médicaments |
| Pansement | boîte | Matériel |

### Lots initiaux (pour tests FIFO)
| Produit | Quantité | Prix | Date |
|---------|----------|------|------|
| Paracétamol | 100 | 0.10€ | J-30 |
| Paracétamol | 50 | 0.12€ | J-15 |
| Paracétamol | 75 | 0.11€ | J-7 |
