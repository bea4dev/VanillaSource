package com.github.bea4dev.vanilla_source.logger

import org.slf4j.LoggerFactory
import java.io.PrintStream

class STDOutLogger(original: PrintStream): PrintStream(original) {

    override fun println(x: Any?) {
        val stack = Thread.currentThread().stackTrace
        val caller = stack.getOrNull(2)
        val logger = if (caller == null) {
            LoggerFactory.getLogger("STDOUT")
        } else {
            LoggerFactory.getLogger(Class.forName(caller.className))
        }
        logger.info(x?.toString())
    }

    override fun println(x: Boolean) {
        this.println(x as Any)
    }

    override fun println(x: Char) {
        this.println(x as Any)
    }

    override fun println(x: CharArray) {
        this.println(x as Any)
    }

    override fun println(x: Double) {
        this.println(x as Any)
    }

    override fun println(x: Float) {
        this.println(x as Any)
    }

    override fun println(x: Int) {
        this.println(x as Any)
    }

    override fun println(x: Long) {
        this.println(x as Any)
    }

    override fun println(x: String?) {
        this.println(x as Any)
    }
}