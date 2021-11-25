package server


/**
 *  默认的设置都在这边，可以在这里添加用户，改变端口号等等
 */


const val DEFAULT_PORT:Int = 21;
val DEFAULT_DATA_PORT:Set<Int> = (2020..2025).toSet()

val anonymous = User("anonymous", null)

var DEFAULT_USERS = mutableSetOf<User>(anonymous)

