package com.github.bea4dev.vanilla_source.util

import org.contan_lang.ContanEngine
import org.contan_lang.ContanModule
import org.contan_lang.runtime.JavaContanFuture
import org.contan_lang.variables.primitive.ContanClassInstance
import org.contan_lang.variables.primitive.JavaClassObject


object ContanUtil {
    var emptyClassInstance: ContanClassInstance? = null
        private set
    private var utilModule: ContanModule? = null
    fun setUpContan(contanEngine: ContanEngine) {
        try {
            val utilCode = """
                const instance = new Empty()
                
                class Empty() {}
                
                function createFuture() { return new Future() }
                """.trimIndent()
            val utilModule = contanEngine.compile("vs_util", utilCode)
            this.utilModule = utilModule

            utilModule.initialize(contanEngine.mainThread)
            val instance = utilModule.moduleEnvironment.getVariable("instance")
                ?: throw IllegalStateException("")
            emptyClassInstance = instance.contanObject as ContanClassInstance
        } catch (e: Exception) {
            e.printStackTrace()
        }

        contanEngine.setRuntimeVariable("ContanUtil", JavaClassObject(contanEngine, ContanUtil::class.java))
    }

    fun createFutureInstance(): JavaContanFuture {
        return try {
            val future = utilModule!!.invokeFunction(
                null,// TODO : Implement thread
                "createFuture"
            ) as ContanClassInstance
            val reference = future.environment.getVariable("javaFuture") ?: throw IllegalStateException()
            reference.contanObject.convertToJavaObject() as JavaContanFuture
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

}
