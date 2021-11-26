package com.fdu.ftp_server

import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

class utilTest {

    @Test
    fun parse(){
        val message = "PASS mumble"
        val parameters = message.split(" ", limit = 2)
        val cmd = parameters[0]
        val arg = parameters.getOrElse(1) { "" }

        println(cmd)
        println(arg)
    }


//    @Test
//    fun sendtest(){
//        Files.newOutputStream(Path("/Users/hwwang/Downloads/big0000")).use{ out ->
//            sendFileBinary(Path("/Users/hwwang/Downloads/big0000"), out)
//        }
//    }
//
//    private fun sendFileBinary(path: Path, out: OutputStream) {
//        out.use { writer ->
//            Files.newInputStream(path).use { reader ->
//                transferTo(reader,writer)
//            }
//        }
//    }
//
//
//    @Throws(IOException::class)
//    fun transferTo(input: InputStream, out: OutputStream): Long {
//        Objects.requireNonNull(out, "out")
//        var transferred = 0L
//        var read: Int
//        val buffer = ByteArray(8192)
//        while (input.read(buffer, 0, 8192).also { read = it } >= 0) {
//            out.write(buffer, 0, read)
//            transferred += read.toLong()
//        }
//        return transferred
//    }
}