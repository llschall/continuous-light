package org.llschall.action.light.request

class ConfigLoader {

    fun getToken(): String {
        return System.getenv("GITHUB_TOKEN")
    }
}