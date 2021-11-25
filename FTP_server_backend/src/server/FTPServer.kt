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
        val parameters = message.split(" ", limit = 2)
        val cmd = parameters[0]
        val arg = parameters.getOrElse(1) { "" }

        return Pair(cmd, arg)
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

    //    private fun dispatch(cmd: String): (String) -> Pair<Int, String>? {
//        return when (cmd.uppercase()) {
//            "SYST" -> this::syst
//            "FEAT" -> this::feat
//            "MDTM" -> this::mdtm
//            "USER" -> this::user
//            "PASS" -> this::pass
//            "TYPE" -> this::type
//            "PWD" -> this::pwd
//            "CDUP" -> this::cdup
//            "CWD" -> this::cwd
//            "LIST" -> this::list
//            "RNFR" -> this::rnfr
//            "RNTO" -> this::rnto
//            "RETR" -> this::retr
//            "STOR" -> this::stor
//            "DELE" -> this::dele
//            "SIZE" -> this::size
//            "EPSV" -> this::epsv
//            "PASV" -> this::pasv
//            "EPRT" -> this::eprt
//            "PORT" -> this::port
//            "QUIT" -> this::quit
//
//            else -> this::error
//        }
//    }


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