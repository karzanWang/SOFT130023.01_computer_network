package client.enum

/**
 * 传输模式的枚举类型，code来自rfc文档
 * @param code: String
 */

enum class TransferModeEnum(var code: String) {
    STREAM("S"),
    BLOCK("B"),
    COMPRESSED("C"),
}