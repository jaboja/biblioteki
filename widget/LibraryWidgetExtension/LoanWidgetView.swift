import SwiftUI
import WidgetKit

struct LoanWidgetView: View {
    let entry: LoanEntry

    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .systemSmall:  smallView
        case .systemMedium: mediumView
        default:            largeView
        }
    }

    // MARK: - Small (1–2 pozycje)

    private var smallView: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Wypożyczenia (\(entry.loans.count))")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(Color.accentColor)
            if entry.loans.isEmpty {
                emptyLabel
            } else {
                if (entry.loans.count > 7) {
                    ForEach(entry.loans.prefix(6)) { loan in
                        SmallLoanRow(loan: loan)
                    }
                    Text("+ \(entry.loans.count - 6) więcej")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(entry.loans) { loan in
                        SmallLoanRow(loan: loan)
                    }
                }
            }
            Spacer(minLength: 0)
            if !entry.fetchErrors.isEmpty {
                errorBadge
            }
        }
        .padding(12)
    }

    // MARK: - Medium (3 pozycje)

    private var mediumView: some View {
        mediumOrLargeView(3)
    }

    // MARK: - Large (7 pozycji)

    private var largeView: some View {
        mediumOrLargeView(8)
    }

    private func mediumOrLargeView(_ n: Int) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            header
            if entry.loans.isEmpty {
                emptyLabel
            } else {
                ForEach(entry.loans.prefix(n)) { loan in
                    MediumLoanRow(loan: loan)
                }
                if entry.loans.count > n {
                    Text("+ \(entry.loans.count - n) więcej")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.top, 2)
                }
            }
            Spacer(minLength: 0)
            if !entry.fetchErrors.isEmpty {
                errorBadge
            }
        }
        .padding(14)
    }

    // MARK: - Komponenty wspólne

    private var header: some View {
        HStack {
            Image(systemName: "books.vertical.fill")
                .font(.caption)
                .foregroundStyle(Color.accentColor)
            Text("Wypożyczenia (\(entry.loans.count))")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(Color.accentColor)
            Spacer()
            Text(entry.date, style: .time)
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
    }

    private var emptyLabel: some View {
        Text("Brak wypożyczeń")
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(.top, 8)
    }

    private var errorBadge: some View {
        Label("Błąd: \(entry.fetchErrors.joined(separator: ", "))", systemImage: "exclamationmark.triangle.fill")
            .font(.caption2)
            .foregroundStyle(.orange)
            .padding(.top, 4)
    }
}

// MARK: - Wiersze

private struct SmallLoanRow: View {
    let loan: Loan
    var body: some View {
        HStack(alignment: .center, spacing: 3) {
            UrgencyCircle(loan: loan)
            Text(loan.title)
                .font(.caption2)
                .fontWeight(.medium)
                .lineLimit(1)
        }
    }
}

private struct MediumLoanRow: View {
    let loan: Loan
    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            HStack(alignment: .center, spacing: 3) {
                UrgencyCircle(loan: loan)
                Text(loan.title)
                    .font(.caption)
                    .fontWeight(.medium)
                    .lineLimit(1)
            }
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                let location = loan.location ?? loan.libraryID
                Text(loan.author)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer()
                if let label = dueLabel(loan) {
                    Text(location + ",")
                        .font(.caption2)
                        .fontWeight(.medium)
                        .foregroundStyle(.secondary)
                    label
                        .font(.caption2)
                } else {
                    Text(location)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 2)
    }
}

private struct UrgencyCircle: View {
    let loan: Loan
    var body: some View {
        if (loan.renew == true) {
            Circle()
                .fill(urgencyColor(loan))
                .frame(width: 6, height: 6)
        } else {
            let rect = Rectangle()
                .fill(urgencyColor(loan))
                .frame(width: 5, height: 5)
            if loan.renew == nil {
                rect
            } else {
                rect
                    .rotationEffect(Angle(degrees: 45))
            }
        }
    }
}

// MARK: - Helpers

private func urgencyColor(_ loan: Loan) -> Color {
    let color = dueForeground(loan)
    if color == .secondary { return .green }
    return color
}

private func dueLabelText(_ loan: Loan) -> String? {
    guard let days = loan.daysUntilDue else { return nil }
    if days < 0 { return "\(-days) dni po terminie" }
    if days == 0 { return "dziś" }
    if days == 1 { return "jutro" }
    return "za \(days) dni"
}

private func dueLabel(_ loan: Loan) -> Text? {
    guard let days = loan.daysUntilDue else { return nil }
    guard let dueDate = loan.dueDate else { return nil }
    if days > 3 {
        return Text(dueDate, style: .date)
            .foregroundStyle(dueForeground(loan))
            .monospacedDigit()
    }
    guard let label = dueLabelText(loan) else { return nil }
    return Text(label)
        .foregroundStyle(dueForeground(loan))
}

private func dueForeground(_ loan: Loan) -> Color {
    guard let days = loan.daysUntilDue else { return .gray }
    if days < 0 { return .red }
    if days <= 3 { return .orange }
    if days <= 7 { return .yellow }
    return .secondary
}

// MARK: - Preview

#Preview(as: .systemLarge) {
    LibraryLoanWidget()
} timeline: {
    LoanEntry.placeholder
}
