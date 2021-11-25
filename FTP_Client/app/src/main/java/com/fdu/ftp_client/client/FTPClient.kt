package client

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.annotation.RequiresApi
import client.enum.FileStruEnum
import client.enum.TransferModeEnum
import client.enum.TransferTypeEnum
import client.exception.FTPException
import java.io.*
import java.net.*
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path

/**
 * FTP server功能的类
 * @param controlSocket
 * @param dataPorts
 * @param log
 * @param name
 */
@RequiresApi(Build.VERSION_CODES.O)
class FTPClient(
    private val appContext: Context,
    private val controlSocket: Socket,//来自manger的socket
    private val dataPorts: Set<Int>,//允许打开的端口范围
    private val log: Handler?,//输出log的流
    private val name: String,//这个线程的名字，每个线程会启动一个server
) {
    //control socket的输入输出流包装，方便信息收发
    private val outControl = PrintWriter(controlSocket.getOutputStream(), true)
    private val inControl = Scanner(controlSocket.getInputStream())
    val DELIMITER = "\n" //分隔符
    private val dateFormat = SimpleDateFormat("hh:mm:ss dd/MM/yy")//输出log的格式

    private var user: User? = null
    private var authenticated = false

    private var dataSocket: Socket? = null
    private var transferType = TransferTypeEnum.BINARY

    init {
        inControl.useDelimiter(DELIMITER);
    }

//    private fun tryOpenData(port: Int): Socket? {
//        try {
//
//        } catch (e: BindException) {
//            return null
//        }
//    }

//    private fun openData(it:Int): Socket? {
//        return try {
//                tryOpenData(it)
//        } catch (e: NoSuchElementException) {
//            null
//        }
//    }


    private fun closeData() {
        val dataSocket = dataSocket

        if (dataSocket != null && !dataSocket.isClosed)
            dataSocket.close()
    }

    fun destory() {
        outControl.close()
        inControl.close()
        controlSocket.close()
    }


    /**
     * 向log输出流输出，日期+message
     */
    private fun printLog(message: String) {
        val date = dateFormat.format(Date())
        if (log != null) {
            val msg = Message()
            val b = Bundle()
            b.putString("data","[$date] $message")
            msg.data = b
            log!!.sendMessage(msg)
        }
    }

    /**
     * rfc文档风格的log，记录输入的指令信息
     */
    private fun printLogIn(message: String) {
        printLog("server: $message-->")
    }

    /**
     * rfc文档风格的log，记录输出的信息
     */
    private fun printLogOut(message: String) {

        printLog("<--client: $message")
    }


    public fun sendFunc(message: String){
        if(dispatchSend(message)){
            send(message)
        }
        if(message.split(" ")[0].uppercase().trim()=="RETR"){
            retr(message)
        }
    }
    /**
     * 向client发送消息，同时log
     * @param message
     */
    private fun send(message: String) {
        outControl.println(message)
        printLogOut(message)
    }

    /**
     * 发送reply code（来自replycode.kt），和消息，用空格分割
     * @param code
     * @param message
     */
    private fun send(code: Int, message: String) {
        send("$code $message")
    }

    /**
     * 解析message，区分command， 返回pair
     */
    private fun parse(message: String): Pair<String, String> {
        val argv = message.split(" ", limit = 2)
        val cmd = argv[0]
        val arg = argv.getOrElse(1) { "" }

        return Pair(cmd, arg)
    }

    private fun split(arg: String): List<String> {
        return if (arg.isBlank())
            listOf()
        else
            arg.split(" ")
    }

    /**
     * 检查参数数量
     */
    private fun checkArgc(args: List<String>, argc: Int): Boolean {
        return argc == args.size
    }

    private fun assertArgc(args: List<String>, vararg argc: Int) {

        val ok = argc.map {
            checkArgc(args, it)
        }.contains(true)

        if (!ok) {
            throw FTPException(ERROR_ARGS, "This command requires ${argc.joinToString(", ")} argument(s)")
        }
    }

    /**
     * Parses and responds to a request
     * @param rawRequest the raw [String] request
     */
    private fun listenRequest(rawRequest: String) {
        printLogIn(rawRequest)

        val (cmdName, args) = parse(rawRequest)
        val command = dispatch(cmdName)

        try {
            val response = command(args)
            val msg = Message()
            val b = Bundle()
            b.putString("data",response)
            msg.data = b
            log?.sendMessage(msg)
        } catch (e: Exception) {
            val msg = Message()
            val b = Bundle()
            b.putString("data",e.message)
            msg.data = b
            log?.sendMessage(msg)
        }

    }

    fun listenForever() {
            inControl.forEachRemaining {
                listenRequest((it))
            }

    }
    private fun dispatchSend(cmd: String):Boolean {
        try{
            when (cmd.split(" ")[0].uppercase().trim()) {
                "TYPE" -> type(cmd)

                "STOR" -> stor(cmd)
                "STRU" -> stru(cmd)
                "MODE" -> mode(cmd)
                "PORT" -> port(cmd)
                "QUIT" -> quit(cmd)
                else -> noCommand(cmd)
            }
            return true
        }catch(e:Exception){
            printLog(e.message.toString())
            return false
        }
    }
    private fun dispatch(cmd: String): (String) -> String {
        return when (cmd.uppercase()) {
            "227" ->this::pasvn
            else -> this::noCommand
        }
    }

    private fun noCommand(arg: String): String {
        return ""
    }

    private fun quit(arg: String) {
            val args = split(arg)
            assertArgc(args, 0)
            throw QuitEvent()
    }

    private fun type(arg: String){
        val type = arg
        val transferType = TransferTypeEnum.values().first {
            it.code == type
        }
        this.transferType = transferType
    }

    private fun mode(arg: String){
        val mode = arg
        TransferModeEnum.values().first {
            it.code == mode
        }
    }

    private fun stru(arg: String){
        val stru = arg
        val fileStru = FileStruEnum.values().first {
            it.code == stru
        }
    }


    private fun getAddress() = Inet4Address.getLocalHost()

    private fun port(arg: String) {
        val args = split(arg)
        val delimiter = ','
        closeData()
        val bytes = args[1].split(delimiter).map(String::toUByte)
        Thread{
            this.dataSocket = ServerSocket((bytes[bytes.size-2].toInt()*256+bytes[bytes.size-1].toInt())).accept()
            printLog("Client open port success!")
        }.start()
    }

    private fun pasvn(arg: String): String {
        val args = split(arg)
        try {
            var bytes = args[3].split(",")
            val address = Inet4Address.getByAddress(bytes.subList(0, 4).map(String::toUByte).map(UByte::toByte).toByteArray())
            val port = Integer.parseInt(bytes[4])*256+Integer.parseInt(bytes[5])
            dataSocket = Socket(address, port)
            return "Connected to $address:$port"
        } catch (e: NumberFormatException) {
            return "Connect failed"
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFileAscii(path: Path, out: OutputStream) {
        PrintStream(out).use { printStream ->
            Files.lines(path, Charsets.US_ASCII).forEachOrdered { line ->
                printStream.println(line)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFileBinary(path: Path, out: OutputStream) {
        out.use { writer ->
            Files.newInputStream(path).use { reader ->
                transferTo(reader,writer)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFile(path: Path, out: OutputStream, transferType: TransferTypeEnum) {
        when (transferType) {
            TransferTypeEnum.BINARY -> sendFileBinary(path, out)
            TransferTypeEnum.ASCII -> sendFileAscii(path, out)
        }
    }

    private fun stor(arg: String){

        printLog(appContext.getExternalFilesDir("")!!.absolutePath+"/"+arg.split(" ")[1]);
        val path = Path(appContext.getExternalFilesDir("")!!.absolutePath+"/"+arg.split(" ")[1])
        val dataSocket = dataSocket

        return if (dataSocket != null) {
            sendFile(path, dataSocket.getOutputStream(), transferType)
            dataSocket.close()
        } else {
        }
    }


    private fun storeBinary(path: Path, input: InputStream) {
        input.use {
            Files.newOutputStream(path).use { out ->
                transferTo(input,out)
            }
        }
    }


    private fun storeAscii(path: Path, input: InputStream) {
        PrintStream(Files.newOutputStream(path)).use { out ->
            Scanner(input).use { scanner ->
                scanner.forEachRemaining(out::println)
            }
        }
    }


    private fun store(path: Path, input: InputStream, transferType: TransferTypeEnum) {
        when (transferType) {
            TransferTypeEnum.BINARY -> storeBinary(path, input)
            TransferTypeEnum.ASCII -> storeAscii(path, input)
        }
    }

    @Throws(IOException::class)
    fun transferTo(input:InputStream,out: OutputStream): Long {
        Objects.requireNonNull(out, "out")
        var transferred = 0L
        var read: Int
        val buffer = ByteArray(8192)
        while (input.read(buffer, 0, 8192).also { read = it } >= 0) {
            out.write(buffer, 0, read)
            transferred += read.toLong()
        }
        return transferred
    }

    private fun retr(arg: String){
        val path = appContext.getExternalFilesDir("")!!.absolutePath+"/"+arg.split(" ")[1]
        printLog(path)
        val dataSocket = dataSocket

        if (dataSocket != null) {
            try {
                store(Path(path), dataSocket.getInputStream(), transferType)
                Pair(CLOSING_DATA_CONNECTION, "Transfer complete")
            } catch (e: IOException) {
                printLog(e.message.toString())
            }
        } else {
        }
    }
}