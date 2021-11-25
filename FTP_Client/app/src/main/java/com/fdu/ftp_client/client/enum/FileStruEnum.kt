package client.enum

/**
 * 传输结构的枚举类型，code来自rfc文档
 * @param code: String
 */

enum class FileStruEnum(var code: String) {
    FILE("F"),
    RECORD("R"),
    PAGE("P"),
}