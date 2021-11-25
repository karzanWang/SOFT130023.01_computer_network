package client

import android.content.Context

//import kotlin.reflect.jvm.internal.impl.renderer.DescriptorRenderer


/**
 *  默认的设置都在这边，可以在这里添加用户，改变端口号等等
 */


const val DEFAULT_PORT:Int = 10021;
val DEFAULT_DATA_PORT:Set<Int> = (2020..2025).toSet()

val anonymous = User("anonymous", null)
val test = User("test", "test")

var DEFAULT_USERS = mutableSetOf<User>(anonymous, test)

