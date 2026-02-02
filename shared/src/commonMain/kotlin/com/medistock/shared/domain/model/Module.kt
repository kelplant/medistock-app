package com.medistock.shared.domain.model

/**
 * Unified module enum for permission management.
 * Used across Android and iOS platforms.
 */
enum class Module(val displayName: String) {
    STOCK("Stock"),
    SALES("Ventes"),
    PURCHASES("Achats"),
    INVENTORY("Inventaire"),
    TRANSFERS("Transferts"),
    ADMIN("Administration"),
    PRODUCTS("Produits"),
    SITES("Sites"),
    CATEGORIES("Catégories"),
    USERS("Utilisateurs"),
    CUSTOMERS("Clients"),
    SUPPLIERS("Fournisseurs"),
    AUDIT("Audit"),
    PACKAGING_TYPES("Types d'emballage");

    companion object {
        /**
         * Get a module by its name (case-insensitive).
         * Returns null if not found.
         */
        fun fromName(name: String): Module? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }

        /**
         * Get all module names as strings.
         */
        fun allNames(): List<String> = entries.map { it.name }
    }
}
