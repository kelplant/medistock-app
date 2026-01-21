import Foundation
import shared

protocol TimestampConvertible {
    var timestampValue: Int64 { get }
}

extension Int64: TimestampConvertible {
    var timestampValue: Int64 { self }
}

extension KotlinLong: TimestampConvertible {
    var timestampValue: Int64 { int64Value }
}
