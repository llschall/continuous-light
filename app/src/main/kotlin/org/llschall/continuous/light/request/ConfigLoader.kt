package org.llschall.continuous.light.request

import kotlin.system.exitProcess

class ConfigLoader {

    fun getToken(): String {
        val token = System.getenv("GITHUB_TOKEN")
        if (token == null) {
            System.err.println("GITHUB_TOKEN environment variable not set.")
            exitProcess(0)
        }
        return token
    }
}