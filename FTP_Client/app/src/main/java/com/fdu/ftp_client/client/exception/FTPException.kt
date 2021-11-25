package client.exception

/**
 * FTPException,用于内部错误处理
 */
class FTPException(val code: Int, message: String?) : Exception(message) {
}