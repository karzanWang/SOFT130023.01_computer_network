package client

import android.content.Context
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import java.net.Socket
import java.util.concurrent.Executors


/**
 * ftp的socket管理
 */
@RequiresApi(Build.VERSION_CODES.O)
class FTPSocketManger(
    private val appContext: Context,
    h:Handler?,
    server_ip: String,
    port: Int = DEFAULT_PORT,
    private val dataPorts: Set<Int> = DEFAULT_DATA_PORT,
    private val timeout: Int = 5000000,
) {
    private lateinit var clientSocket:Socket
    private var count = 0
    private val handler = h;
    public var client:FTPClient? = null
    private val pool = Executors.newFixedThreadPool(1)

    val ip = server_ip
    val port_1 = port

    fun create() {
        clientSocket = Socket(ip, port_1)
        val socket = clientSocket
        socket.soTimeout = timeout

        val order = count++ //Fetch and increment
        pool.execute {
            client = FTPClient(appContext,socket, dataPorts, handler, "client$order")
            client!!.listenForever()
            client!!.destory()
        }
    }
//    fun send(msg:String){
//        client!!.sendFunc(msg)
//    }

//    fun listenForever() {
//        while (true) create()
//    }

    /**
     * 关闭control port
     */
    fun destory() {
        if(clientSocket.isConnected){
            clientSocket.close()
        }
    }
}