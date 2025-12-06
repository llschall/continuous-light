package org.llschall.action.light.request

import com.fasterxml.jackson.databind.ObjectMapper
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
        // Only get the latest runs, sorted by created date in descending order
        val url = "https://api.github.com/repos/$repo/actions/runs?per_page=10&sort=created&direction=desc"

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .GET() // Request the GitHub REST media type; this is the recommended header for the API
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .header("X-GitHub-Api-Version", "2022-11-28")

        val request = reqBuilder.build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val statusCode = response.statusCode()

            if (statusCode != 200) {
                System.err.println("GitHub API returned non-OK status: $statusCode")
                System.err.println(response.body())
                exitProcess(3)
            }

            val body = response.body()
            if (body == null) {
                System.err.println("GitHub API returned empty body")
                exitProcess(5)
            }

            val tree = ObjectMapper().readTree(body)
            for (node in tree.get("workflow_runs")) {
                val status = node.get("status")
                // get the workflow name
                val name = node.get("name")
                // get the workflow run conclusion
                val conclusion = node.get("conclusion")
                println("=> $name $status $conclusion")
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
}