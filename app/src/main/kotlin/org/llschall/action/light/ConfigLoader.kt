package org.llschall.action.light

class ConfigLoader {

    fun getToken(): String {
        return System.getenv("GITHUB_TOKEN")
    }
}