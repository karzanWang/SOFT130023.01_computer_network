package com.fdu.ftp_client.client

import android.os.Bundle
import android.os.Handler
import android.os.Message
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

class Client(h: Handler?) {
    var dataSoc: Socket? = null
    var commandSoc: Socket? = null
    var dos: DataOutputStream? = null
    var dis: DataInputStream? = null
    var messageRecv: String? = null
    var server_ip:String? = null
    var handler: Handler? = null
    init{
        handler = h
    }
    fun connectServer(msg: String?){
        ConnectionThread(msg).start()
    }
    fun conveyData(data: String?,p: Int){
        DataConnectionThread(data,p).start()
    }
    fun dealWithMsg(msg: String?){
        if(msg?.startsWith("PORTP")==true){
            val port = Integer.parseInt(msg.substring(6))
            conveyData("HELLO",port)
        }
    }
    internal inner class ConnectionThread(msg: String?) : Thread() {
        var message: String? = null
        override fun run() {
            if (commandSoc == null) {
                try {
                    if ("" == server_ip) {
                        return
                    }
                    commandSoc = Socket(server_ip,9998)
                    //获取socket的输入输出流
                    dis = DataInputStream(commandSoc!!.getInputStream())
                    dos = DataOutputStream(commandSoc!!.getOutputStream())
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
            try {
                clientOperate(message)
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
        fun clientOperate(msg: String?){
            if(msg?.startsWith("PORT")==true){
                val port = Integer.parseInt(msg.substring(5));
                Thread { ClientPort(port).startService() }
                    .start()
            }else if(msg?.startsWith("PASV")==true){
                println("PASV")
                //等待PASVP返回，不做任何操作
            }
            //待添加其他指令的客户端操作
        }
        init {
            message = msg
        }
    }
    internal inner class DataConnectionThread(msg: String?,p: Int) : Thread() {
        var message: String? = null
        var port:Int = 0
        override fun run() {
            if (dataSoc == null) {
                try {
                    if ("" == server_ip) {
                        return
                    }
                    println(server_ip);
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
                clientOperate(message)
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
        fun clientOperate(msg: String?){
            if(msg?.startsWith("PORT")==true){
                val port = Integer.parseInt(msg.substring(5));
                Thread { ClientPort(port).startService() }
                    .start()
            }else if(msg?.startsWith("PASV")==true){
                println("PASV")
            }
        }
        init {
            port = p
            message = msg
        }
    }
}