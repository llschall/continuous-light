package org.llschall.continuous.light.request

class ConfigLoader {

    fun getToken(): String {
        return System.getenv("GITHUB_TOKEN")
    }
}