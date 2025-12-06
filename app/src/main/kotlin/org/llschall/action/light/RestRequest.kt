package org.llschall.action.light

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.system.exitProcess

class RestRequest(val repo: String, val token: String) {

    fun send() {

        // Build the request URL (owner/repo must be in the form owner/repo)
        val url = "https://api.github.com/repos/$repo/actions/runs?per_page=100"

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .GET() // Request the GitHub REST media type; this is the recommended header for the API
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")

        val request = reqBuilder.build()

        try {
            val response = client.send<String?>(request, HttpResponse.BodyHandlers.ofString())
            val statusCode = response.statusCode()

            if (statusCode != 200) {
                System.err.println("GitHub API returned non-OK status: $statusCode")
                System.err.println(response.body())
                exitProcess(3)
            }

            val body = response.body()!!

            // Very small, dependency-free approach: look for occurrences of status values in the JSON
            val inProgress = countOccurrences(body, "\"status\":\"in_progress\"")
            val queued = countOccurrences(body, "\"status\":\"queued\"")

            System.out.printf("Repository %s - in_progress: %d, queued: %d%n", repo, inProgress, queued)

            if (inProgress + queued > 0) {
                println("There are running or queued GitHub Actions runs.")
            } else {
                println("No running or queued GitHub Actions runs found.")
            }
        } catch (e: IOException) {
            // Print a helpful message for the user and propagate an exit code
            System.err.println("Failed to query GitHub API: " + e.message)
            Thread.currentThread().interrupt()
            exitProcess(4)
        } catch (e: InterruptedException) {
            System.err.println("Failed to query GitHub API: " + e.message)
            Thread.currentThread().interrupt()
            exitProcess(4)
        }
    }

    // Utility: counts non-overlapping occurrences of a substring in a string
    private fun countOccurrences(text: String, sub: String): Int {
        var idx = 0
        var count = 0
        while ((text.indexOf(sub, idx).also { idx = it }) != -1) {
            count++
            idx += sub.length
        }
        return count
    }

}