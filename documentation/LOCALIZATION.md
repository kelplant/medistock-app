# Guide de Localisation (i18n)

Ce guide explique comment fonctionne le système de localisation de MediStock et comment ajouter une nouvelle langue.

## Architecture

Le système i18n est basé sur le module partagé KMM (Kotlin Multiplatform Mobile) :

```
shared/src/commonMain/kotlin/com/medistock/shared/i18n/
├── Strings.kt              # Interface définissant toutes les clés
├── StringsEn.kt            # Anglais (défaut)
├── StringsFr.kt            # Français
├── StringsDe.kt            # Allemand
├── StringsEs.kt            # Espagnol
├── StringsIt.kt            # Italien
├── StringsRu.kt            # Russe
├── StringsBm.kt            # Bemba
├── StringsNy.kt            # Nyanja
├── SupportedLocale.kt      # Enum des langues supportées
└── LocalizationManager.kt  # Gestionnaire de locale
```

Pour iOS, un wrapper Swift existe :
```
iosApp/iosApp/Services/Localization.swift
```

## Langues actuellement supportées

| Code | Langue | Nom natif |
|------|--------|-----------|
| `en` | English | English |
| `fr` | French | Français |
| `de` | German | Deutsch |
| `es` | Spanish | Español |
| `it` | Italian | Italiano |
| `ru` | Russian | Русский |
| `bm` | Bemba | Ichibemba |
| `ny` | Nyanja | Chinyanja |

## Ajouter une nouvelle langue

### Étape 1 : Créer le fichier de traductions

Créer un nouveau fichier `StringsXx.kt` dans `shared/src/commonMain/kotlin/com/medistock/shared/i18n/` où `Xx` est le code ISO de la langue (ex: `Pt` pour portugais).

```kotlin
package com.medistock.shared.i18n

/**
 * Portuguese strings implementation.
 */
object StringsPt : Strings {
    // ============================================
    // COMMON
    // ============================================
    override val appName = "MediStock"
    override val ok = "OK"
    override val cancel = "Cancelar"
    override val save = "Salvar"
    // ... toutes les autres propriétés
}
```

**Astuce** : Copier un fichier existant (ex: `StringsEn.kt`) et traduire toutes les valeurs.

### Étape 2 : Ajouter l'entrée dans SupportedLocale

Ouvrir `SupportedLocale.kt` et ajouter la nouvelle langue dans l'enum :

```kotlin
enum class SupportedLocale(
    val code: String,
    val displayName: String,
    val nativeDisplayName: String
) {
    ENGLISH("en", "English", "English"),
    FRENCH("fr", "French", "Français"),
    // ... autres langues ...
    PORTUGUESE("pt", "Portuguese", "Português");  // <- Ajouter ici

    // ... reste du code
}
```

### Étape 3 : Mettre à jour LocalizationManager

Ouvrir `LocalizationManager.kt` et ajouter le cas dans `getStringsForLocale()` :

```kotlin
fun getStringsForLocale(locale: SupportedLocale): Strings {
    return when (locale) {
        SupportedLocale.ENGLISH -> StringsEn
        SupportedLocale.FRENCH -> StringsFr
        // ... autres langues ...
        SupportedLocale.PORTUGUESE -> StringsPt  // <- Ajouter ici
    }
}
```

### Étape 4 : Mettre à jour le wrapper iOS

Ouvrir `iosApp/iosApp/Services/Localization.swift` et ajouter la langue dans l'enum `AppLanguage` :

```swift
enum AppLanguage: String, CaseIterable, Identifiable {
    case english = "en"
    case french = "fr"
    // ... autres langues ...
    case portuguese = "pt"  // <- Ajouter ici

    var displayName: String {
        switch self {
        // ... autres cas ...
        case .portuguese: return "Português"  // <- Ajouter ici
        }
    }

    var supportedLocale: SupportedLocale {
        switch self {
        // ... autres cas ...
        case .portuguese: return .portuguese  // <- Ajouter ici
        }
    }
}
```

### Étape 5 : Tester

1. Compiler le module partagé :
   ```bash
   ./gradlew :shared:compileKotlinIosArm64
   ```

2. Compiler Android :
   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

3. Compiler iOS :
   ```bash
   cd iosApp && xcodebuild -scheme iosApp build
   ```

## Utilisation dans le code

### Android (Kotlin)

```kotlin
import com.medistock.shared.i18n.L
import com.medistock.shared.i18n.LocalizationManager
import com.medistock.shared.i18n.SupportedLocale

// Changer la langue
LocalizationManager.setLocale(SupportedLocale.FRENCH)
// ou
L.setLocale(SupportedLocale.FRENCH)

// Utiliser les strings
val saveText = L.strings.save  // "Enregistrer"
val title = L.strings.loginTitle  // "Connexion"

// Avec paramètres
val welcome = LocalizationManager.format(
    L.strings.welcomeBack,
    "name" to "Jean"
)  // "Bon retour, Jean !"
```

### iOS (Swift)

```swift
import shared

// Changer la langue
Localized.setLanguage(.french)
// ou par code
Localized.setLanguageByCode("fr")

// Utiliser les strings
Text(Localized.save)  // "Enregistrer"
Text(Localized.loginTitle)  // "Connexion"

// Accès direct via strings
let title = Localized.strings.products  // "Produits"

// Avec paramètres
let welcome = Localized.format(
    Localized.welcomeBack,
    "name", "Jean"
)  // "Bon retour, Jean !"
```

## Export JSON pour traduction

Un fichier JSON est disponible pour faciliter la traduction avec des outils externes (ChatGPT, DeepL, etc.) :

```
shared/src/commonMain/resources/i18n/strings_en.json
```

### Workflow de traduction

1. Copier le fichier JSON
2. Envoyer à ChatGPT avec le prompt :
   ```
   Traduis ce fichier JSON en [langue].
   Garde les clés identiques, traduis uniquement les valeurs.
   Ne traduis pas les placeholders comme {name}, {count}, etc.
   ```
3. Créer le fichier Kotlin correspondant avec les traductions

## Structure des strings

Les strings sont organisées par catégorie :

- **common** : OK, Cancel, Save, Delete, etc.
- **auth** : Login, Logout, Username, Password
- **home** : Dashboard, Quick Actions
- **sites** : Sites management
- **categories** : Categories management
- **products** : Products management
- **customers** : Customers management
- **purchases** : Purchases/batches
- **sales** : Sales management
- **inventory** : Inventory counts
- **transfers** : Stock transfers
- **stock** : Stock movements
- **reports** : Reports
- **settings** : App settings
- **users** : User management
- **packagingTypes** : Packaging configuration
- **validation** : Validation messages
- **referentialIntegrity** : Soft delete messages
- **dateTime** : Date/time formats
- **currency** : Currency formats

## Strings avec paramètres

Certaines strings contiennent des paramètres entre accolades :

```kotlin
// Définition
override val welcomeBack = "Bon retour, {name} !"
override val insufficientStock = "Stock insuffisant pour {product} : {available} disponibles, {requested} demandés"
override val entityInUse = "Ce(tte) {entity} est utilisé(e) dans {count} enregistrements"
```

Utilisation :
```kotlin
LocalizationManager.format(
    L.strings.insufficientStock,
    "product" to "Paracétamol",
    "available" to 10,
    "requested" to 20
)
// "Stock insuffisant pour Paracétamol : 10 disponibles, 20 demandés"
```

## Persistance de la langue

### iOS
La préférence de langue est sauvegardée dans `UserDefaults` sous la clé `app_language`.

### Android
À implémenter selon le besoin (SharedPreferences recommandé).

## Notes importantes

1. **Ne pas traduire le contenu de la base de données** (noms de produits, sites, clients, etc.)
2. **Garder les placeholders intacts** ({name}, {count}, etc.)
3. **L'anglais est la langue par défaut** si une langue n'est pas trouvée
4. **Tester toutes les langues** après modification de l'interface Strings
