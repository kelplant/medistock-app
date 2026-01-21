import SwiftUI
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @State private var greeting: String = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                // App Logo/Header
                Image(systemName: "cross.case.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 80, height: 80)
                    .foregroundColor(.blue)

                Text("MediStock")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text(greeting)
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Divider()
                    .padding(.vertical)

                // Main Menu
                VStack(spacing: 15) {
                    NavigationLink(destination: AuthView()) {
                        MenuButton(title: "Authentification", icon: "person.badge.key", color: .blue)
                    }

                    NavigationLink(destination: AdminView()) {
                        MenuButton(title: "Administration", icon: "lock.shield", color: .indigo)
                    }

                    NavigationLink(destination: SupabaseView()) {
                        MenuButton(title: "Supabase", icon: "cloud", color: .teal)
                    }

                    NavigationLink(destination: PasswordManagementView()) {
                        MenuButton(title: "Gestion du mot de passe", icon: "key", color: .mint)
                    }

                    NavigationLink(destination: SitesView(sdk: sdk)) {
                        MenuButton(title: "Sites", icon: "building.2", color: .blue)
                    }

                    NavigationLink(destination: ProductsView(sdk: sdk)) {
                        MenuButton(title: "Produits", icon: "shippingbox", color: .green)
                    }

                    NavigationLink(destination: PurchasesView()) {
                        MenuButton(title: "Achats", icon: "cart.badge.plus", color: .orange)
                    }

                    NavigationLink(destination: SalesView()) {
                        MenuButton(title: "Ventes", icon: "cart", color: .orange)
                    }

                    NavigationLink(destination: TransfersView()) {
                        MenuButton(title: "Transferts", icon: "arrow.left.arrow.right", color: .purple)
                    }

                    NavigationLink(destination: StockView()) {
                        MenuButton(title: "Stock", icon: "cube.box", color: .purple)
                    }
                }
                .padding(.horizontal)

                Spacer()

                Text("Version \(MedistockSDK.companion.VERSION)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .navigationTitle("")
            .navigationBarHidden(true)
        }
        .onAppear {
            greeting = Greeting().greet()
        }
    }
}

struct MenuButton: View {
    let title: String
    let icon: String
    let color: Color

    var body: some View {
        HStack {
            Image(systemName: icon)
                .frame(width: 30)
            Text(title)
                .fontWeight(.medium)
            Spacer()
            Image(systemName: "chevron.right")
        }
        .padding()
        .background(color.opacity(0.1))
        .foregroundColor(color)
        .cornerRadius(10)
    }
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
