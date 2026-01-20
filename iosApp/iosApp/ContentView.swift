import SwiftUI
import shared

struct ContentView: View {
    let sdk: MedistockSDK
    @State private var greeting: String = ""

    var body: some View {
        NavigationView {
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
                    NavigationLink(destination: SitesView(sdk: sdk)) {
                        MenuButton(title: "Sites", icon: "building.2", color: .blue)
                    }

                    NavigationLink(destination: ProductsView(sdk: sdk)) {
                        MenuButton(title: "Produits", icon: "shippingbox", color: .green)
                    }

                    NavigationLink(destination: SalesView(sdk: sdk)) {
                        MenuButton(title: "Ventes", icon: "cart", color: .orange)
                    }

                    NavigationLink(destination: StockView(sdk: sdk)) {
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

// Placeholder views - to be implemented
struct SitesView: View {
    let sdk: MedistockSDK
    var body: some View {
        Text("Sites - Coming Soon")
            .navigationTitle("Sites")
    }
}

struct ProductsView: View {
    let sdk: MedistockSDK
    var body: some View {
        Text("Produits - Coming Soon")
            .navigationTitle("Produits")
    }
}

struct SalesView: View {
    let sdk: MedistockSDK
    var body: some View {
        Text("Ventes - Coming Soon")
            .navigationTitle("Ventes")
    }
}

struct StockView: View {
    let sdk: MedistockSDK
    var body: some View {
        Text("Stock - Coming Soon")
            .navigationTitle("Stock")
    }
}

#Preview {
    ContentView(sdk: MedistockSDK(driverFactory: DatabaseDriverFactory()))
}
