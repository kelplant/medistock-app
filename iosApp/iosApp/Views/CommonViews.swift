import Foundation
import SwiftUI

// MARK: - Empty State View
struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text(title)
                .font(.headline)

            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }
}

// MARK: - Loading View
struct LoadingView: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Error View
struct ErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundColor(.orange)

            Text("Erreur")
                .font(.headline)

            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Button("Réessayer", action: onRetry)
                .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

// MARK: - Labeled Content (iOS 15 compatibility)
@available(iOS 15.0, *)
struct LabeledContentCompat<Label: View, Content: View>: View {
    let label: Label
    let content: Content

    init(@ViewBuilder label: () -> Label, @ViewBuilder content: () -> Content) {
        self.label = label()
        self.content = content()
    }

    var body: some View {
        HStack {
            label
            Spacer()
            content
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Date Formatting Extension
extension Date {
    func formatted(style: DateFormatter.Style = .medium) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = style
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: self)
    }

    func formattedWithTime() -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: self)
    }
}

// MARK: - Number Formatting Extension
extension Double {
    func formatted(decimals: Int = 2) -> String {
        String(format: "%.\(decimals)f", self)
    }

    func asCurrency() -> String {
        String(format: "%.2f €", self)
    }
}

// MARK: - Timestamp to Date Extension
extension Int64 {
    func toDate() -> Date {
        Date(timeIntervalSince1970: Double(self) / 1000)
    }

    func formatted(style: DateFormatter.Style = .medium) -> String {
        toDate().formatted(style: style)
    }

    func formattedWithTime() -> String {
        toDate().formattedWithTime()
    }
}

// MARK: - View Modifiers
struct CardStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

extension View {
    func cardStyle() -> some View {
        modifier(CardStyle())
    }
}

// MARK: - Badge View
struct BadgeView: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.2))
            .foregroundColor(color)
            .cornerRadius(4)
    }
}

// MARK: - Quantity Stepper
struct QuantityStepper: View {
    @Binding var value: Double
    let step: Double
    let range: ClosedRange<Double>

    var body: some View {
        HStack {
            Button(action: { value = max(range.lowerBound, value - step) }) {
                Image(systemName: "minus.circle.fill")
                    .font(.title2)
            }
            .disabled(value <= range.lowerBound)

            Text(String(format: "%.1f", value))
                .font(.headline)
                .frame(minWidth: 60)

            Button(action: { value = min(range.upperBound, value + step) }) {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
            }
            .disabled(value >= range.upperBound)
        }
        .foregroundColor(.accentColor)
    }
}
