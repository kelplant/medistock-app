import Foundation
import SwiftUI
import shared

// MARK: - Home View
struct HomeView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var permissions = PermissionManager.shared
    @ObservedObject private var realtimeService = RealtimeService.shared
    @State private var selectedSite: Site?
    @State private var sites: [Site] = []
    @State private var showSiteSelector = false
    @State private var showProfileSheet = false

    var body: some View {
        List {
            // Site selector section
            Section(header: Text("Site actuel")) {
                Button(action: { showSiteSelector = true }) {
                    HStack {
                        Image(systemName: "building.2")
                            .foregroundColor(.accentColor)
                        Text(selectedSite?.name ?? "Sélectionner un site")
                            .foregroundColor(selectedSite == nil ? .secondary : .primary)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
            }

            // Main operations - with permission checks
            Section(header: Text("Opérations")) {
                if permissions.canView(.purchases) {
                    NavigationLink(destination: PurchasesListView(sdk: sdk, session: session, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "shippingbox.fill", title: "Achats", color: .blue)
                    }
                }

                if permissions.canView(.sales) {
                    NavigationLink(destination: SalesListView(sdk: sdk, session: session, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "cart.fill", title: "Ventes", color: .green)
                    }
                }

                if permissions.canView(.transfers) {
                    NavigationLink(destination: TransfersListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "arrow.left.arrow.right", title: "Transferts", color: .orange)
                    }
                }

                if permissions.canView(.stock) {
                    NavigationLink(destination: StockListView(sdk: sdk, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "chart.bar.fill", title: "Stock", color: .purple)
                    }
                }

                if permissions.canView(.inventory) {
                    NavigationLink(destination: InventoryListView(sdk: sdk, session: session, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "list.clipboard.fill", title: "Inventaire", color: .teal)
                    }
                }
            }

            // Administration - only show if user has any admin permissions
            if session.isAdmin || permissions.canView(.admin) {
                Section(header: Text("Administration")) {
                    NavigationLink(destination: AdminMenuView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "gearshape.fill", title: "Administration", color: .gray)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("MediStock")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showProfileSheet = true }) {
                    ProfileBadgeView(session: session)
                }
            }
        }
        .sheet(isPresented: $showSiteSelector) {
            SiteSelectorView(sdk: sdk, selectedSite: $selectedSite)
        }
        .sheet(isPresented: $showProfileSheet) {
            ProfileMenuView(sdk: sdk, session: session)
        }
        .task {
            await loadSites()
            if selectedSite == nil, let siteId = session.currentSiteId {
                selectedSite = sites.first { $0.id == siteId }
            }
            if selectedSite == nil {
                selectedSite = sites.first
            }

            // Start realtime sync automatically
            if SupabaseClient.shared.isConfigured {
                realtimeService.start(sdk: sdk)
            }
        }
        .onChange(of: selectedSite) { newSite in
            session.currentSiteId = newSite?.id
        }
        .onDisappear {
            // Don't stop realtime when navigating, only when logging out
        }
    }

    @MainActor
    private func loadSites() async {
        do {
            sites = try await sdk.siteRepository.getAll()
        } catch {
            sites = []
        }
    }
}

// MARK: - Home Menu Row
struct HomeMenuRow: View {
    let icon: String
    let title: String
    let color: Color

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(color)
                .frame(width: 32)
            Text(title)
                .font(.headline)
            Spacer()
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Admin Menu View
struct AdminMenuView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var permissions = PermissionManager.shared

    var body: some View {
        List {
            Section(header: Text("Gestion")) {
                if permissions.canView(.sites) {
                    NavigationLink(destination: SitesListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "building.2.fill", title: "Sites", color: .blue)
                    }
                }

                if permissions.canView(.products) {
                    NavigationLink(destination: ProductsListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "cube.box.fill", title: "Produits", color: .green)
                    }
                }

                if permissions.canView(.categories) {
                    NavigationLink(destination: CategoriesListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "folder.fill", title: "Catégories", color: .orange)
                    }
                }

                if permissions.canView(.customers) {
                    NavigationLink(destination: CustomersListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "person.2.fill", title: "Clients", color: .purple)
                    }
                }

                if permissions.canView(.packagingTypes) {
                    NavigationLink(destination: PackagingTypesListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "shippingbox", title: "Conditionnements", color: .teal)
                    }
                }
            }

            // Users section - permissions are managed per user, not globally
            if permissions.canView(.users) {
                Section(header: Text("Utilisateurs")) {
                    NavigationLink(destination: UsersListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "person.3.fill", title: "Utilisateurs", color: .indigo)
                    }
                }
            }

            if permissions.canView(.stock) || permissions.canView(.audit) {
                Section(header: Text("Historique")) {
                    if permissions.canView(.stock) {
                        NavigationLink(destination: StockMovementsListView(sdk: sdk)) {
                            HomeMenuRow(icon: "arrow.up.arrow.down", title: "Mouvements de stock", color: .cyan)
                        }
                    }

                    if permissions.canView(.audit) {
                        NavigationLink(destination: AuditHistoryListView(sdk: sdk)) {
                            HomeMenuRow(icon: "clock.arrow.circlepath", title: "Historique des audits", color: .brown)
                        }
                    }
                }
            }

            Section(header: Text("Configuration")) {
                NavigationLink(destination: SupabaseConfigView(sdk: sdk)) {
                    HomeMenuRow(icon: "server.rack", title: "Configuration Supabase", color: .mint)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Administration")
    }
}

// MARK: - Site Selector View
struct SiteSelectorView: View {
    let sdk: MedistockSDK
    @Binding var selectedSite: Site?
    @Environment(\.dismiss) private var dismiss
    @State private var sites: [Site] = []
    @State private var isLoading = true

    var body: some View {
        NavigationView {
            List {
                if isLoading {
                    ProgressView()
                } else if sites.isEmpty {
                    Text("Aucun site disponible")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(sites, id: \.id) { site in
                        Button(action: {
                            selectedSite = site
                            dismiss()
                        }) {
                            HStack {
                                Text(site.name)
                                Spacer()
                                if site.id == selectedSite?.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                }
                            }
                        }
                        .foregroundColor(.primary)
                    }
                }
            }
            .navigationTitle("Sélectionner un site")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Fermer") { dismiss() }
                }
            }
            .task {
                await loadSites()
            }
        }
    }

    @MainActor
    private func loadSites() async {
        isLoading = true
        do {
            sites = try await sdk.siteRepository.getAll()
        } catch {
            sites = []
        }
        isLoading = false
    }
}
