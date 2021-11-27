package server

import android.content.Context
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import java.net.ServerSocket
import java.util.concurrent.Executors



/**
 * ftp的socket管理
 */
@RequiresApi(Build.VERSION_CODES.O)
class FTPSocketManger(
    private val appContext: Context,
    h : Handler?,
    users: Set<User> = DEFAULT_USERS,
    port: Int = DEFAULT_PORT,
    private val dataPorts: Set<Int> = DEFAULT_DATA_PORT,
    nThread: Int = 5,
    private val timeout: Int = 5000000,
) {
    private val handler = h
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
            FTPServer(appContext,socket, dataPorts, handler, "server$order").listenForever()
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