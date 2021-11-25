package server

import java.net.ServerSocket
import java.util.concurrent.Executors


/**
 * ftp的socket管理
 */
class FTPSocketManger(
    users: Set<User> = DEFAULT_USERS,
    port: Int = DEFAULT_PORT,
    private val dataPorts: Set<Int> = DEFAULT_DATA_PORT,
    nThread: Int = 5,
    private val timeout: Int = 50000,
) {
    private val serverSocket = ServerSocket(port)
    private val pool = Executors.newFixedThreadPool(nThread)

    private var count = 1


    /**
     * 监听来自默认端口的连接，然后放入线程池运行主程序
     */
    fun listen() {
        val socket = serverSocket.accept()
        socket.soTimeout = timeout

        val order = count++ //Fetch and increment

        pool.execute {
//            ClientListener(socket, dataPorts, System.out, "Michel n°$order").use {
//                it.listenForever()
//            }
        }
    }

    /**
     * 保持监听
     */
    fun listenForever() {
        while (true) listen()
    }


    /**
     * 关闭control port
     */
    fun destory() {
        serverSocket.close()
    }
}