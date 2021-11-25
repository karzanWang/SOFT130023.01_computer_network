package server


/**
 *  QuitEvent : Throwable() ,用来方便线程退出的控制，直接throw，有manger来控制关闭
 */
class QuitEvent : Throwable() {
}