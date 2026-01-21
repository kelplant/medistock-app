import Foundation
import SwiftUI
import shared

struct SitesView: View {
    let sdk: MedistockSDK
    @State private var sites: [Site] = []
    @State private var isPresentingAdd = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            if sites.isEmpty {
                EmptyStateView(
                    title: "Aucun site",
                    message: "Ajoutez votre premier site pour commencer."
                )
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("Sites")) {
                    ForEach(sites, id: \.id) { site in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(site.name)
                                .font(.headline)
                            Text("ID: \(site.id)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Sites")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    isPresentingAdd = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $isPresentingAdd) {
            SiteEditorView { name in
                addSite(name: name)
            }
        }
        .refreshable {
            await loadSites()
        }
        .task {
            await loadSites()
        }
    }

    @MainActor
    private func loadSites() async {
        errorMessage = nil
        sdk.siteRepository.getAll { result, error in
            DispatchQueue.main.async {
                if let error {
                    errorMessage = "Erreur lors du chargement: \(error.localizedDescription)"
                    sites = []
                    return
                }
                sites = result as? [Site] ?? []
            }
        }
    }

    private func addSite(name: String) {
        let newSite = sdk.createSite(name: name)
        sdk.siteRepository.insert(site: newSite) { _, error in
            DispatchQueue.main.async {
                if let error {
                    errorMessage = "Erreur lors de l'ajout: \(error.localizedDescription)"
                    return
                }
                Task { await loadSites() }
            }
        }
    }
}

struct ProductsView: View {
    let sdk: MedistockSDK
    @State private var products: [Product] = []
    @State private var sites: [Site] = []
    @State private var isPresentingAdd = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            if products.isEmpty {
                EmptyStateView(
                    title: "Aucun produit",
                    message: "Ajoutez un produit pour commencer votre inventaire."
                )
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("Produits")) {
                    ForEach(products, id: \.id) { product in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(product.name)
                                .font(.headline)
                            Text("Site: \(siteName(for: product.siteId))")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Text("Unité: \(product.unit) • Volume: \(String(format: "%.2f", product.unitVolume))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Produits")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    isPresentingAdd = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $isPresentingAdd) {
            ProductEditorView(sites: sites) { name, unit, volume, siteId in
                addProduct(name: name, unit: unit, volume: volume, siteId: siteId)
            }
        }
        .refreshable {
            await loadProducts()
        }
        .task {
            await loadProducts()
        }
    }

    @MainActor
    private func loadProducts() async {
        errorMessage = nil
        sdk.siteRepository.getAll { siteResult, siteError in
            DispatchQueue.main.async {
                if let siteError {
                    errorMessage = "Erreur lors du chargement des sites: \(siteError.localizedDescription)"
                    sites = []
                } else {
                    sites = siteResult as? [Site] ?? []
                }
            }
        }

        sdk.productRepository.getAll { result, error in
            DispatchQueue.main.async {
                if let error {
                    errorMessage = "Erreur lors du chargement: \(error.localizedDescription)"
                    products = []
                    return
                }
                products = result as? [Product] ?? []
            }
        }
    }

    private func addProduct(name: String, unit: String, volume: Double, siteId: String) {
        let newProduct = sdk.createProduct(
            name: name,
            siteId: siteId,
            unit: unit,
            unitVolume: volume
        )
        sdk.productRepository.insert(product: newProduct) { _, error in
            DispatchQueue.main.async {
                if let error {
                    errorMessage = "Erreur lors de l'ajout: \(error.localizedDescription)"
                    return
                }
                Task { await loadProducts() }
            }
        }
    }

    private func siteName(for siteId: String) -> String {
        sites.first { $0.id == siteId }?.name ?? "Inconnu"
    }
}

struct SiteEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    let onSave: (String) -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom du site", text: $name)
                }
            }
            .navigationTitle("Nouveau site")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Ajouter") {
                        onSave(name.trimmingCharacters(in: .whitespacesAndNewlines))
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

struct ProductEditorView: View {
    @Environment(\.dismiss) private var dismiss
    let sites: [Site]
    let onSave: (String, String, Double, String) -> Void

    @State private var name: String = ""
    @State private var unit: String = "unité"
    @State private var volumeText: String = "1"
    @State private var selectedSiteId: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Produit")) {
                    TextField("Nom", text: $name)
                    TextField("Unité", text: $unit)
                    TextField("Volume", text: $volumeText)
                        .keyboardType(.decimalPad)
                }

                Section(header: Text("Site")) {
                    if sites.isEmpty {
                        Text("Ajoutez d'abord un site.")
                            .foregroundColor(.secondary)
                    } else {
                        Picker("Site", selection: $selectedSiteId) {
                            ForEach(sites, id: \.id) { site in
                                Text(site.name).tag(site.id)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Nouveau produit")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Ajouter") {
                        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        let trimmedUnit = unit.trimmingCharacters(in: .whitespacesAndNewlines)
                        let volume = Double(volumeText.replacingOccurrences(of: ",", with: ".")) ?? 1.0
                        onSave(trimmedName, trimmedUnit.isEmpty ? "unité" : trimmedUnit, volume, selectedSiteId)
                        dismiss()
                    }
                    .disabled(!canSave)
                }
            }
            .onAppear {
                if selectedSiteId.isEmpty {
                    selectedSiteId = sites.first?.id ?? ""
                }
            }
        }
    }

    private var canSave: Bool {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmedName.isEmpty && !selectedSiteId.isEmpty
    }
}

struct EmptyStateView: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }
}

struct SalesView: View {
    var body: some View {
        FeatureUnavailableView(title: "Ventes", message: "Les ventes seront disponibles prochainement dans l'application iOS.")
    }
}

struct PurchasesView: View {
    var body: some View {
        FeatureUnavailableView(title: "Achats", message: "La gestion des achats est en cours d'intégration pour iOS.")
    }
}

struct TransfersView: View {
    var body: some View {
        FeatureUnavailableView(title: "Transferts", message: "Les transferts inter-sites seront disponibles prochainement sur iOS.")
    }
}

struct StockView: View {
    var body: some View {
        FeatureUnavailableView(title: "Stock", message: "Le suivi du stock est en cours d'intégration dans l'app iOS.")
    }
}

struct AuthView: View {
    var body: some View {
        FeatureUnavailableView(title: "Authentification", message: "La connexion et la gestion des sessions seront disponibles prochainement sur iOS.")
    }
}

struct AdminView: View {
    var body: some View {
        FeatureUnavailableView(title: "Administration", message: "Les outils d'administration sont en cours de migration vers iOS.")
    }
}

struct SupabaseView: View {
    var body: some View {
        FeatureUnavailableView(title: "Supabase", message: "La configuration Supabase sera disponible dans une prochaine version iOS.")
    }
}

struct PasswordManagementView: View {
    var body: some View {
        FeatureUnavailableView(title: "Gestion du mot de passe", message: "La gestion des mots de passe sera disponible prochainement sur iOS.")
    }
}

struct FeatureUnavailableView: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "hammer")
                .font(.system(size: 48))
                .foregroundColor(.orange)
            Text(title)
                .font(.title2)
                .fontWeight(.semibold)
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Spacer()
        }
        .padding()
        .navigationTitle(title)
    }
}
