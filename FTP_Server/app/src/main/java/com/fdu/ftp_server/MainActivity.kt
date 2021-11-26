package com.fdu.ftp_server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.fdu.ftp_server.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.InternalCoroutinesApi
import server.FTPServer
import server.FTPSocketManger
import java.net.Inet4Address
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var ipTextView: TextView? = null
    private var nameTextView: TextView? = null
    private var mConnectivityManager: ConnectivityManager? = null
    private var mActiveNetInfo: NetworkInfo? = null


    //显示或处理服务器返回数据
    var handler_Server: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)


        val drawerLayout: DrawerLayout = binding.activityMian
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,  R.id.nav_aboutinfo
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        home_bt_startServer?.setOnClickListener(View.OnClickListener { v: View? ->
            Log.d("home_bt_startServer", "home_bt_startServer clicked")
            Thread {    FTPSocketManger(applicationContext,handler_Server).listen() }
                .start()
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()

            home_bt_startServer?.isEnabled = false
        })

        handler_Server = Handler { msg: Message ->
            val b = msg.data //获取消息中的Bundle对象
            val str = b.getString("data") //获取键为data的字符串的值
            home_tv_reply_server?.text = str + "\n" + home_tv_reply_server?.text
            false
        }
        nameTextView = findViewById<TextView>(R.id.nametextview)
        ipTextView = findViewById<TextView>(R.id.ipTextView)
        mConnectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? //获取系统的连接服务
        mActiveNetInfo = mConnectivityManager?.getActiveNetworkInfo() //获取网络连接的信息
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
}