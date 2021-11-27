package com.fdu.ftp_client

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import client.FTPSocketManger
import com.fdu.ftp_client.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.delay
import java.lang.Thread.sleep

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private var ipTextView: TextView? = null
    private var nameTextView: TextView? = null
    private var mConnectivityManager: ConnectivityManager? = null
    private var mActiveNetInfo: NetworkInfo? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    //显示或处理服务器返回数据
    var handler: Handler? = null

    //客户端
    var client: FTPSocketManger? = null
    var cmd: String? = null
    lateinit var spinner: Spinner

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        //下拉框体内容绑定
        spinner = findViewById(R.id.planets_spinner)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.planets_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = this

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_aboutinfo
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        home_edittext_ip?.setText("127.0.0.1")
        home_bt_connect?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread {
                client =
                    FTPSocketManger(applicationContext, handler, home_edittext_ip?.text.toString())
                client!!.listenForever()
            }.start()
//            home_edittext_ip?.isEnabled = false
//            home_bt_connect?.isEnabled = false
        })
        home_bt_communicate?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread {
                client!!.client?.sendFunc(cmd + " " + home_edittext_message?.getText().toString())
            }.start()
        })

        home_bt_small1?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread {
                var i = 0
                while (i < 10){
                    client!!.client?.sendFunc("PORT 127,0,0,1,40,1")
                    sleep(500)
                    client!!.client?.sendFunc("STOR small000$i")
                    sleep(2000)
                    i++
                }
            }.start()
        })

        home_bt_small2?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread {
                var i = 80
                while (i < 100){
                    client!!.client?.sendFunc("PASV")
                    sleep(500)
                    client!!.client?.sendFunc("STOR small99$i")
                    sleep(2000)
                    i++
                }
            }.start()
        })

        home_bt_small3?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread {
                var i = 80
                while (i < 100){
                    client!!.client?.sendFunc("PASV")
                    sleep(500)
                    client!!.client?.sendFunc("RETR small99$i")
                    sleep(2000)
                    i++
                }
            }.start()
        })

        var imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager;
        imm.hideSoftInputFromWindow(home_edittext_message.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(home_edittext_ip.getWindowToken(), 0);


        handler = Handler { msg: Message ->
            val b = msg.data //获取消息中的Bundle对象
            val str = b.getString("data") //获取键为data的字符串的值
            home_tv_reply?.text = str + "\n" + home_tv_reply?.text
            false
        }

//        nameTextView = findViewById<TextView>(R.id.nametextview)
//        ipTextView = findViewById<TextView>(R.id.ipTextView)
//        mConnectivityManager =
//            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? //获取系统的连接服务
//        mActiveNetInfo = mConnectivityManager?.getActiveNetworkInfo() //获取网络连接的信息
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.action_secret -> Toast.makeText(this, "114514",
//                Toast.LENGTH_SHORT).show()
//        }
//        return true
//    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @SuppressLint("SetTextI18n")
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition
        cmd = spinner.adapter.getItem(position).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Another interface callback
    }


}