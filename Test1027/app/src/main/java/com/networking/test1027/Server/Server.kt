package com.networking.test1027.Server

import android.os.Bundle
import android.os.Handler
import android.os.Message
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class Server (var port:Int,h: Handler?){
    var commandSoc: Socket? = null
    var dataSoc: Socket? = null
    var serverSocket: ServerSocket? = null
    var dos: DataOutputStream? = null
    var dis: DataInputStream? = null
    var messageRecv: String? = null
    var handler: Handler? = null
    init {
        handler = h
    }
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
                    //服务器的响应机制应该在这里写
                    if(msgRecv.equals("NOOP")){
                        dos.writeUTF("220 Service ready \n")
                    }else if(msgRecv.startsWith("PORT")){
                        val port = Integer.parseInt(msgRecv.substring(5))
                        val clientAddress = socket!!.remoteSocketAddress.toString();
                        println("***************************"+clientAddress.substring(1,clientAddress.indexOf(':')))
                        DataConnectionThread("NOOP",clientAddress.substring(1,clientAddress.indexOf(':')),port).start()
                        dos.writeUTF("connected!\n")
                    }else if(msgRecv.startsWith("PASV")){
                        dos.writeUTF("PORTP 9999\n")
                        ServerPort(9999).startService()
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
    internal inner class DataConnectionThread(msg: String?,s_ip:String?,p: Int) : Thread() {
        var message: String? = null
        var port:Int = 0
        var server_ip:String? = null
        override fun run() {
            if (dataSoc == null) {
                try {
                    if ("" == server_ip) {
                        return
                    }
                    println(server_ip)
                    dataSoc = Socket(server_ip,port)
                    //获取socket的输入输出流
                    dis = DataInputStream(dataSoc!!.getInputStream())
                    dos = DataOutputStream(dataSoc!!.getOutputStream())
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
            try {
                serverOperate(message)
                dos!!.writeUTF(message)
                dos!!.flush()
                messageRecv = dis!!.readUTF() //如果没有收到数据，会阻塞
                val msg = Message()
                val b = Bundle()
                b.putString("data", messageRecv)
                msg.data = b
                handler!!.sendMessage(msg)
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        fun serverOperate(msg: String?){
            println("get:$msg")
        }
        init {
            port = p
            message = msg
            server_ip = s_ip
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