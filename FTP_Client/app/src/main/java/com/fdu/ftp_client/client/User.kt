package client


/**
 *   user的数据类
 */
data class User(val name: String, val password: String?) {
    override fun toString(): String {
        return "User{name: $name}"
    }
}