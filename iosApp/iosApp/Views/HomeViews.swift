import Foundation
import SwiftUI
import shared

// MARK: - Home View
struct HomeView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var permissions = PermissionManager.shared
    @State private var selectedSite: Site?
    @State private var sites: [Site] = []
    @State private var showSiteSelector = false

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

            // Supabase status
            Section {
                HStack {
                    Circle()
                        .fill(SupabaseClient.shared.isConfigured ? Color.green : Color.orange)
                        .frame(width: 8, height: 8)
                    Text(SupabaseClient.shared.isConfigured ? "Supabase connecté" : "Mode hors-ligne")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    if !session.isAdmin {
                        Text("Droits limités")
                            .font(.caption)
                            .foregroundColor(.orange)
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
                Menu {
                    Text("Connecté: \(session.fullName.isEmpty ? session.username : session.fullName)")
                    if session.isAdmin {
                        Text("(Administrateur)")
                    }
                    Divider()
                    Button(role: .destructive) {
                        session.logout()
                    } label: {
                        Label("Déconnexion", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                } label: {
                    Image(systemName: "person.circle")
                }
            }
        }
        .sheet(isPresented: $showSiteSelector) {
            SiteSelectorView(sdk: sdk, selectedSite: $selectedSite)
        }
        .task {
            await loadSites()
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

                // Permission management (admin only)
                if session.isAdmin {
                    NavigationLink(destination: PermissionsManagementView(sdk: sdk, session: session)) {
                        HomeMenuRow(icon: "lock.shield.fill", title: "Gestion des droits", color: .red)
                    }
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

// MARK: - Permissions Management View
struct PermissionsManagementView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @State private var users: [User] = []
    @State private var selectedUser: User?
    @State private var isLoading = true

    var body: some View {
        List {
            if isLoading {
                ProgressView()
            } else if users.isEmpty {
                Text("Aucun utilisateur")
                    .foregroundColor(.secondary)
            } else {
                ForEach(users, id: \.id) { user in
                    NavigationLink(destination: UserPermissionsEditView(sdk: sdk, user: user)) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(user.fullName)
                                    .font(.headline)
                                Text(user.username)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            if user.isAdmin {
                                Text("Admin")
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.blue.opacity(0.2))
                                    .foregroundColor(.blue)
                                    .cornerRadius(4)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Gestion des droits")
        .task {
            await loadUsers()
        }
    }

    @MainActor
    private func loadUsers() async {
        isLoading = true
        do {
            users = try await sdk.userRepository.getAll()
        } catch {
            users = []
        }
        isLoading = false
    }
}

// MARK: - User Permissions Edit View
struct UserPermissionsEditView: View {
    let sdk: MedistockSDK
    let user: User
    @State private var permissions: [UserPermission] = []
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if user.isAdmin {
                Section {
                    HStack {
                        Image(systemName: "info.circle")
                            .foregroundColor(.blue)
                        Text("Cet utilisateur est administrateur et a tous les droits.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }

            if isLoading {
                ProgressView()
            } else {
                ForEach(Module.allCases, id: \.rawValue) { module in
                    Section(header: Text(module.displayName)) {
                        let permission = getPermission(for: module)
                        Toggle("Voir", isOn: binding(for: module, action: \.canView))
                        Toggle("Créer", isOn: binding(for: module, action: \.canCreate))
                        Toggle("Modifier", isOn: binding(for: module, action: \.canEdit))
                        Toggle("Supprimer", isOn: binding(for: module, action: \.canDelete))
                    }
                    .disabled(user.isAdmin)
                }
            }

            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }
        }
        .navigationTitle("Droits de \(user.fullName)")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: savePermissions) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Text("Enregistrer")
                    }
                }
                .disabled(isSaving || user.isAdmin)
            }
        }
        .task {
            await loadPermissions()
        }
    }

    private func getPermission(for module: Module) -> UserPermission? {
        permissions.first { $0.module == module.rawValue }
    }

    private func binding(for module: Module, action: KeyPath<UserPermission, Bool>) -> Binding<Bool> {
        Binding(
            get: {
                getPermission(for: module)?[keyPath: action] ?? false
            },
            set: { newValue in
                updatePermission(for: module, action: action, value: newValue)
            }
        )
    }

    private func updatePermission(for module: Module, action: KeyPath<UserPermission, Bool>, value: Bool) {
        if let index = permissions.firstIndex(where: { $0.module == module.rawValue }) {
            var permission = permissions[index]
            switch action {
            case \.canView:
                permission = UserPermission(
                    id: permission.id, userId: permission.userId, module: permission.module,
                    canView: value, canCreate: permission.canCreate,
                    canEdit: permission.canEdit, canDelete: permission.canDelete,
                    createdAt: permission.createdAt, updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    createdBy: permission.createdBy, updatedBy: SessionManager.shared.userId
                )
            case \.canCreate:
                permission = UserPermission(
                    id: permission.id, userId: permission.userId, module: permission.module,
                    canView: permission.canView, canCreate: value,
                    canEdit: permission.canEdit, canDelete: permission.canDelete,
                    createdAt: permission.createdAt, updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    createdBy: permission.createdBy, updatedBy: SessionManager.shared.userId
                )
            case \.canEdit:
                permission = UserPermission(
                    id: permission.id, userId: permission.userId, module: permission.module,
                    canView: permission.canView, canCreate: permission.canCreate,
                    canEdit: value, canDelete: permission.canDelete,
                    createdAt: permission.createdAt, updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    createdBy: permission.createdBy, updatedBy: SessionManager.shared.userId
                )
            case \.canDelete:
                permission = UserPermission(
                    id: permission.id, userId: permission.userId, module: permission.module,
                    canView: permission.canView, canCreate: permission.canCreate,
                    canEdit: permission.canEdit, canDelete: value,
                    createdAt: permission.createdAt, updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                    createdBy: permission.createdBy, updatedBy: SessionManager.shared.userId
                )
            default:
                break
            }
            permissions[index] = permission
        } else {
            // Create new permission
            let permission = UserPermission(
                userId: user.id,
                module: module.rawValue,
                canView: action == \.canView ? value : false,
                canCreate: action == \.canCreate ? value : false,
                canEdit: action == \.canEdit ? value : false,
                canDelete: action == \.canDelete ? value : false,
                createdBy: SessionManager.shared.userId,
                updatedBy: SessionManager.shared.userId
            )
            permissions.append(permission)
        }
    }

    @MainActor
    private func loadPermissions() async {
        isLoading = true
        do {
            permissions = try await PermissionManager.shared.getPermissions(forUserId: user.id)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func savePermissions() {
        isSaving = true
        errorMessage = nil

        Task {
            do {
                for permission in permissions {
                    try await PermissionManager.shared.savePermission(permission)
                }
                await MainActor.run {
                    isSaving = false
                }
            } catch {
                await MainActor.run {
                    isSaving = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
}
