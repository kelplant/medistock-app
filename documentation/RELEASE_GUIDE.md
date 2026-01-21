# Guide de Création de Releases pour MediStock

Ce guide explique comment créer des releases pour permettre la mise à jour automatique de l'application MediStock.

## 📋 Table des matières

1. [Méthode Automatique (Recommandée)](#méthode-automatique-recommandée)
2. [Méthode Manuelle](#méthode-manuelle)
3. [Configuration Initiale](#configuration-initiale)
4. [Vérification du Système de Mise à Jour](#vérification-du-système-de-mise-à-jour)

---

## 🤖 Méthode Automatique (Recommandée)

Le workflow GitHub Actions automatise le processus de création de release.

### Étapes :

1. **Mettre à jour la version dans `app/build.gradle`**

   **IMPORTANT** : Vous devez mettre à jour la version AVANT de créer le tag.

   ```gradle
   defaultConfig {
       applicationId "com.medistock"
       minSdk 26
       targetSdk 34
       versionCode 9        // Incrémenter de 1 à chaque release
       versionName "0.7.0"  // Nouvelle version (SANS le préfixe "v")
   }
   ```

2. **Committer et pousser les changements**
   ```bash
   git add app/build.gradle
   git commit -m "chore: bump version to 0.7.0"
   git push origin main
   ```

3. **Créer et pousser le tag de version**
   ```bash
   # Assurez-vous d'être sur la branche main
   git checkout main
   git pull origin main

   # Créer et pousser le tag (format: v1.2.3)
   # ⚠️ Le tag doit correspondre au versionName dans build.gradle
   git tag v0.7.0
   git push origin v0.7.0
   ```

4. **Le workflow GitHub Actions se déclenche automatiquement et va:**
   - ✅ Vérifier que la version dans `build.gradle` correspond au tag
   - ✅ Compiler l'APK en mode release
   - ✅ Signer l'APK avec votre clé de signature
   - ✅ Créer une release GitHub avec l'APK attaché
   - ✅ Générer les notes de version automatiquement

5. **C'est tout !** La release est créée et disponible pour la mise à jour automatique.

### Voir l'avancement :
- Allez sur : https://github.com/kelplant/medistock-app/actions
- Cliquez sur le workflow "Create Release with APK"
- Surveillez la progression

---

## 🔧 Configuration Initiale

### Pour utiliser le workflow automatique, vous devez configurer les secrets GitHub :

1. **Accédez aux paramètres du dépôt**
   - Allez sur : https://github.com/kelplant/medistock-app/settings/secrets/actions

2. **Créez les secrets suivants** :

   #### `SIGNING_KEY`
   Votre fichier de clé de signature encodé en base64.

   **Comment générer :**
   ```bash
   # Si vous n'avez pas encore de keystore, créez-en un :
   keytool -genkey -v -keystore medistock-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias medistock

   # Encoder le keystore en base64
   base64 medistock-release-key.jks > keystore-base64.txt

   # Copiez le contenu de keystore-base64.txt dans le secret SIGNING_KEY
   ```

   #### `KEY_ALIAS`
   L'alias de votre clé (ex: `medistock`)

   #### `KEY_STORE_PASSWORD`
   Le mot de passe du keystore

   #### `KEY_PASSWORD`
   Le mot de passe de la clé

3. **⚠️ IMPORTANT :**
   - Conservez votre fichier `.jks` en sécurité (ne le committez JAMAIS)
   - Gardez une copie de sauvegarde de votre keystore
   - Notez tous vos mots de passe dans un endroit sûr
   - **Si vous perdez votre keystore, vous ne pourrez plus mettre à jour l'app !**

---

## 📝 Méthode Manuelle

Si vous préférez créer les releases manuellement :

### 1. Mettre à jour la version dans `app/build.gradle`

```gradle
defaultConfig {
    applicationId "com.medistock"
    minSdk 26
    targetSdk 34
    versionCode 8        // Incrémenter de 1
    versionName "0.7.0"  // Nouvelle version (SANS le préfixe "v")
}
```

### 2. Compiler l'APK

```bash
# Compiler l'APK en mode release
./gradlew assembleRelease

# L'APK sera dans : app/build/outputs/apk/release/app-release-unsigned.apk
```

### 3. Signer l'APK

```bash
# Signer l'APK avec jarsigner
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore /chemin/vers/medistock-release-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  medistock

# Aligner l'APK avec zipalign
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  medistock-v0.7.0.apk
```

### 4. Créer la release sur GitHub

1. Allez sur : https://github.com/kelplant/medistock-app/releases/new

2. **Remplissez le formulaire :**
   - **Tag version** : `v0.7.0` (avec le préfixe "v")
   - **Release title** : `v0.7.0` ou un titre descriptif
   - **Description** : Décrivez les nouveautés de cette version

3. **Téléversez l'APK :**
   - Glissez-déposez `medistock-v0.7.0.apk` dans la zone "Attach binaries"
   - ⚠️ Le nom du fichier **DOIT** contenir "medistock" et se terminer par ".apk"

4. **Publiez la release :**
   - Décochez "Set as a pre-release" (sauf si c'est une préversion)
   - Décochez "Set as the latest release" uniquement si nécessaire
   - Cliquez sur "Publish release"

### 5. Vérifier la release

```bash
# Vérifier via l'API GitHub
curl -s https://api.github.com/repos/kelplant/medistock-app/releases/latest | grep '"tag_name"'
```

---

## ✅ Vérification du Système de Mise à Jour

### Comment ça fonctionne :

1. **Vérification automatique :**
   - L'application vérifie automatiquement les mises à jour toutes les 5 minutes quand elle revient au premier plan
   - La vérification se fait aussi au démarrage de la HomeActivity

2. **Affichage de la notification :**
   - Si une mise à jour est disponible, un dialogue s'affiche avec :
     - La version actuelle
     - La nouvelle version
     - Les notes de version
     - Un bouton "Télécharger"

3. **Téléchargement et installation :**
   - L'utilisateur clique sur "Télécharger"
   - L'APK est téléchargé depuis GitHub Releases
   - L'application demande la permission d'installer des sources inconnues (Android 8+)
   - L'installation démarre automatiquement

### Tester la mise à jour :

1. **Installez la version actuelle (0.6.0) sur un appareil**

2. **Créez une nouvelle release (0.7.0) sur GitHub**

3. **Sur l'appareil :**
   - Fermez l'application complètement
   - Rouvrez l'application
   - Attendez quelques secondes
   - Un dialogue devrait apparaître proposant la mise à jour

4. **Vérifiez les logs :**
   ```bash
   adb logcat | grep -i "update\|version\|github"
   ```

### Dépannage :

#### La mise à jour ne s'affiche pas
- ✅ Vérifiez que la version dans `build.gradle` est bien inférieure à celle de la release
- ✅ Vérifiez que le nom du fichier APK contient "medistock" et se termine par ".apk"
- ✅ Vérifiez que la release n'est pas marquée comme "draft" ou "prerelease"
- ✅ Vérifiez les logs avec `adb logcat`

#### L'installation échoue
- ✅ Vérifiez que l'APK est signé avec la même clé que l'application installée
- ✅ Vérifiez que la permission d'installer des sources inconnues est accordée
- ✅ Vérifiez que le versionCode est supérieur à celui de l'application installée

---

---

## 🍎 Configuration iOS (Quand les certificats seront disponibles)

Le workflow de release supporte également la génération d'IPA iOS, mais nécessite une configuration préalable.

### Prérequis

1. **Compte Apple Developer** (99$/an)
2. **Certificat de distribution** (.p12)
3. **Provisioning Profile** (App Store ou Ad Hoc)

### Étape 1 : Activer le build iOS

Dans GitHub → Settings → Variables → Repository variables :

| Variable | Valeur |
|----------|--------|
| `IOS_BUILD_ENABLED` | `true` |

### Étape 2 : Configurer les secrets

Dans GitHub → Settings → Secrets → Actions, ajouter :

| Secret | Description | Comment l'obtenir |
|--------|-------------|-------------------|
| `IOS_BUILD_CERTIFICATE_BASE64` | Certificat .p12 encodé en base64 | `base64 -i certificate.p12` |
| `IOS_P12_PASSWORD` | Mot de passe du certificat .p12 | Défini lors de l'export |
| `IOS_KEYCHAIN_PASSWORD` | Mot de passe temporaire pour le keychain CI | Générer un mot de passe aléatoire |
| `IOS_PROVISIONING_PROFILE_BASE64` | Provisioning profile encodé en base64 | `base64 -i profile.mobileprovision` |
| `IOS_TEAM_ID` | ID de l'équipe Apple Developer | Visible sur developer.apple.com |
| `IOS_PROVISIONING_PROFILE_NAME` | Nom exact du provisioning profile | Nom défini dans Apple Developer Portal |

### Étape 3 : Exporter le certificat depuis Xcode

1. Ouvrir **Keychain Access**
2. Trouver le certificat "Apple Distribution: ..."
3. Clic droit → **Export...**
4. Sauvegarder en format `.p12` avec un mot de passe
5. Encoder en base64 :
   ```bash
   base64 -i MyCertificate.p12 | pbcopy
   ```
6. Coller dans le secret `IOS_BUILD_CERTIFICATE_BASE64`

### Étape 4 : Télécharger le Provisioning Profile

1. Aller sur https://developer.apple.com/account/resources/profiles/list
2. Créer ou télécharger un profil "App Store" ou "Ad Hoc"
3. Encoder en base64 :
   ```bash
   base64 -i MyProfile.mobileprovision | pbcopy
   ```
4. Coller dans le secret `IOS_PROVISIONING_PROFILE_BASE64`

### Vérification

Une fois configuré, le prochain tag `v*.*.*` déclenchera :
- ✅ Build Android APK (comme avant)
- ✅ Build iOS IPA
- ✅ Release GitHub avec les deux fichiers

---

## 📚 Ressources

- **Documentation Android :** https://developer.android.com/studio/publish/app-signing
- **Documentation iOS Code Signing :** https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- **GitHub Releases :** https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository
- **GitHub Actions :** https://docs.github.com/en/actions

---

## 🔐 Sécurité

- ⚠️ Ne committez JAMAIS votre keystore dans le dépôt
- ⚠️ Ne partagez JAMAIS vos mots de passe de signature
- ✅ Utilisez GitHub Secrets pour stocker les informations sensibles
- ✅ Gardez une copie de sauvegarde de votre keystore

---

## 📞 Support

Si vous rencontrez des problèmes :
1. Vérifiez les logs avec `adb logcat`
2. Consultez les GitHub Actions logs
3. Vérifiez que tous les secrets sont correctement configurés
