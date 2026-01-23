import Foundation
import SwiftUI
import shared

// MARK: - Users List View
struct UsersListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared

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
                        title: "No users",
                        message: "Add users to manage access."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(users.count) user(s)")) {
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
        .navigationTitle("Users")
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
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
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

        // Online-first: try Supabase first, then sync to local
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteUsers: [UserDTO] = try await SupabaseService.shared.fetchAll(from: "app_users")
                // Sync to local database using upsert (INSERT OR REPLACE)
                for dto in remoteUsers {
                    try? await sdk.userRepository.upsert(user: dto.toEntity())
                }
            } catch {
                debugLog("UsersListView", "Failed to sync users from Supabase: \(error)")
            }
        }

        do {
            users = try await sdk.userRepository.getAll()
        } catch {
            errorMessage = "Error: \(error.localizedDescription)"
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
                            Text("Inactive")
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
                    Label("Edit", systemImage: "pencil")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .tint(.blue)

                if canManagePermissions && !user.isAdmin {
                    Button(action: onPermissions) {
                        Label("Permissions", systemImage: "lock.shield")
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

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var fullName: String = ""
    @State private var isAdmin = false
    @State private var isActive = true
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { user != nil }

    // Password validation computed properties
    private var passwordValidation: PasswordPolicy.ValidationResult {
        PasswordPolicy.shared.validate(password: password)
    }

    private var passwordStrength: PasswordPolicy.PasswordStrength {
        PasswordPolicy.shared.getStrength(password: password)
    }

    private var passwordRequirements: [PasswordPolicy.PasswordError: KotlinBoolean] {
        PasswordPolicy.shared.checkRequirements(password: password)
    }

    private var strengthColor: Color {
        let rgb = passwordStrength.toRGB()
        return Color(red: Double(rgb.first!.intValue) / 255.0,
                     green: Double(rgb.second!.intValue) / 255.0,
                     blue: Double(rgb.third!.intValue) / 255.0)
    }

    private var strengthProgress: Double {
        Double(passwordStrength.toProgress()) / 100.0
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(Localized.information)) {
                    TextField(Localized.username, text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .disabled(isEditing)

                    TextField(Localized.fullName, text: $fullName)
                }

                if !isEditing {
                    Section(header: Text(Localized.password)) {
                        SecureField(Localized.password, text: $password)

                        // Password strength indicator
                        if !password.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(Localized.strings.passwordStrength)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text(PasswordPolicy.shared.getStrengthLabel(strength: passwordStrength, strings: Localized.strings))
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .foregroundColor(strengthColor)
                                }

                                GeometryReader { geometry in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(Color.gray.opacity(0.3))
                                            .frame(height: 8)

                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(strengthColor)
                                            .frame(width: geometry.size.width * strengthProgress, height: 8)
                                    }
                                }
                                .frame(height: 8)
                            }
                            .padding(.vertical, 4)
                        }
                    }

                    // Password requirements card
                    Section(header: Text(Localized.strings.passwordRequirements)) {
                        VStack(alignment: .leading, spacing: 8) {
                            PasswordRequirementRow(
                                text: Localized.strings.passwordMinLength,
                                isMet: passwordRequirements[PasswordPolicy.PasswordError.tooShort]?.boolValue ?? false
                            )
                            PasswordRequirementRow(
                                text: Localized.strings.passwordNeedsUppercase,
                                isMet: passwordRequirements[PasswordPolicy.PasswordError.missingUppercase]?.boolValue ?? false
                            )
                            PasswordRequirementRow(
                                text: Localized.strings.passwordNeedsLowercase,
                                isMet: passwordRequirements[PasswordPolicy.PasswordError.missingLowercase]?.boolValue ?? false
                            )
                            PasswordRequirementRow(
                                text: Localized.strings.passwordNeedsDigit,
                                isMet: passwordRequirements[PasswordPolicy.PasswordError.missingDigit]?.boolValue ?? false
                            )
                            PasswordRequirementRow(
                                text: Localized.strings.passwordNeedsSpecial,
                                isMet: passwordRequirements[PasswordPolicy.PasswordError.missingSpecial]?.boolValue ?? false
                            )
                        }
                    }
                }

                Section(header: Text(Localized.permissions)) {
                    Toggle(Localized.admin, isOn: $isAdmin)
                    Toggle(Localized.active, isOn: $isActive)
                }

                if isEditing {
                    Section(header: Text(Localized.password)) {
                        SecureField(Localized.newPassword, text: $password)

                        // Password strength indicator (only when password is being changed)
                        if !password.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(Localized.strings.passwordStrength)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text(PasswordPolicy.shared.getStrengthLabel(strength: passwordStrength, strings: Localized.strings))
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .foregroundColor(strengthColor)
                                }

                                GeometryReader { geometry in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(Color.gray.opacity(0.3))
                                            .frame(height: 8)

                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(strengthColor)
                                            .frame(width: geometry.size.width * strengthProgress, height: 8)
                                    }
                                }
                                .frame(height: 8)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? Localized.editUser : Localized.addUser)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(Localized.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? Localized.save : Localized.add) {
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
            // For editing, password is optional but if provided must be valid
            if !password.isEmpty && !passwordValidation.isValid {
                return false
            }
            return !trimmedUsername.isEmpty && !trimmedFullName.isEmpty
        } else {
            // For new users, password is required and must be valid
            return !trimmedUsername.isEmpty && !trimmedFullName.isEmpty && !password.isEmpty && passwordValidation.isValid
        }
    }

    private func saveUser() {
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedFullName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)

        // Validate password complexity for new users or when password is being changed
        if !password.isEmpty && !passwordValidation.isValid {
            if let firstError = passwordValidation.errors.first {
                errorMessage = PasswordPolicy.shared.getErrorMessage(error: firstError, strings: Localized.strings)
            }
            return
        }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let isNew = user == nil
                var savedUser: User

                if let existingUser = user {
                    // Update user - hash password if changed
                    let finalPassword: String
                    if password.isEmpty {
                        finalPassword = existingUser.password
                    } else {
                        // Hash password with BCrypt - fail if hashing fails
                        guard let hashedPassword = PasswordHasher.shared.hashPassword(password) else {
                            throw UserEditorError.hashingFailed
                        }
                        finalPassword = hashedPassword
                    }

                    savedUser = User(
                        id: existingUser.id,
                        username: trimmedUsername,
                        password: finalPassword,
                        fullName: trimmedFullName,
                        isAdmin: isAdmin,
                        isActive: isActive,
                        createdAt: existingUser.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingUser.createdBy,
                        updatedBy: session.userId
                    )
                } else {
                    // Create new user - hash password with BCrypt
                    guard let hashedPassword = PasswordHasher.shared.hashPassword(password) else {
                        throw UserEditorError.hashingFailed
                    }

                    savedUser = sdk.createUser(
                        username: trimmedUsername,
                        password: hashedPassword,
                        fullName: trimmedFullName,
                        isAdmin: isAdmin,
                        userId: session.userId
                    )
                }

                let dto = UserDTO(from: savedUser)

                // Online-first: try Supabase first if configured
                var savedOnline = false
                if syncStatus.isOnline && SupabaseService.shared.isConfigured {
                    do {
                        try await SupabaseService.shared.upsert(into: "app_users", record: dto)
                        savedOnline = true
                    } catch {
                        debugLog("UserEditorView", "Failed to save to Supabase: \(error)")
                    }
                }

                // Sync to local database
                if isNew {
                    try await sdk.userRepository.insert(user: savedUser)
                } else {
                    try await sdk.userRepository.update(user: savedUser)
                }

                // Queue for sync if not saved online
                if !savedOnline {
                    if isNew {
                        SyncQueueHelper.shared.enqueueInsert(
                            entityType: .user,
                            entityId: savedUser.id,
                            entity: dto,
                            userId: session.userId
                        )
                    } else {
                        SyncQueueHelper.shared.enqueueUpdate(
                            entityType: .user,
                            entityId: savedUser.id,
                            entity: dto,
                            userId: session.userId,
                            lastKnownRemoteUpdatedAt: user?.updatedAt
                        )
                    }
                }

                await MainActor.run {
                    onSave()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isSaving = false
                    errorMessage = "Error: \(error.localizedDescription)"
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
                        Text("This user is an administrator and has all permissions.")
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
                        Toggle("View", isOn: binding(for: module, action: \.canView))
                        Toggle("Create", isOn: binding(for: module, action: \.canCreate))
                        Toggle("Edit", isOn: binding(for: module, action: \.canEdit))
                        Toggle("Delete", isOn: binding(for: module, action: \.canDelete))
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
        .navigationTitle("\(user.fullName)'s Permissions")
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Close") { dismiss() }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: savePermissions) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Text("Save")
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
                    successMessage = "Permissions saved successfully"
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

// MARK: - User Editor Errors
enum UserEditorError: LocalizedError {
    case hashingFailed

    var errorDescription: String? {
        switch self {
        case .hashingFailed:
            return "Error hashing password"
        }
    }
}

extension User: @retroactive Identifiable {}
