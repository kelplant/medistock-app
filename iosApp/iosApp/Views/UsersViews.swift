import Foundation
import SwiftUI
import shared

// MARK: - Users List View
struct UsersListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager

    @State private var users: [User] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var userToEdit: User?
    @State private var userForPermissions: User?

    var body: some View {
        List {
            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundColor(.red)
                }
            }

            if isLoading {
                Section {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                }
            } else if users.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "person.3",
                        title: "Aucun utilisateur",
                        message: "Ajoutez des utilisateurs pour gérer les accès."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(users.count) utilisateur(s)")) {
                    ForEach(users, id: \.id) { user in
                        UserRowView(
                            user: user,
                            onEdit: { userToEdit = user },
                            onPermissions: { userForPermissions = user },
                            canManagePermissions: session.isAdmin
                        )
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Utilisateurs")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            UserEditorView(sdk: sdk, session: session, user: nil) {
                Task { await loadUsers() }
            }
        }
        .sheet(item: $userToEdit) { user in
            UserEditorView(sdk: sdk, session: session, user: user) {
                Task { await loadUsers() }
            }
        }
        .sheet(item: $userForPermissions) { user in
            NavigationView {
                UserPermissionsEditView(sdk: sdk, user: user)
            }
        }
        .refreshable {
            await loadUsers()
        }
        .task {
            await loadUsers()
        }
    }

    @MainActor
    private func loadUsers() async {
        isLoading = true
        errorMessage = nil
        do {
            users = try await sdk.userRepository.getAll()
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
            users = []
        }
        isLoading = false
    }
}

// MARK: - User Row View
struct UserRowView: View {
    let user: User
    let onEdit: () -> Void
    let onPermissions: () -> Void
    let canManagePermissions: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(user.fullName)
                            .font(.headline)
                        if user.isAdmin {
                            Text("Admin")
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Color.blue.opacity(0.2))
                                .foregroundColor(.blue)
                                .cornerRadius(4)
                        }
                        if !user.isActive {
                            Text("Inactif")
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Color.red.opacity(0.2))
                                .foregroundColor(.red)
                                .cornerRadius(4)
                        }
                    }
                    Text("@\(user.username)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }

            // Action buttons
            HStack(spacing: 12) {
                Button(action: onEdit) {
                    Label("Modifier", systemImage: "pencil")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .tint(.blue)

                if canManagePermissions && !user.isAdmin {
                    Button(action: onPermissions) {
                        Label("Droits", systemImage: "lock.shield")
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                    .tint(.orange)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - User Editor View
struct UserEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let user: User?
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var fullName: String = ""
    @State private var isAdmin = false
    @State private var isActive = true
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { user != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom d'utilisateur", text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .disabled(isEditing)

                    if !isEditing {
                        SecureField("Mot de passe", text: $password)
                    }

                    TextField("Nom complet", text: $fullName)
                }

                Section(header: Text("Permissions")) {
                    Toggle("Administrateur", isOn: $isAdmin)
                    Toggle("Compte actif", isOn: $isActive)
                }

                if isEditing {
                    Section(header: Text("Mot de passe")) {
                        SecureField("Nouveau mot de passe (laisser vide pour ne pas changer)", text: $password)
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier l'utilisateur" : "Nouvel utilisateur")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Enregistrer" : "Ajouter") {
                        saveUser()
                    }
                    .disabled(!canSave || isSaving)
                }
            }
            .onAppear {
                if let user = user {
                    username = user.username
                    fullName = user.fullName
                    isAdmin = user.isAdmin
                    isActive = user.isActive
                }
            }
        }
    }

    private var canSave: Bool {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedFullName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)

        if isEditing {
            return !trimmedUsername.isEmpty && !trimmedFullName.isEmpty
        } else {
            return !trimmedUsername.isEmpty && !trimmedFullName.isEmpty && !password.isEmpty
        }
    }

    private func saveUser() {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedFullName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)

        isSaving = true
        errorMessage = nil

        Task {
            do {
                if let existingUser = user {
                    // Update user
                    let updated = User(
                        id: existingUser.id,
                        username: trimmedUsername,
                        password: existingUser.password,
                        fullName: trimmedFullName,
                        isAdmin: isAdmin,
                        isActive: isActive,
                        createdAt: existingUser.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingUser.createdBy,
                        updatedBy: session.username
                    )
                    try await sdk.userRepository.update(user: updated)

                    // Update password if provided
                    if !password.isEmpty {
                        try await sdk.userRepository.updatePassword(
                            userId: existingUser.id,
                            password: password, // In production, hash this
                            updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                            updatedBy: session.username
                        )
                    }
                } else {
                    // Create new user
                    let newUser = sdk.createUser(
                        username: trimmedUsername,
                        password: password, // In production, hash this
                        fullName: trimmedFullName,
                        isAdmin: isAdmin,
                        userId: session.username
                    )
                    try await sdk.userRepository.insert(user: newUser)
                }

                await MainActor.run {
                    onSave()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isSaving = false
                    errorMessage = "Erreur: \(error.localizedDescription)"
                }
            }
        }
    }
}

// MARK: - User Permissions Edit View
struct UserPermissionsEditView: View {
    let sdk: MedistockSDK
    let user: User
    @Environment(\.dismiss) private var dismiss
    @State private var permissions: [UserPermission] = []
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var successMessage: String?

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
                Section {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                }
            } else {
                ForEach(Module.allCases, id: \.rawValue) { module in
                    Section(header: Text(module.displayName)) {
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

            if let successMessage {
                Section {
                    Text(successMessage)
                        .foregroundColor(.green)
                }
            }
        }
        .navigationTitle("Droits de \(user.fullName)")
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Fermer") { dismiss() }
            }
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
        errorMessage = nil
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
        successMessage = nil

        Task {
            do {
                for permission in permissions {
                    try await PermissionManager.shared.savePermission(permission)
                }
                await MainActor.run {
                    isSaving = false
                    successMessage = "Permissions enregistrées avec succès"
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

extension User: @retroactive Identifiable {}
