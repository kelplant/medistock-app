import Foundation
import SwiftUI
import shared

// MARK: - Sites List View
struct SitesListView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    @State private var sites: [Site] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showAddSheet = false
    @State private var siteToEdit: Site?

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
            } else if sites.isEmpty {
                Section {
                    EmptyStateView(
                        icon: "building.2",
                        title: "Aucun site",
                        message: "Ajoutez votre premier site pour commencer."
                    )
                }
                .listRowSeparator(.hidden)
            } else {
                Section(header: Text("\(sites.count) site(s)")) {
                    ForEach(sites, id: \.id) { site in
                        SiteRowView(site: site)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                siteToEdit = site
                            }
                    }
                    .onDelete(perform: deleteSites)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Sites")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            SiteEditorView(sdk: sdk, session: session, site: nil) {
                Task { await loadSites() }
            }
        }
        .sheet(item: $siteToEdit) { site in
            SiteEditorView(sdk: sdk, session: session, site: site) {
                Task { await loadSites() }
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
        isLoading = true
        errorMessage = nil

        // Online-first: try Supabase first, then sync to local
        if SyncManager.shared.isOnline && SupabaseClient.shared.isConfigured {
            do {
                let remoteSites: [RemoteSite] = try await SupabaseClient.shared.fetchAll(from: "sites")
                // Sync to local database
                for remoteSite in remoteSites {
                    let localSite = Site(
                        id: remoteSite.id,
                        name: remoteSite.name,
                        createdAt: remoteSite.createdAt,
                        updatedAt: remoteSite.updatedAt,
                        createdBy: remoteSite.createdBy,
                        updatedBy: remoteSite.updatedBy
                    )
                    try? await sdk.siteRepository.upsert(site: localSite)
                }
                sites = try await sdk.siteRepository.getAll()
                isLoading = false
                return
            } catch {
                // Fall through to local
                print("Failed to fetch from Supabase: \(error)")
            }
        }

        // Fallback to local database
        do {
            sites = try await sdk.siteRepository.getAll()
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
            sites = []
        }
        isLoading = false
    }

    private func deleteSites(at offsets: IndexSet) {
        let sitesToDelete = offsets.map { sites[$0] }
        Task {
            for site in sitesToDelete {
                do {
                    // Online-first: delete from Supabase first
                    if SyncManager.shared.isOnline && SupabaseClient.shared.isConfigured {
                        try await SupabaseClient.shared.delete(from: "sites", id: site.id)
                    }
                    // Then delete locally
                    try await sdk.siteRepository.delete(id: site.id)
                } catch {
                    await MainActor.run {
                        errorMessage = "Erreur lors de la suppression: \(error.localizedDescription)"
                    }
                }
            }
            await loadSites()
        }
    }
}

// MARK: - Site Row View
struct SiteRowView: View {
    let site: Site

    var body: some View {
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

// MARK: - Site Editor View
struct SiteEditorView: View {
    let sdk: MedistockSDK
    @ObservedObject var session: SessionManager
    let site: Site?
    let onSave: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { site != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Informations")) {
                    TextField("Nom du site", text: $name)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier le site" : "Nouveau site")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Enregistrer" : "Ajouter") {
                        saveSite()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSaving)
                }
            }
            .onAppear {
                if let site = site {
                    name = site.name
                }
            }
        }
    }

    private func saveSite() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }

        isSaving = true
        errorMessage = nil

        Task {
            do {
                var savedSite: Site

                if let existingSite = site {
                    // Update
                    savedSite = Site(
                        id: existingSite.id,
                        name: trimmedName,
                        createdAt: existingSite.createdAt,
                        updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
                        createdBy: existingSite.createdBy,
                        updatedBy: session.username
                    )
                } else {
                    // Insert
                    savedSite = sdk.createSite(name: trimmedName, userId: session.username)
                }

                // Online-first: save to Supabase first
                if SyncManager.shared.isOnline && SupabaseClient.shared.isConfigured {
                    let remoteSite = RemoteSite(
                        id: savedSite.id,
                        name: savedSite.name,
                        createdAt: savedSite.createdAt,
                        updatedAt: savedSite.updatedAt,
                        createdBy: savedSite.createdBy,
                        updatedBy: savedSite.updatedBy
                    )
                    _ = try await SupabaseClient.shared.upsert(into: "sites", record: remoteSite)
                }

                // Then sync to local database
                if site != nil {
                    try await sdk.siteRepository.update(site: savedSite)
                } else {
                    try await sdk.siteRepository.insert(site: savedSite)
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

extension Site: @retroactive Identifiable {}
