package com.networking.test1027

import ClientPort
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.networking.test1027.Server.Server
import com.networking.test1027.databinding.ActivityMainBinding
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.*

class MainActivity : AppCompatActivity() {
    var et_message: EditText? = null //需要发送的内容
    var et_ip: EditText? = null //输入的IP地址
    var bt_connect: Button? = null //连接测试
    var bt_communicate: Button? = null //发送
    var bt_startServer: Button? = null //启动服务端
    var tv_reply: TextView? = null //服务器回复的消息
    //下面四个是显示当前机器的ip地址用的
    private var ipTextView: TextView? = null
    private var nameTextView: TextView? = null
    private var mConnectivityManager: ConnectivityManager? = null
    private var mActiveNetInfo: NetworkInfo? = null
    //流
    var handler: Handler? = null
    var commandSoc: Socket? = null
    var dataSoc: Socket? = null
    var dos: DataOutputStream? = null
    var dis: DataInputStream? = null
    var messageRecv: String? = null
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et_message = findViewById(R.id.et_message)
        et_ip = findViewById(R.id.et_ip)
        bt_connect = findViewById(R.id.bt_connect)
        bt_communicate = findViewById(R.id.bt_communicate)
        bt_startServer = findViewById(R.id.bt_startServer)
        tv_reply = findViewById(R.id.tv_reply)
        bt_startServer?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread { Server(9998).startService() }
                .start()
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
            bt_startServer?.isEnabled = false
        })
        bt_connect?.setOnClickListener(View.OnClickListener { v: View? ->
            ConnectionThread("NOOP").start()
            et_ip?.isEnabled = false
        })
        bt_communicate?.setOnClickListener(View.OnClickListener { v: View? ->
            ConnectionThread(et_message?.getText().toString()).start()
        })
        handler = Handler { msg: Message ->
            val b = msg.data //获取消息中的Bundle对象
            val str = b.getString("data") //获取键为data的字符串的值
            tv_reply?.text = str+tv_reply?.text

            if(str?.startsWith("PORTP")==true){
                val port = Integer.parseInt(str.substring(6))
                DataConnectionThread("HELLO",port).start()
            }
            false
        }
        nameTextView = findViewById<TextView>(R.id.nametextview)
        ipTextView = findViewById<TextView>(R.id.ipTextView)
        mConnectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? //获取系统的连接服务
        mActiveNetInfo = mConnectivityManager?.getActiveNetworkInfo() //获取网络连接的信息
        if (mActiveNetInfo == null) myDialog() else setUpInfo()

    }
    val iPAddress: String?
        get() {
            val info: NetworkInfo? = mConnectivityManager?.getActiveNetworkInfo()
            if (info != null && info.isConnected()) {
                if (info.getType() == ConnectivityManager.TYPE_MOBILE || info.getType() == ConnectivityManager.TYPE_WIFI) { //当前使用2G/3G/4G网络
                    try {
                        val en = NetworkInterface.getNetworkInterfaces()
                        while (en.hasMoreElements()) {
                            val intf = en.nextElement()
                            val enumIpAddr = intf.inetAddresses
                            while (enumIpAddr.hasMoreElements()) {
                                val inetAddress = enumIpAddr.nextElement()
                                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                    return inetAddress.getHostAddress()
                                }
                            }
                        }
                    } catch (e: SocketException) {
                        e.printStackTrace()
                    }
                }
            } else { //当前无网络连接,请在设置中打开网络
                return null
            }
            return null
        }
    //显示地址
    @SuppressLint("SetTextI18n")
    fun setUpInfo() {
        if(iPAddress?.startsWith("192.168") == true){
            nameTextView?.setText("获取WIFI环境下地址成功，请通过此IP地址连接！")
        }else{
            nameTextView?.setText("获取WIFI环境下地址失败，请建立热点或连接已有WIFI！")
        }
        ipTextView?.setText("IP地址：$iPAddress")
    }

    private fun myDialog() {
        val mDialog: AlertDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("注意")
            .setMessage("当前网络不可用，请检查网络！")
            .setPositiveButton("确定", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.dismiss()
                    this@MainActivity.finish()
                }
            })
            .create() //创建这个对话框
        mDialog.show() //显示这个对话框
    }

    //新建一个子线程，实现socket通信
    internal inner class ConnectionThread(msg: String?) : Thread() {
        var message: String? = null
        override fun run() {
            if (commandSoc == null) {
                try {
                    if ("" == et_ip?.text.toString()) {
                        return
                    }
                    commandSoc = Socket(et_ip?.text.toString(),9998)
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
            }
        }
        init {
            message = msg
        }
    }

    internal inner class DataConnectionThread(msg: String?,p: Int) : Thread() {
        var message: String? = null
        var port: Int = 0
        override fun run() {
            if (commandSoc == null) {
                try {
                    if ("" == et_ip?.text.toString()) {
                        return
                    }
                    commandSoc = Socket(et_ip?.text.toString(), port)
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
            }
        }
        init {
            message = msg
            port = p
        }
    }
}