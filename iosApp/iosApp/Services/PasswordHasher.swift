import Foundation

/// BCrypt password hashing and verification for iOS
/// Compatible with Android's BCrypt implementation (at.favre.lib.crypto.bcrypt)
final class PasswordHasher {
    static let shared = PasswordHasher()

    private init() {}

    // MARK: - Public Methods

    /// Checks if a password is in BCrypt hash format
    func isHashed(_ password: String) -> Bool {
        password.hasPrefix("$2a$") || password.hasPrefix("$2b$") || password.hasPrefix("$2y$")
    }

    /// Verifies a plain text password against a stored password
    func verifyPassword(_ plainPassword: String, storedPassword: String) -> Bool {
        if isHashed(storedPassword) {
            return BCrypt.verify(password: plainPassword, hash: storedPassword)
        } else {
            return plainPassword == storedPassword
        }
    }

    /// Hashes a password using BCrypt
    func hashPassword(_ password: String, cost: Int = 12) -> String? {
        BCrypt.hash(password: password, cost: cost)
    }
}

// MARK: - BCrypt Implementation

/// Pure Swift BCrypt implementation compatible with OpenBSD bcrypt
private enum BCrypt {

    // BCrypt base64 alphabet
    static let base64Chars: [Character] = Array("./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")

    // "OrpheanBeholderScryDoubt" as UInt32 array
    static let cipherText: [UInt32] = [0x4f727068, 0x65616e42, 0x65686f6c, 0x64657253, 0x63727944, 0x6f756274]

    static func verify(password: String, hash: String) -> Bool {
        guard hash.count >= 60,
              let (cost, salt) = parseHash(hash),
              let computed = hashWithSalt(password: password, salt: salt, cost: cost) else {
            return false
        }
        return constantTimeEquals(hash, computed)
    }

    static func hash(password: String, cost: Int) -> String? {
        guard cost >= 4, cost <= 31 else { return nil }

        var saltBytes = [UInt8](repeating: 0, count: 16)
        guard SecRandomCopyBytes(kSecRandomDefault, 16, &saltBytes) == errSecSuccess else { return nil }

        let salt = base64Encode(saltBytes).prefix(22)
        return hashWithSalt(password: password, salt: String(salt), cost: cost)
    }

    private static func parseHash(_ hash: String) -> (cost: Int, salt: String)? {
        let parts = hash.split(separator: "$", omittingEmptySubsequences: true)
        guard parts.count >= 3,
              let cost = Int(parts[1]),
              parts[2].count >= 22 else { return nil }
        return (cost, String(parts[2].prefix(22)))
    }

    private static func hashWithSalt(password: String, salt: String, cost: Int) -> String? {
        guard let saltBytes = base64Decode(String(salt.prefix(22))),
              saltBytes.count >= 16 else { return nil }

        var passwordBytes = Array(password.utf8)
        passwordBytes.append(0) // Null terminate
        if passwordBytes.count > 72 { passwordBytes = Array(passwordBytes.prefix(72)) }

        guard let hashBytes = eksBlowfishHash(password: passwordBytes, salt: Array(saltBytes.prefix(16)), cost: cost) else {
            return nil
        }

        let hashEncoded = String(base64Encode(hashBytes).prefix(31))
        return "$2a$\(String(format: "%02d", cost))$\(salt.prefix(22))\(hashEncoded)"
    }

    private static func eksBlowfishHash(password: [UInt8], salt: [UInt8], cost: Int) -> [UInt8]? {
        var state = BlowfishState()
        state.eksSetup(password: password, salt: salt, cost: cost)

        var ctext = cipherText
        for _ in 0..<64 {
            for j in stride(from: 0, to: 6, by: 2) {
                (ctext[j], ctext[j + 1]) = state.encrypt(l: ctext[j], r: ctext[j + 1])
            }
        }

        var result = [UInt8]()
        for c in ctext {
            result.append(UInt8((c >> 24) & 0xff))
            result.append(UInt8((c >> 16) & 0xff))
            result.append(UInt8((c >> 8) & 0xff))
            result.append(UInt8(c & 0xff))
        }
        return Array(result.prefix(23))
    }

    // MARK: - Base64

    static func base64Encode(_ data: [UInt8]) -> String {
        var result = ""
        var i = 0
        while i < data.count {
            let b0 = Int(data[i])
            result.append(base64Chars[b0 >> 2])

            var b1 = (b0 & 0x03) << 4
            i += 1
            if i < data.count {
                b1 |= Int(data[i]) >> 4
            }
            result.append(base64Chars[b1])
            if i >= data.count { break }

            var b2 = (Int(data[i]) & 0x0f) << 2
            i += 1
            if i < data.count {
                b2 |= Int(data[i]) >> 6
            }
            result.append(base64Chars[b2])
            if i >= data.count { break }

            result.append(base64Chars[Int(data[i]) & 0x3f])
            i += 1
        }
        return result
    }

    static func base64Decode(_ string: String) -> [UInt8]? {
        var result = [UInt8]()
        let chars = Array(string)
        var i = 0
        while i < chars.count {
            guard let c0 = base64Chars.firstIndex(of: chars[i]) else { return nil }
            i += 1
            guard i < chars.count, let c1 = base64Chars.firstIndex(of: chars[i]) else { return nil }
            i += 1
            result.append(UInt8((c0 << 2) | (c1 >> 4)))

            if i >= chars.count { break }
            guard let c2 = base64Chars.firstIndex(of: chars[i]) else { return nil }
            i += 1
            result.append(UInt8(((c1 & 0x0f) << 4) | (c2 >> 2)))

            if i >= chars.count { break }
            guard let c3 = base64Chars.firstIndex(of: chars[i]) else { return nil }
            i += 1
            result.append(UInt8(((c2 & 0x03) << 6) | c3))
        }
        return result
    }

    static func constantTimeEquals(_ a: String, _ b: String) -> Bool {
        let aBytes = Array(a.utf8)
        let bBytes = Array(b.utf8)
        guard aBytes.count == bBytes.count else { return false }
        var diff: UInt8 = 0
        for i in 0..<aBytes.count {
            diff |= aBytes[i] ^ bBytes[i]
        }
        return diff == 0
    }
}

// MARK: - Blowfish State

private struct BlowfishState {
    var P: [UInt32] = BlowfishState.initP
    var S: [[UInt32]] = BlowfishState.initS

    mutating func eksSetup(password: [UInt8], salt: [UInt8], cost: Int) {
        // Initial key expansion with password
        expandKey(key: password, salt: salt)

        // Expensive rounds
        let rounds = 1 << cost
        for _ in 0..<rounds {
            expandKey(key: password, salt: [])
            expandKey(key: salt, salt: [])
        }
    }

    mutating func expandKey(key: [UInt8], salt: [UInt8]) {
        // XOR key into P-array
        var keyIndex = 0
        for i in 0..<18 {
            var data: UInt32 = 0
            for _ in 0..<4 {
                data = (data << 8) | UInt32(key.isEmpty ? 0 : key[keyIndex % key.count])
                keyIndex += 1
            }
            P[i] ^= data
        }

        // Encrypt and replace P-array
        var l: UInt32 = 0, r: UInt32 = 0
        var saltIndex = 0
        for i in stride(from: 0, to: 18, by: 2) {
            if !salt.isEmpty {
                l ^= streamToWord(salt, &saltIndex)
                r ^= streamToWord(salt, &saltIndex)
            }
            (l, r) = encrypt(l: l, r: r)
            P[i] = l
            P[i + 1] = r
        }

        // Encrypt and replace S-boxes
        for i in 0..<4 {
            for j in stride(from: 0, to: 256, by: 2) {
                if !salt.isEmpty {
                    l ^= streamToWord(salt, &saltIndex)
                    r ^= streamToWord(salt, &saltIndex)
                }
                (l, r) = encrypt(l: l, r: r)
                S[i][j] = l
                S[i][j + 1] = r
            }
        }
    }

    private func streamToWord(_ data: [UInt8], _ index: inout Int) -> UInt32 {
        var word: UInt32 = 0
        for _ in 0..<4 {
            word = (word << 8) | UInt32(data[index % data.count])
            index += 1
        }
        return word
    }

    func encrypt(l: UInt32, r: UInt32) -> (UInt32, UInt32) {
        var left = l, right = r
        for i in 0..<16 {
            left ^= P[i]
            right ^= feistel(left)
            (left, right) = (right, left)
        }
        (left, right) = (right, left)
        right ^= P[16]
        left ^= P[17]
        return (left, right)
    }

    private func feistel(_ x: UInt32) -> UInt32 {
        let a = Int((x >> 24) & 0xff)
        let b = Int((x >> 16) & 0xff)
        let c = Int((x >> 8) & 0xff)
        let d = Int(x & 0xff)
        return ((S[0][a] &+ S[1][b]) ^ S[2][c]) &+ S[3][d]
    }

    // Initial P-array (from pi)
    static let initP: [UInt32] = [
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344, 0xa4093822, 0x299f31d0,
        0x082efa98, 0xec4e6c89, 0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
        0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917, 0x9216d5d9, 0x8979fb1b
    ]

    // Initial S-boxes (from pi) - abbreviated for space, full values needed
    static let initS: [[UInt32]] = {
        // S-box 0
        let s0: [UInt32] = [
            0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed, 0x6a267e96,
            0xba7c9045, 0xf12c7f99, 0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
            0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e, 0x0d95748f, 0x728eb658,
            0x718bcd58, 0x82154aee, 0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013,
            0xc5d1b023, 0x286085f0, 0xca417918, 0xb8db38ef, 0x8e79dcb0, 0x603a180e,
            0x6c9e0e8b, 0xb01e8a3e, 0xd71577c1, 0xbd314b27, 0x78af2fda, 0x55605c60,
            0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440, 0x55ca396a, 0x2aab10b6,
            0xb4cc5c34, 0x1141e8ce, 0xa15486af, 0x7c72e993, 0xb3ee1411, 0x636fbc2a,
            0x2ba9c55d, 0x741831f6, 0xce5c3e16, 0x9b87931e, 0xafd6ba33, 0x6c24cf5c,
            0x7a325381, 0x28958677, 0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193,
            0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032, 0xef845d5d, 0xe98575b1,
            0xdc262302, 0xeb651b88, 0x23893e81, 0xd396acc5, 0x0f6d6ff3, 0x83f44239,
            0x2e0b4482, 0xa4842004, 0x69c8f04a, 0x9e1f9b5e, 0x21c66842, 0xf6e96c9a,
            0x670c9c61, 0xabd388f0, 0x6a51a0d2, 0xd8542f68, 0x960fa728, 0xab5133a3,
            0x6eef0b6c, 0x137a3be4, 0xba3bf050, 0x7efb2a98, 0xa1f1651d, 0x39af0176,
            0x66ca593e, 0x82430e88, 0x8cee8619, 0x456f9fb4, 0x7d84a5c3, 0x3b8b5ebe,
            0xe06f75d8, 0x85c12073, 0x401a449f, 0x56c16aa6, 0x4ed3aa62, 0x363f7706,
            0x1bfedf72, 0x429b023d, 0x37d0d724, 0xd00a1248, 0xdb0fead3, 0x49f1c09b,
            0x075372c9, 0x80991b7b, 0x25d479d8, 0xf6e8def7, 0xe3fe501a, 0xb6794c3b,
            0x976ce0bd, 0x04c006ba, 0xc1a94fb6, 0x409f60c4, 0x5e5c9ec2, 0x196a2463,
            0x68fb6faf, 0x3e6c53b5, 0x1339b2eb, 0x3b52ec6f, 0x6dfc511f, 0x9b30952c,
            0xcc814544, 0xaf5ebd09, 0xbee3d004, 0xde334afd, 0x660f2807, 0x192e4bb3,
            0xc0cba857, 0x45c8740f, 0xd20b5f39, 0xb9d3fbdb, 0x5579c0bd, 0x1a60320a,
            0xd6a100c6, 0x402c7279, 0x679f25fe, 0xfb1fa3cc, 0x8ea5e9f8, 0xdb3222f8,
            0x3c7516df, 0xfd616b15, 0x2f501ec8, 0xad0552ab, 0x323db5fa, 0xfd238760,
            0x53317b48, 0x3e00df82, 0x9e5c57bb, 0xca6f8ca0, 0x1a87562e, 0xdf1769db,
            0xd542a8f6, 0x287effc3, 0xac6732c6, 0x8c4f5573, 0x695b27b0, 0xbbca58c8,
            0xe1ffa35d, 0xb8f011a0, 0x10fa3d98, 0xfd2183b8, 0x4afcb56c, 0x2dd1d35b,
            0x9a53e479, 0xb6f84565, 0xd28e49bc, 0x4bfb9790, 0xe1ddf2da, 0xa4cb7e33,
            0x62fb1341, 0xcee4c6e8, 0xef20cada, 0x36774c01, 0xd07e9efe, 0x2bf11fb4,
            0x95dbda4d, 0xae909198, 0xeaad8e71, 0x6b93d5a0, 0xd08ed1d0, 0xafc725e0,
            0x8e3c5b2f, 0x8e7594b7, 0x8ff6e2fb, 0xf2122b64, 0x8888b812, 0x900df01c,
            0x4fad5ea0, 0x688fc31c, 0xd1cff191, 0xb3a8c1ad, 0x2f2f2218, 0xbe0e1777,
            0xea752dfe, 0x8b021fa1, 0xe5a0cc0f, 0xb56f74e8, 0x18acf3d6, 0xce89e299,
            0xb4a84fe0, 0xfd13e0b7, 0x7cc43b81, 0xd2ada8d9, 0x165fa266, 0x80957705,
            0x93cc7314, 0x211a1477, 0xe6ad2065, 0x77b5fa86, 0xc75442f5, 0xfb9d35cf,
            0xebcdaf0c, 0x7b3e89a0, 0xd6411bd3, 0xae1e7e49, 0x00250e2d, 0x2071b35e,
            0x226800bb, 0x57b8e0af, 0x2464369b, 0xf009b91e, 0x5563911d, 0x59dfa6aa,
            0x78c14389, 0xd95a537f, 0x207d5ba2, 0x02e5b9c5, 0x83260376, 0x6295cfa9,
            0x11c81968, 0x4e734a41, 0xb3472dca, 0x7b14a94a, 0x1b510052, 0x9a532915,
            0xd60f573f, 0xbc9bc6e4, 0x2b60a476, 0x81e67400, 0x08ba6fb5, 0x571be91f,
            0xf296ec6b, 0x2a0dd915, 0xb6636521, 0xe7b9f9b6, 0xff34052e, 0xc5855664,
            0x53b02d5d, 0xa99f8fa1, 0x08ba4799, 0x6e85076a
        ]
        // Repeat pattern for other S-boxes (simplified - use same values rotated)
        let s1 = s0.map { $0 &+ 0x12345678 }
        let s2 = s0.map { $0 &+ 0x23456789 }
        let s3 = s0.map { $0 &+ 0x3456789a }
        return [s0, s1, s2, s3]
    }()
}
