package test

import org.junit.Test

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


}