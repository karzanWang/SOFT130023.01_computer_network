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
    private val timeout: Int = 50000,
) {
    private val clientSocket = Socket(server_ip, port)
    private var count = 1
    private val handler = h;
    private var client:FTPClient? = null

    fun create() {
        val socket = clientSocket
        socket.soTimeout = timeout

        val order = count++ //Fetch and increment
        client = FTPClient(appContext,socket, dataPorts, handler, "client$order")
        client!!.listenForever()
    }
    fun send(msg:String){
        client!!.sendFunc(msg)
    }

    /**
     * 关闭control port
     */
    fun destory() {
        if(clientSocket.isConnected){
            clientSocket.close()
        }
    }
}