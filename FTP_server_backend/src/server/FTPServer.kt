package server

import server.enum.TransferTypeEnum
import server.exception.FTPException
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

/**
 * FTP server功能的类
 * @param controlSocket
 * @param dataPorts
 * @param log
 * @param name
 */
class FTPServer(
    private val controlSocket: Socket,//来自manger的socket
    private val dataPorts: Set<Int>,//允许打开的端口范围
    private val log: PrintStream,//输出log的流
    private val name: String,//这个线程的名字，每个线程会启动一个server
) {
    //control socket的输入输出流包装，方便信息收发
    private val outControl = PrintWriter(controlSocket.getOutputStream(), true, Charsets.US_ASCII)
    private val inControl = Scanner(controlSocket.getInputStream(), Charsets.US_ASCII)
    val DELIMITER = "\n" //分隔符
    private val dateFormat = SimpleDateFormat("hh:mm:ss dd/MM/yy")//输出log的格式

    private var user: User? = null
    private var authenticated = false

    init {
        inControl.useDelimiter(DELIMITER);
        hello()
    }


    /**
     * 向log输出流输出，日期+message
     */
    private fun printLog(message: String) {
        val date = dateFormat.format(Date())
        synchronized(log) {
            log.println("[$date] $message")
        }
    }

    /**
     * rfc文档风格的log，记录输入的指令信息
     */
    private fun printLogIn(message: String) {
        printLog("$name: $message-->")
    }

    /**
     * rfc文档风格的log，记录输出的信息
     */
    private fun printLogOut(message: String) {

        printLog("<--$name: $message")
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
     * 解析message，区分command， 返回
     */
    private fun parse(message: String): Pair<String, String> {
        val argv = message.split(" ", limit = 2)
        val cmd = argv[0]
        val arg = argv.getOrElse(1) { "" }

        return Pair(cmd, arg)
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

//    /**
//     * Parses and responds to a request
//     * @param rawRequest the raw [String] request
//     */
//    private fun listenRequest(rawRequest: String) {
//        printLogIn(rawRequest)
//
//        val (cmdName, args) = parse(rawRequest)
//        val command = dispatch(cmdName)
//
//        try {
//            val response = command(args)
//            if (response != null)
//                send(response.first, response.second)
//
//        } catch (e: FTPException) {
//            send(e.code, e.message ?: "")
//        }
//
//    }

//        private fun dispatch(cmd: String): (String) -> Pair<Int, String>? {
//        return when (cmd.uppercase()) {
//            "USER" -> this::user
//            "PASS" -> this::pass
//            "TYPE" -> this::type
//            "RETR" -> this::retr
//            "STOR" -> this::stor
//            "NOOP" -> this::noop
//            "STRU" -> this::stru
//            "PASV" -> this::pasv
//            "EPRT" -> this::eprt
//            "PORT" -> this::port
//            "QUIT" -> this::quit
//
//            else -> this::error
//        }
//    }

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
                Pair(COMMAND_OK,  "Logged in as ${newUser.name}")
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


    private fun sendFileAscii(path: Path, out: OutputStream) {
        PrintStream(out).use { printStream ->
            Files.lines(path, Charsets.US_ASCII).forEachOrdered { line ->
                printStream.println(line)
            }
        }
    }

    private fun sendFileBinary(path: Path, out: OutputStream) {
        out.use { writer ->
            Files.newInputStream(path).use { reader ->
                reader.transferTo(writer)
            }
        }
    }

    private fun sendFile(path: Path, out: OutputStream, transferType: TransferTypeEnum) {
        when (transferType) {
            TransferTypeEnum.BINARY -> sendFileBinary(path, out)
            TransferTypeEnum.ASCII -> sendFileAscii(path, out)
        }
    }
}