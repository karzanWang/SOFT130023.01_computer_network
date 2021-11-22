package com.fdu.ftp_server.server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class ServerPort (var port:Int){
    var serverSocket: ServerSocket? = null
    fun startService() {
        try {
            var socket: Socket? = null
            println("waiting...")
            //等待连接，每建立一个连接，就新建一个线程
            while (true) {
                socket = serverSocket!!.accept() //等待一个客户端的连接，在连接之前，此方法是阻塞的
                println("connect to" + socket.inetAddress + ":" + socket.localPort)
                ConnectThread(socket).start()
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            println("IOException")
            e.printStackTrace()
        }
    }

    //向客户端发送信息
    internal inner class ConnectThread(socket: Socket?) : Thread() {
        var socket: Socket? = null
        override fun run() {
            try {
                val dis = DataInputStream(socket!!.getInputStream())
                val dos = DataOutputStream(socket!!.getOutputStream())
                while (true) {
                    val msgRecv = dis.readUTF()
                    println("msg from client:$msgRecv")
                    if(msgRecv.equals("NOOP")){
                        dos.writeUTF("220 Service ready \n")
                    }else{
                        dos.writeUTF("getMessage:$msgRecv\n")
                    }
                    dos.flush()
                }
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        init {
            this.socket = socket
        }
    }

    init {

        //输出服务器的IP地址
        try {
            val addr = InetAddress.getLocalHost()
            println("local host:$addr")
            serverSocket = ServerSocket(port)
            println("0k")
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }
}