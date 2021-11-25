package client.enum

/**
 * 传输类型的枚举类型，code来自rfc文档
 * @param code: String
 */


enum class TransferTypeEnum(var code: String) {
    BINARY("I"),
    ASCII("A"),
}