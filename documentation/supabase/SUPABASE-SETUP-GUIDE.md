# 🚀 Guide de Configuration Supabase pour Medistock

## ✅ Étapes Complétées

- [x] Création du projet Supabase
- [x] Génération du schéma SQL
- [x] Création des tables (17 tables)
- [x] Configuration RLS

---

## 📋 Prochaines Étapes

### **ÉTAPE 3 : Appliquer les politiques RLS (Row Level Security)**

1. Allez sur **Dashboard Supabase** → **SQL Editor**
2. Cliquez sur **"New query"**
3. Copiez tout le contenu du fichier `supabase/rls-policies.sql`
4. Collez et cliquez sur **"RUN"**

**Note** : Les politiques RLS actuelles sont **permissives** (accès total) pour faciliter le développement. Vous pourrez les restreindre plus tard pour la production.

---

### **ÉTAPE 4 : Tester les APIs auto-générées**

Vos APIs REST sont déjà disponibles ! Testez-les :

#### **4.1 - Via l'interface Supabase**

1. Allez dans **Table Editor**
2. Cliquez sur la table `sites`
3. Essayez d'ajouter un nouveau site :
   - Cliquez sur **"Insert" → "Insert row"**
   - Name: `Pharmacie Centre`
   - created_by: `admin`
   - Cliquez sur **"Save"**

#### **4.2 - Via API REST (avec curl ou Postman)**

**Récupérer tous les sites :**
```bash
curl 'https://VOTRE_PROJECT_URL.supabase.co/rest/v1/sites?select=*' \
  -H "apikey: VOTRE_ANON_KEY" \
  -H "Authorization: Bearer VOTRE_ANON_KEY"
```

**Créer une catégorie :**
```bash
curl -X POST 'https://VOTRE_PROJECT_URL.supabase.co/rest/v1/categories' \
  -H "apikey: VOTRE_ANON_KEY" \
  -H "Authorization: Bearer VOTRE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=representation" \
  -d '{"name": "Antipaludiques", "created_by": "admin"}'
```

**Récupérer le stock actuel (vue) :**
```bash
curl 'https://VOTRE_PROJECT_URL.supabase.co/rest/v1/current_stock?select=*' \
  -H "apikey: VOTRE_ANON_KEY" \
  -H "Authorization: Bearer VOTRE_ANON_KEY"
```

---

### **ÉTAPE 5 : Intégrer Supabase dans l'app Android**

#### **5.1 - Ajouter les dépendances**

Dans `app/build.gradle.kts` :

```kotlin
dependencies {
    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")
    implementation("io.ktor:ktor-client-android:2.3.7")

    // Existing dependencies...
}
```

#### **5.2 - Créer le client Supabase**

Créer `app/src/main/java/com/medistock/data/remote/SupabaseClient.kt` :

```kotlin
package com.medistock.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "VOTRE_PROJECT_URL",
        supabaseKey = "VOTRE_ANON_KEY"
    ) {
        install(Postgrest)
        install(Realtime)
    }
}
```

#### **5.3 - Créer un repository Supabase**

Exemple pour les sites :

```kotlin
package com.medistock.data.repository

import com.medistock.data.entities.Site
import com.medistock.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class SiteDto(
    val id: Long = 0,
    val name: String,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val created_by: String = "",
    val updated_by: String = ""
)

class SiteSupabaseRepository {
    private val supabase = SupabaseClient.client

    suspend fun getAllSites(): List<SiteDto> {
        return supabase.from("sites").select().decodeList()
    }

    suspend fun createSite(site: SiteDto): SiteDto {
        return supabase.from("sites").insert(site) {
            select()
        }.decodeSingle()
    }

    suspend fun updateSite(id: Long, site: SiteDto): SiteDto {
        return supabase.from("sites").update(site) {
            filter { eq("id", id) }
            select()
        }.decodeSingle()
    }

    suspend fun deleteSite(id: Long) {
        supabase.from("sites").delete {
            filter { eq("id", id) }
        }
    }
}
```

#### **5.4 - Utiliser dans un ViewModel**

```kotlin
class SiteViewModel : ViewModel() {
    private val repository = SiteSupabaseRepository()

    fun loadSites() {
        viewModelScope.launch {
            try {
                val sites = repository.getAllSites()
                // Update UI state
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
```

---

### **ÉTAPE 6 : Synchronisation temps réel**

Pour recevoir les changements en temps réel :

```kotlin
suspend fun observeSites() {
    supabase.from("sites").realtime().listen {
        when (it) {
            is Realtime.Insert -> {
                // Nouveau site ajouté
                val newSite = it.record.decodeAs<SiteDto>()
            }
            is Realtime.Update -> {
                // Site mis à jour
                val updatedSite = it.record.decodeAs<SiteDto>()
            }
            is Realtime.Delete -> {
                // Site supprimé
                val deletedId = it.oldRecord["id"]
            }
        }
    }
}
```

---

## 🎯 Architecture de Synchronisation

```
┌─────────────────────────────────────┐
│      APP ANDROID (Kotlin)           │
│  ┌──────────────────────────────┐   │
│  │  UI Layer (Compose)          │   │
│  └────────────┬─────────────────┘   │
│               │                      │
│  ┌────────────▼─────────────────┐   │
│  │  ViewModel                   │   │
│  └────────────┬─────────────────┘   │
│               │                      │
│  ┌────────────▼─────────────────┐   │
│  │  Repository Layer            │   │
│  │  ┌────────────────────────┐  │   │
│  │  │ Room (Local Cache)     │  │   │
│  │  └────────────────────────┘  │   │
│  │  ┌────────────────────────┐  │   │
│  │  │ Supabase Repository    │  │   │
│  │  └────────────────────────┘  │   │
│  └────────────┬─────────────────┘   │
└───────────────┼─────────────────────┘
                │ HTTPS / WebSocket
                ▼
┌─────────────────────────────────────┐
│         SUPABASE CLOUD              │
│  ┌──────────────────────────────┐   │
│  │  PostgreSQL (17 tables)      │   │
│  │  + RLS Policies              │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │  Auto REST APIs              │   │
│  │  + Realtime Subscriptions    │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │  Edge Functions (TODO)       │   │
│  │  - FIFO allocation           │   │
│  │  - Business logic            │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 📊 État de la Base de Données

**Tables créées** : 17
- ✅ sites
- ✅ categories
- ✅ packaging_types
- ✅ app_users
- ✅ user_permissions
- ✅ customers
- ✅ products
- ✅ product_prices
- ✅ purchase_batches
- ✅ stock_movements
- ✅ inventories
- ✅ product_transfers
- ✅ sales
- ✅ sale_items
- ✅ sale_batch_allocations
- ✅ product_sales
- ✅ audit_history

**Vues** : 1
- ✅ current_stock (stock en temps réel par produit/site)

**Données initiales** :
- 1 site : "Site Principal"
- 4 catégories
- 3 types de conditionnement
- 1 utilisateur admin (username: `admin`, password: `admin123`)

---

## 🔐 Sécurité

### **Clés Supabase**

- **anon key** : À utiliser dans l'application Android (respecte RLS)
- **service_role key** : À utiliser uniquement côté serveur (bypass RLS) - **NE JAMAIS EXPOSER**

### **Row Level Security (RLS)**

Actuellement : **Politiques permissives** (accès total pour développement)

Pour la production, activez les politiques restrictives dans `supabase-rls-policies.sql` :
- Accès limité par site
- Vérification des permissions utilisateur
- Accès admin complet

---

## 🚀 Prochaines Fonctionnalités

- [ ] Edge Functions pour logique FIFO
- [ ] Système d'authentification complet
- [ ] Synchronisation offline-first
- [ ] Gestion des conflits
- [ ] Webhooks pour audit
- [ ] Rapports et analytics

---

## 📚 Ressources

- [Documentation Supabase](https://supabase.com/docs)
- [Supabase Kotlin Client](https://supabase.com/docs/reference/kotlin/introduction)
- [PostgREST API](https://postgrest.org/en/stable/)
- [Row Level Security](https://supabase.com/docs/guides/auth/row-level-security)

---

**Créé par Claude pour Medistock** 🏥
