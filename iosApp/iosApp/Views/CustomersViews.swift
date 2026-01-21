import Foundation
import SwiftUI
import shared

// MARK: - Customers List View
struct CustomersListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @ObservedObject private var syncStatus = SyncStatusManager.shared

    @State private var customers: [Customer] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var customerToEdit: Customer?
    @State private var searchText = ""

    var filteredCustomers: [Customer] {
        if searchText.isEmpty {
            return customers
        }
        return customers.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

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
            } else if customers.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "person.2",
                        title: "Aucun client",
                        message: "Ajoutez vos clients pour faciliter les ventes."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(filteredCustomers.count) client(s)")) {
                    ForEach(filteredCustomers, id: \.id) { customer in
                        CustomerRowView(customer: customer)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                customerToEdit = customer
                            }
                    }
                    .onDelete(perform: deleteCustomers)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Clients")
        .searchable(text: $searchText, prompt: "Rechercher un client")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            CustomerEditorView(sdk: sdk, session: session, customer: nil) {
                Task { await loadCustomers() }
            }
        }
        .sheet(item: $customerToEdit) { customer in
            CustomerEditorView(sdk: sdk, session: session, customer: customer) {
                Task { await loadCustomers() }
            }
        }
        .refreshable {
            await BidirectionalSyncManager.shared.fullSync(sdk: sdk)
            await loadCustomers()
        }
        .task {
            await loadCustomers()
        }
    }

    @MainActor
    private func loadCustomers() async {
        isLoading = true
        errorMessage = nil

        // Online-first: try Supabase first, then sync to local
        if syncStatus.isOnline && SupabaseService.shared.isConfigured {
            do {
                let remoteCustomers: [CustomerDTO] = try await SupabaseService.shared.fetchAll(from: "customers")
                for dto in remoteCustomers {
                    let entity = dto.toEntity()
                    let existing = try? await sdk.customerRepository.getById(id: dto.id)
                    if existing != nil {
                        try? await sdk.customerRepository.update(customer: entity)
                    } else {
                        try? await sdk.customerRepository.insert(customer: entity)
                    }
                }
            } catch {
                print("[CustomersListView] Failed to sync customers from Supabase: \(error)")
            }
        }

        // Load from local database
        do {
            customers = try await sdk.customerRepository.getAll()
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
            customers = []
        }
        isLoading = false
    }

    private func deleteCustomers(at offsets: IndexSet) {
        let customersToDelete = offsets.map { filteredCustomers[$0] }
        Task {
            for customer in customersToDelete {
                do {
                    try await OnlineFirstHelper.shared.delete(
                        table: "customers",
                        entityType: .customer,
                        entityId: customer.id,
                        userId: session.userId
                    ) {
                        try await sdk.customerRepository.delete(id: customer.id)
                    }
                } catch {
                    await MainActor.run {
                        errorMessage = "Erreur lors de la suppression: \(error.localizedDescription)"
                    }
                }
            }
            await loadCustomers()
        }
    }
}

// MARK: - Customer Row View
struct CustomerRowView: View {
    let customer: Customer

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(customer.name)
                .font(.headline)

            HStack {
                if let phone = customer.phone, !phone.isEmpty {
                    Label(phone, systemImage: "phone")
                }
                if let email = customer.email, !email.isEmpty {
                    Label(email, systemImage: "envelope")
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let address = customer.address, !address.isEmpty {
                Label(address, systemImage: "location")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Customer Editor View
struct CustomerEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let customer: Customer?
    let onSave: () -> Void

    @ObservedObject private var syncStatus = SyncStatusManager.shared
    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var phone: String = ""
    @State private var email: String = ""
    @State private var address: String = ""
    @State private var notes: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { customer != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom du client", text: $name)
                    TextField("Téléphone", text: $phone)
                        .keyboardType(.phonePad)
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                }

                Section(header: Text("Adresse")) {
                    TextField("Adresse", text: $address)
                }

                Section(header: Text("Notes")) {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier le client" : "Nouveau client")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Enregistrer" : "Ajouter") {
                        saveCustomer()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSaving)
                }
            }
            .onAppear {
                if let customer = customer {
                    name = customer.name
                    phone = customer.phone ?? ""
                    email = customer.email ?? ""
                    address = customer.address ?? ""
                    notes = customer.notes ?? ""
                }
            }
        }
    }

    private func saveCustomer() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                let isNew = customer == nil
                var savedCustomer: Customer

                if let existingCustomer = customer {
                    savedCustomer = Customer(
                        id: existingCustomer.id,
                        name: trimmedName,
                        phone: phone.isEmpty ? nil : phone,
                        email: email.isEmpty ? nil : email,
                        address: address.isEmpty ? nil : address,
                        notes: notes.isEmpty ? nil : notes,
                        createdAt: existingCustomer.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingCustomer.createdBy,
                        updatedBy: session.userId
                    )
                } else {
                    savedCustomer = sdk.createCustomer(
                        name: trimmedName,
                        phone: phone.isEmpty ? nil : phone,
                        email: email.isEmpty ? nil : email,
                        address: address.isEmpty ? nil : address,
                        notes: notes.isEmpty ? nil : notes,
                        userId: session.userId
                    )
                }

                let dto = CustomerDTO(from: savedCustomer)

                try await OnlineFirstHelper.shared.save(
                    table: "customers",
                    dto: dto,
                    entityType: .customer,
                    entityId: savedCustomer.id,
                    isNew: isNew,
                    userId: session.userId,
                    lastKnownRemoteUpdatedAt: customer?.updatedAt
                ) {
                    if isNew {
                        try await sdk.customerRepository.insert(customer: savedCustomer)
                    } else {
                        try await sdk.customerRepository.update(customer: savedCustomer)
                    }
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

extension Customer: @retroactive Identifiable {}
