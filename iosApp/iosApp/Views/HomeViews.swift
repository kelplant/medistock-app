import Foundation
import SwiftUI
import shared

// MARK: - Home View
struct HomeView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var permissions = PermissionManager.shared
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @State private var selectedSite: Site?
    @State private var sites: [Site] = []
    @State private var showSiteSelector = false
    @State private var showProfileSheet = false

    var body: some View {
        List {
            // Sync status banner (if issues)
            if syncStatus.hasIssues || syncStatus.pendingCount > 0 {
                Section {
                    SyncStatusBannerView(sdk: sdk)
                }
            }

            // Site selector section
            Section(header: Text("Current Site")) {
                Button(action: { showSiteSelector = true }) {
                    HStack {
                        Image(systemName: "building.2")
                            .foregroundColor(.accentColor)
                        Text(selectedSite?.name ?? "Select a site")
                            .foregroundColor(selectedSite == nil ? .secondary : .primary)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
            }

            // Main operations - with permission checks
            Section(header: Text("Operations")) {
                if permissions.canView(.purchases) {
                    NavigationLink(destination: PurchasesListView(sdk: sdk, session: session, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "shippingbox.fill", title: "Purchase Products", color: .blue)
                    }
                }

                if permissions.canView(.sales) {
                    NavigationLink(destination: SalesListView(sdk: sdk, session: session, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "cart.fill", title: "Sell Products", color: .green)
                    }
                }

                if permissions.canView(.transfers) {
                    NavigationLink(destination: TransfersListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "arrow.left.arrow.right", title: "Transfer Products", color: .orange)
                    }
                }

                if permissions.canView(.stock) {
                    NavigationLink(destination: StockListView(sdk: sdk, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "chart.bar.fill", title: "View Stock", color: .purple)
                    }
                }

                if permissions.canView(.inventory) {
                    NavigationLink(destination: InventoryListView(sdk: sdk, session: session, siteId: selectedSite?.id)) {
                        HomeMenuRow(icon: "list.clipboard.fill", title: "Inventory Stock", color: .teal)
                    }
                }
            }

            // Administration - only show if user has any admin permissions
            if session.isAdmin || permissions.canView(.admin) {
                Section(header: Text("Administration")) {
                    NavigationLink(destination: AdminMenuView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "gearshape.fill", title: "Administration", color: .gray)
                    }
                    .accessibilityIdentifier("admin-menu-link")
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
            // Start sync scheduler
            SyncScheduler.shared.start(sdk: sdk)

            // Load sites (online-first)
            await loadSites()

            // Restore selected site
            if selectedSite == nil, let siteId = session.currentSiteId {
                selectedSite = sites.first { $0.id == siteId }
            }
            if selectedSite == nil {
                selectedSite = sites.first
            }
        }
        .onChange(of: selectedSite) { newSite in
            session.currentSiteId = newSite?.id
        }
        .refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            await loadSites()
        }
    }

    @MainActor
    private func loadSites() async {
        // Online-first: try Supabase first
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteSites: [SiteDTO] = try await SupabaseService.shared.fetchAll(from: "sites")
                // Sync to local using upsert (INSERT OR REPLACE)
                for dto in remoteSites {
                    try? await sdk.siteRepository.upsert(site: dto.toEntity())
                }
            } catch {
                debugLog("HomeView", "Failed to fetch sites from Supabase: \(error)")
            }
        }

        // Load from local (always have local data)
        do {
            sites = try await sdk.siteRepository.getAll()
        } catch {
            sites = []
        }
    }
}

// MARK: - Sync Status Banner
struct SyncStatusBannerView: View {
    let sdk: MedistockSDK
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @ObservedObject private var syncManager = BidirectionalSyncManager.shared

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: statusIcon)
                .foregroundColor(statusColor)

            VStack(alignment: .leading, spacing: 2) {
                Text(statusTitle)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(statusSubtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            if syncStatus.isSyncing {
                ProgressView()
                    .scaleEffect(0.8)
            } else if syncStatus.isOnline && syncStatus.pendingCount > 0 {
                Button(action: sync) {
                    Image(systemName: "arrow.clockwise")
                        .foregroundColor(.accentColor)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private var statusIcon: String {
        if !syncStatus.isOnline {
            return "wifi.slash"
        } else if syncStatus.conflictCount > 0 {
            return "exclamationmark.triangle.fill"
        } else if syncStatus.pendingCount > 0 {
            return "arrow.triangle.2.circlepath"
        } else {
            return "checkmark.circle.fill"
        }
    }

    private var statusColor: Color {
        if !syncStatus.isOnline {
            return .gray
        } else if syncStatus.conflictCount > 0 {
            return .red
        } else if syncStatus.pendingCount > 0 {
            return .orange
        } else {
            return .green
        }
    }

    private var statusTitle: String {
        if !syncStatus.isOnline {
            return "Offline Mode"
        } else if syncStatus.conflictCount > 0 {
            return "Conflicts Detected"
        } else if syncStatus.pendingCount > 0 {
            return "Pending Changes"
        } else {
            return "Synchronized"
        }
    }

    private var statusSubtitle: String {
        if !syncStatus.isOnline {
            return "Changes will sync automatically when online"
        } else if syncStatus.conflictCount > 0 {
            return "\(syncStatus.conflictCount) conflict(s) to resolve"
        } else if syncStatus.pendingCount > 0 {
            return "\(syncStatus.pendingCount) pending change(s)"
        } else {
            return syncStatus.statusSummary
        }
    }

    private func sync() {
        Task {
            await syncManager.fullSync(sdk: sdk)
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
            // Management section - order aligned with Android
            Section(header: Text("Management")) {
                // 1. Site Management (Android #1)
                if permissions.canView(.sites) {
                    NavigationLink(destination: SitesListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "building.2.fill", title: "Site Management", color: .blue)
                    }
                    .accessibilityIdentifier("sites-link")
                }

                // 2. Manage Products (Android #2 - submenu in Android, flat here)
                if permissions.canView(.products) {
                    NavigationLink(destination: ProductsListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "cube.box.fill", title: "Manage Products", color: .green)
                    }
                    .accessibilityIdentifier("products-link")
                }

                // 2b. Manage Categories (part of Android #2 submenu)
                if permissions.canView(.categories) {
                    NavigationLink(destination: CategoriesListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "folder.fill", title: "Manage Categories", color: .orange)
                    }
                    .accessibilityIdentifier("categories-link")
                }

                // 3. Stock Movement (Android #3 - moved up from History section)
                if permissions.canView(.stock) {
                    NavigationLink(destination: StockMovementsListView(sdk: sdk)) {
                        HomeMenuRow(icon: "arrow.up.arrow.down", title: "Stock Movement", color: .cyan)
                    }
                    .accessibilityIdentifier("stock-movement-link")
                }

                // 4. Packaging Types (Android #4)
                if permissions.canView(.packagingTypes) {
                    NavigationLink(destination: PackagingTypesListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "shippingbox", title: "Packaging Types", color: .teal)
                    }
                    .accessibilityIdentifier("packaging-types-link")
                }

                // 5. Manage Customers (Android #5 - now in both platforms)
                if permissions.canView(.customers) {
                    NavigationLink(destination: CustomersListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "person.2.fill", title: "Manage Customers", color: .purple)
                    }
                    .accessibilityIdentifier("customers-link")
                }
            }

            // Users section (Android #6)
            if permissions.canView(.users) {
                Section(header: Text("Users")) {
                    NavigationLink(destination: UsersListView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "person.3.fill", title: "User Management", color: .indigo)
                    }
                    .accessibilityIdentifier("users-link")
                }
            }

            // History section (Android #6 - Audit only, Stock Movement moved to Management)
            if permissions.canView(.audit) {
                Section(header: Text("History")) {
                    NavigationLink(destination: AuditHistoryListView(sdk: sdk)) {
                        HomeMenuRow(icon: "clock.arrow.circlepath", title: "Audit History", color: .brown)
                    }
                    .accessibilityIdentifier("audit-link")
                }
            }

            // Configuration section (Android #7)
            Section(header: Text("Configuration")) {
                NavigationLink(destination: SupabaseConfigView(sdk: sdk)) {
                    HomeMenuRow(icon: "server.rack", title: "Supabase Configuration", color: .mint)
                }
                .accessibilityIdentifier("supabase-config-link")
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
    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var sites: [Site] = []
    @State private var isLoading = true

    var body: some View {
        NavigationView {
            List {
                if isLoading {
                    ProgressView()
                } else if sites.isEmpty {
                    Text("No sites available")
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
            .navigationTitle("Select a Site")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
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

        // Online-first
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteSites: [SiteDTO] = try await SupabaseService.shared.fetchAll(from: "sites")
                // Sync to local using upsert (INSERT OR REPLACE)
                for dto in remoteSites {
                    try? await sdk.siteRepository.upsert(site: dto.toEntity())
                }
            } catch {
                debugLog("SiteSelectorView", "Failed to fetch sites from Supabase: \(error)")
            }
        }

        // Load from local
        do {
            sites = try await sdk.siteRepository.getAll()
        } catch {
            sites = []
        }

        isLoading = false
    }
}
