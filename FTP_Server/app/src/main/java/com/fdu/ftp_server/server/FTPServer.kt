package server

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.annotation.RequiresApi
import kotlinx.coroutines.internal.synchronized
import server.enum.FileStruEnum
import server.enum.TransferModeEnum
import server.enum.TransferTypeEnum
import server.exception.FTPException
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
class FTPServer(
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
        hello()
    }

    private fun tryOpenData(port: Int, onOpen: (Int) -> Unit): Socket? {
        try {
            ServerSocket(port).use {
                onOpen(port)
                return it.accept()
            }
        } catch (e: BindException) {
            return null
        }
    }

    private fun openData(onOpen: (Int) -> Unit): Socket? {
        return try {
            dataPorts.firstNotNullOf {
                tryOpenData(it, onOpen)
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }

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
            b.putString("data", "[$date] $message")
            msg.data = b
            log!!.sendMessage(msg)
        }
    }

    /**
     * rfc文档风格的log，记录输入的指令信息
     */
    private fun printLogIn(message: String) {
        printLog("client: $message-->")
    }

    /**
     * rfc文档风格的log，记录输出的信息
     */
    private fun printLogOut(message: String) {
        printLog("<--server: $message")
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
     * 输出hello world 提示连接建立
     */
    private fun hello() {
        send(COMMAND_OK, "Hello world!")
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
            throw FTPException(
                ERROR_ARGS,
                "This command requires ${argc.joinToString(", ")} argument(s)"
            )
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
            if (response != null)
                send(response.first, response.second)

        } catch (e: FTPException) {
            send(e.code, e.message ?: "")
        }

    }

    fun listenForever() {
        try {
            inControl.forEachRemaining {
                listenRequest((it))
            }

            throw inControl.ioException()
        } catch (quit: QuitEvent) {
            send(DISCONNECTION, "See ya.")
        } catch (e: SocketTimeoutException) {
            send(TIMEOUT, "Connection timed out.")
        } catch (e: IOException) {
            send(DISCONNECTION, "Fatal connection error")
        }
    }

    private fun dispatch(cmd: String): (String) -> Pair<Int, String>? {
        if (!authenticated) {
            return when (cmd.uppercase()) {
                "USER" -> this::user
                "PASS" -> this::pass
                "NOOP" -> this::noop

                "QUIT" -> this::quit

                else -> this::noCommand
            }
        } else {
            return when (cmd.uppercase()) {
                "USER" -> this::user
                "PASS" -> this::pass
                "TYPE" -> this::type
                "RETR" -> this::retr
                "STOR" -> this::stor
                "NOOP" -> this::noop
                "STRU" -> this::stru
                "PASV" -> this::pasv
                "MODE" -> this::mode
                "PORT" -> this::port
                "QUIT" -> this::quit

                else -> this::noCommand
            }
        }
    }

    private fun noCommand(arg: String): Pair<Int, String> {
        return Pair(COMMAND_NOT_IMPLEMENTED, "Command not implemented.")
    }

    private fun quit(arg: String): Nothing {
        val args = split(arg)
        assertArgc(args, 0)
        throw QuitEvent()
    }

    private fun noop(arg: String): Pair<Int, String> {
        return Pair(SERVICE_RDY, "Service ready")
    }

    private fun user(name: String): Pair<Int, String> {
        return try {
            val newUser = DEFAULT_USERS.first {
                it.name == name
            }

            user = newUser

            if (newUser.password != null) {
                Pair(USER_OK, "password needed")
            } else {
                authenticated = true
                Pair(COMMAND_OK, "Logged in as ${newUser.name}")
            }

        } catch (e: NoSuchElementException) {
            Pair(NOT_CONNECTED, "User unknown")
        }

    }


    private fun pass(password: String): Pair<Int, String> {
        val user = user

        return if (user == null) {
            Pair(NOT_CONNECTED, "send USER command before")
        } else if (user.password == password) {
            authenticated = true
            Pair(USER_LOGGED_IN, "Logged in as ${user.name}")
        } else {
            Pair(NOT_CONNECTED, "Wrong password")
        }
    }

    private fun type(arg: String): Pair<Int, String> {
        val type = arg

        return try {
            val transferType = TransferTypeEnum.values().first {
                it.code == type
            }

            this.transferType = transferType

            Pair(COMMAND_OK, "Changed transfer type to $transferType")
        } catch (e: NoSuchElementException) {
            Pair(ERROR_ARGS, "Unrecognized type")
        }

    }

    private fun mode(arg: String): Pair<Int, String> {
        val mode = arg

        return try {
            val transferMode = TransferModeEnum.values().first {
                it.code == mode
            }

            Pair(COMMAND_OK, "Changed transfer mode to $transferMode")
        } catch (e: NoSuchElementException) {
            Pair(ERROR_ARGS, "Unrecognized mode")
        }

    }

    private fun stru(arg: String): Pair<Int, String> {
        val stru = arg

        return try {
            val fileStru = FileStruEnum.values().first {
                it.code == stru
            }

            Pair(COMMAND_OK, "Changed FILE STRUCTURE to $fileStru")
        } catch (e: NoSuchElementException) {
            Pair(ERROR_ARGS, "Unrecognized structure")
        }

    }


    public fun getAddress() = Inet4Address.getLocalHost()

    private fun pasvMsg(port: Int) {
        assert(port < UShort.MAX_VALUE.toInt())

        val mod: Int = UByte.MAX_VALUE.toInt() + 1
        val address = getAddress().address

        send(
            ENTERING_PASSIVE,
            "Entering passive mode ${address[0]},${address[1]},${address[2]},${address[3]},${port / mod},${port % mod}"
        )
    }

    private fun pasv(arg: String): Pair<Int, String>? {
        val args = split(arg)
        assertArgc(args, 0)
        closeData()

        val dataSocket = openData(this::pasvMsg)

        return if (dataSocket != null) {
            this.dataSocket = dataSocket
            null
        } else {
            Pair(NOT_CONNECTED, "Unable to open a data port for connection")
        }
    }

    private fun port(arg: String): Pair<Int, String> {
        val args = split(arg)
        assertArgc(args, 1)

        val delimiter = ','
        val addressStr = args[0]

        return try {
            val bytes = addressStr.split(delimiter).map(String::toUByte)

            if (bytes.size != 6)
                return Pair(ERROR_ARGS, "Ill formed address")

            val address =
                Inet4Address.getByAddress(bytes.subList(0, 4).map(UByte::toByte).toByteArray())
            val port = bytes.subList(4, 6).map(UByte::toInt).reduce { i1, i2 ->
                i1 * 256 + i2
            }

            dataSocket = Socket(address, port)

            Pair(COMMAND_OK, "Connected to $address:$port")
        } catch (e: NumberFormatException) {
            Pair(ERROR_ARGS, "Ill formed address")
        } catch (e: IOException) {
            Pair(ERROR_ARGS, "Failed to connect")
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFileAscii(path: String, out: OutputStream) {
//        PrintStream(out).use { printStream ->
//            Files.lines(path, Charsets.US_ASCII).forEachOrdered { line ->
//                printStream.println(line)
//            }
//        }

        val outStream = PrintStream(out)
        val file = File(path)
        val a = Files.lines(file.toPath(), Charsets.US_ASCII)
        a.forEachOrdered { line ->
            outStream.println(line)
        }
        a.close()
        outStream.close()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFileBinary(path: String, out: OutputStream) {
        var file = File(path)
        var a = Files.newInputStream(file.toPath())
        transferTo(a, out)
        a.close()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFile(path: String, out: OutputStream, transferType: TransferTypeEnum) {
        when (transferType) {
            TransferTypeEnum.BINARY -> sendFileBinary(path, out)
            TransferTypeEnum.ASCII -> sendFileAscii(path, out)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun retr(arg: String): Pair<Int, String> {
        val path = appContext.getExternalFilesDir("")!!.absolutePath + "/" + arg
        val file = File(path)
        if (!file.exists()) {
            return Pair(553, "File name not allowed")
        }
        val dataSocket = dataSocket
        return if (dataSocket != null) {
            sendFile((path), dataSocket.getOutputStream(), transferType)
            dataSocket.close()

            Pair(CLOSING_DATA_CONNECTION, "Transfer complete, closing data connection")
        } else {
            Pair(NOT_CONNECTED, "The data connection is not established")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun storeBinary(path: String, input: InputStream) {
//        input.use {
//            Files.newOutputStream(path).use { out ->
//                transferTo(input,out)
//            }
//        }
        var file = File(path)
        var a = Files.newOutputStream(file.toPath())
        transferTo(input, a)
        a.close()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun storeAscii(path: String, input: InputStream) {
//        PrintStream(Files.newOutputStream(path)).use { out ->
//            Scanner(input).use { scanner ->
//                scanner.forEachRemaining(out::println)
//            }
//        }

        var file = File(path)
        var a = PrintStream(Files.newOutputStream(file.toPath()))
        var b = Scanner(input)
        b.forEachRemaining(a::println)
        b.close()
        a.close()
    }


    private fun store(path: String, input: InputStream, transferType: TransferTypeEnum) {
        when (transferType) {
            TransferTypeEnum.BINARY -> storeBinary(path, input)
            TransferTypeEnum.ASCII -> storeAscii(path, input)
        }
    }

    @Throws(IOException::class)
    fun transferTo(input: InputStream, out: OutputStream): Long {
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

    private fun stor(arg: String): Pair<Int, String> {
        val path = appContext.getExternalFilesDir("")!!.absolutePath + "/" + arg
        val dataSocket = dataSocket
        printLog(path)
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        printLog(path)
        return if (dataSocket != null) {
            try {
                store((path), dataSocket.getInputStream(), transferType)
                Pair(CLOSING_DATA_CONNECTION, "Transfer complete")
            } catch (e: IOException) {
                Pair(426, "An IO error occurred")
            }
        } else {
            Pair(NOT_CONNECTED, "Data connection not established")
        }
    }
}