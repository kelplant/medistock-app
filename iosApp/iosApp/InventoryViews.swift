import Foundation
import SwiftUI
import shared

struct LoginView: View {
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var errorMessage: String?
    @State private var isShowingSupabase = false

    let onLogin: (String) -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "cross.case.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 80, height: 80)
                .foregroundColor(.blue)

            Text("MediStock")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Connexion")
                .font(.headline)
                .foregroundColor(.secondary)

            VStack(spacing: 12) {
                TextField("Nom d'utilisateur", text: $username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                SecureField("Mot de passe", text: $password)
                    .textFieldStyle(.roundedBorder)
            }

            if let errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .font(.footnote)
            }

            Button("Se connecter") {
                let trimmedUser = username.trimmingCharacters(in: .whitespacesAndNewlines)
                let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmedUser.isEmpty, !trimmedPassword.isEmpty else {
                    errorMessage = "Veuillez renseigner vos identifiants."
                    return
                }
                errorMessage = nil
                onLogin(trimmedUser)
            }
            .buttonStyle(.borderedProminent)

            Button("Configurer Supabase") {
                isShowingSupabase = true
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $isShowingSupabase) {
                SupabaseView()
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Authentification")
    }
}

struct HomeView: View {
    let sdk: MedistockSDK
    let username: String
    let onLogout: () -> Void

    var body: some View {
        List {
            Section {
                NavigationLink(destination: PurchasesView()) {
                    HomeMenuRow(emoji: "📦", title: "Purchase Products")
                }

                NavigationLink(destination: SalesView()) {
                    HomeMenuRow(emoji: "💰", title: "Sell Products")
                }

                NavigationLink(destination: TransfersView()) {
                    HomeMenuRow(emoji: "🔄", title: "Transfer Products")
                }

                NavigationLink(destination: StockView()) {
                    HomeMenuRow(emoji: "📊", title: "View Stock")
                }

                NavigationLink(destination: InventoryView()) {
                    HomeMenuRow(emoji: "📋", title: "Inventory Stock")
                }
            }

            Section(header: Text("Administration")) {
                NavigationLink(destination: AdminMenuView(sdk: sdk)) {
                    HomeMenuRow(emoji: "⚙️", title: "Administration")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("MediStock - \(username)")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Déconnexion") {
                    onLogout()
                }
            }
        }
    }
}

struct HomeMenuRow: View {
    let emoji: String
    let title: String

    var body: some View {
        HStack(spacing: 16) {
            Text(emoji)
                .font(.system(size: 28))
                .frame(width: 32)
            Text(title)
                .font(.headline)
            Spacer()
        }
        .padding(.vertical, 4)
    }
}

struct AdminMenuView: View {
    let sdk: MedistockSDK

    var body: some View {
        List {
            Section {
                NavigationLink(destination: SitesView(sdk: sdk)) {
                    HomeMenuRow(emoji: "📍", title: "Site Management")
                }

                NavigationLink(destination: ProductsView(sdk: sdk)) {
                    HomeMenuRow(emoji: "📦", title: "Manage Products")
                }

                NavigationLink(destination: StockMovementView()) {
                    HomeMenuRow(emoji: "🔄", title: "Stock Movement")
                }

                NavigationLink(destination: PackagingTypesView()) {
                    HomeMenuRow(emoji: "📦", title: "Packaging Types")
                }

                NavigationLink(destination: UserManagementView()) {
                    HomeMenuRow(emoji: "👥", title: "User Management")
                }

                NavigationLink(destination: AuditHistoryView()) {
                    HomeMenuRow(emoji: "🧾", title: "Audit History")
                }

                NavigationLink(destination: SupabaseView()) {
                    HomeMenuRow(emoji: "🔑", title: "Configuration Supabase")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Administration")
    }
}

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
        do {
            let result = try await sdk.siteRepository.getAll()
            sites = result
        } catch {
            errorMessage = "Erreur lors du chargement: \(error.localizedDescription)"
            sites = []
        }
    }

    private func addSite(name: String) {
        let newSite = sdk.createSite(name: name, userId: "ios")
        Task {
            await MainActor.run {
                errorMessage = nil
            }
            do {
                try await sdk.siteRepository.insert(site: newSite)
                await loadSites()
            } catch {
                await MainActor.run {
                    errorMessage = "Erreur lors de l'ajout: \(error.localizedDescription)"
                }
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
        do {
            let siteResult = try await sdk.siteRepository.getAll()
            sites = siteResult
        } catch {
            errorMessage = "Erreur lors du chargement des sites: \(error.localizedDescription)"
            sites = []
        }

        do {
            let result = try await sdk.productRepository.getAll()
            products = result
        } catch {
            errorMessage = "Erreur lors du chargement: \(error.localizedDescription)"
            products = []
        }
    }

    private func addProduct(name: String, unit: String, volume: Double, siteId: String) {
        let newProduct = sdk.createProduct(
            name: name,
            siteId: siteId,
            unit: unit,
            unitVolume: volume,
            userId: "ios"
        )
        Task {
            await MainActor.run {
                errorMessage = nil
            }
            do {
                try await sdk.productRepository.insert(product: newProduct)
                await loadProducts()
            } catch {
                await MainActor.run {
                    errorMessage = "Erreur lors de l'ajout: \(error.localizedDescription)"
                }
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

struct InventoryView: View {
    var body: some View {
        FeatureUnavailableView(
            title: "Inventory Stock",
            message: "L'inventaire sera disponible prochainement dans l'application iOS."
        )
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

struct SupabaseView: View {
    var body: some View {
        FeatureUnavailableView(title: "Supabase", message: "La configuration Supabase sera disponible dans une prochaine version iOS.")
    }
}

struct StockMovementView: View {
    var body: some View {
        FeatureUnavailableView(title: "Stock Movement", message: "Les mouvements de stock seront disponibles prochainement sur iOS.")
    }
}

struct PackagingTypesView: View {
    var body: some View {
        FeatureUnavailableView(title: "Packaging Types", message: "La gestion des conditionnements sera disponible prochainement sur iOS.")
    }
}

struct UserManagementView: View {
    var body: some View {
        FeatureUnavailableView(title: "User Management", message: "La gestion des utilisateurs sera disponible prochainement sur iOS.")
    }
}

struct AuditHistoryView: View {
    var body: some View {
        FeatureUnavailableView(title: "Audit History", message: "L'historique des audits sera disponible prochainement sur iOS.")
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
