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
@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
        private var ipTextView: TextView? = null
    private var nameTextView: TextView? = null
    private var mConnectivityManager: ConnectivityManager? = null
    private var mActiveNetInfo: NetworkInfo? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    //显示或处理服务器返回数据
    var handler: Handler? = null

    //客户端
    var client: FTPSocketManger ? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)


        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_file, R.id.nav_aboutinfo
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        home_edittext_ip?.setText("192.168.43.1")
        home_bt_connect?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread{
                client = FTPSocketManger(applicationContext,handler,home_edittext_ip?.text.toString())
                client!!.create()
            }.start()
            home_edittext_ip?.isEnabled = false
            home_bt_connect?.isEnabled = false
        })
        home_bt_communicate?.setOnClickListener(View.OnClickListener { v: View? ->
            Thread {
                client!!.send(home_edittext_message?.getText().toString())
            }.start()
        })
        var imm : InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager;
        imm.hideSoftInputFromWindow(home_edittext_message.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(home_edittext_ip.getWindowToken(), 0);


        handler = Handler { msg: Message ->
            val b = msg.data //获取消息中的Bundle对象
            val str = b.getString("data") //获取键为data的字符串的值
            home_tv_reply?.text = str+"\n"+home_tv_reply?.text
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