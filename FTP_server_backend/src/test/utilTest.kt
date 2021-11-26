package test

import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

class utilTest {

    @Test
    fun parse() {
        val message = "PASS mumble"
        val parameters = message.split(" ", limit = 2)
        val cmd = parameters[0]
        val arg = parameters.getOrElse(1) { "" }

        println(cmd)
        println(arg)
    }


    @Test
    fun sendtest() {
//        val file: Path = Paths.get("/Users/hwwang/Downloads/small0000")
//        if (Files.exists(file)) {
//            println("1")
//        }
        var s = Files.newOutputStream(Paths.get("/Users/hwwang/Downloads/big0000.1"))
        sendFileBinary("/Users/hwwang/Downloads/big0000",s)
        s.close()

//        var file = File("/Users/hwwang/Downloads/small0000")
//        var a = Files.newInputStream(file.toPath())
//        val buffer = ByteArray(10)
//        a.read(buffer)
//        println(buffer.toString())
//        //a.transferTo(System.out)
//        a.close()
    }

    private fun sendFileAscii(path: Path, out: OutputStream) {
        PrintStream(out, true).use { printStream ->
            Files.lines(path, Charsets.US_ASCII).forEachOrdered { line ->
                printStream.println(line)
            }
        }
    }

    private fun sendFileBinary(path: String, out: OutputStream) {
//        out.use { writer ->
//            Files.newInputStream(path).use { reader ->
//                transferTo(reader, writer)
//            }
//        }

        var file = File(path)
        var a = Files.newInputStream(file.toPath())
        transferTo(a, out)
        a.close()

    }


    @Throws(IOException::class)
    fun transferTo(input: InputStream, out: OutputStream): Long {
        Objects.requireNonNull(out, "out")
        var transferred = 0L
        var read: Int
        val buffer = ByteArray(8192)
        while (input.read(buffer).also { read = it } >= 0) {
            out.write(buffer,0, read)
            transferred += read.toLong()
        }
        return transferred
    }


}