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
                        UserRowView(user: user)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                userToEdit = user
                            }
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

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(user.fullName)
                    .font(.headline)
                Spacer()
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

extension User: @retroactive Identifiable {}
