package com.networking.test1027

import ClientPort
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
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
import com.networking.test1027.Client.Client
import com.networking.test1027.Server.Server
import com.networking.test1027.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.*

class MainActivity : AppCompatActivity() {
//    var et_message: EditText? = null //需要发送的内容
//    var et_ip: EditText? = null //输入的IP地址
//    var bt_connect: Button? = null //连接测试
//    var bt_communicate: Button? = null //发送
//    var bt_startServer: Button? = null //启动服务端
//    var tv_reply: TextView? = null //服务器回复的消息
//    var tv_reply_Server: TextView? = null
    //下面四个是显示当前机器的ip地址用的
//    private var ipTextView: TextView? = null
//    private var nameTextView: TextView? = null
//    private var mConnectivityManager: ConnectivityManager? = null
//    private var mActiveNetInfo: NetworkInfo? = null

    //显示或处理服务器返回数据
    var handler: Handler? = null
    var handler_Server: Handler? = null

    //客户端
    var client: Client? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        bt_startServer?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread { Server(9998,handler_Server).startService() }
                .start()
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
            bt_startServer?.isEnabled = false
        })
        bt_connect?.setOnClickListener(View.OnClickListener { v: View? ->
            client = Client(handler);
            client?.server_ip = et_ip?.text.toString()
            client?.connectServer("NOOP")
            et_ip?.isEnabled = false
        })
        bt_communicate?.setOnClickListener(View.OnClickListener { v: View? ->
            client?.connectServer(et_message?.getText().toString())
        })
        handler = Handler { msg: Message ->
            val b = msg.data //获取消息中的Bundle对象
            val str = b.getString("data") //获取键为data的字符串的值
            tv_reply?.text = str+"\n"+tv_reply?.text
            client?.dealWithMsg(str)
            false
        }
        handler_Server = Handler { msg: Message ->
            val b = msg.data //获取消息中的Bundle对象
            val str = b.getString("data") //获取键为data的字符串的值
            tv_reply_server?.text = str+"\n"+tv_reply_server?.text
            client?.dealWithMsg(str)
            false
        }
//        nameTextView = findViewById<TextView>(R.id.nametextview)
//        ipTextView = findViewById<TextView>(R.id.ipTextView)
//        mConnectivityManager =
//            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? //获取系统的连接服务
//        mActiveNetInfo = mConnectivityManager?.getActiveNetworkInfo() //获取网络连接的信息
//        if (mActiveNetInfo == null) myDialog() else setUpInfo()

    }

    //新建一个子线程，实现socket通信

}