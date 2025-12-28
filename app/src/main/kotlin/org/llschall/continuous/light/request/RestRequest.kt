package org.llschall.continuous.light.request

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

        // Build the request URL to fetch open pull requests
        val url = "https://api.github.com/repos/$repo/pulls?state=open&sort=created&direction=desc"

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
            for (prNode in tree) {
                val prNumber = prNode.get("number").asInt()
                val prTitle = prNode.get("title").asText()
                val headSha = prNode.get("head").get("sha").asText()

                // Check status checks for this PR
                checkStatusChecks(headSha, prNumber, prTitle, client)
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

    private fun checkStatusChecks(headSha: String, prNumber: Int, prTitle: String, client: HttpClient) {
        // Use the Check Runs API to get all check runs for this commit
        val url = "https://api.github.com/repos/$repo/commits/$headSha/check-runs"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .GET()
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                System.err.println("Failed to fetch check runs for PR #$prNumber")
                return
            }

            val responseBody = response.body()
            val jsonResponse = ObjectMapper().readTree(responseBody)
            val checkRuns = jsonResponse.get("check_runs")

            var allSuccessful = true
            var hasStatusChecks = false

            for (checkRun in checkRuns) {
                hasStatusChecks = true
                val status = checkRun.get("status").asText()
                val conclusion = checkRun.get("conclusion").asText()
                val name = checkRun.get("name").asText()

                println("PR #$prNumber ($prTitle) - $name: $status / $conclusion")

                // Check run is successful only if conclusion is "success"
                if (conclusion != "success") {
                    allSuccessful = false
                }
            }

            if (!hasStatusChecks) {
                System.err.println("PR #$prNumber: No check runs found - cannot merge")
            } else if (!allSuccessful) {
                System.err.println("PR #$prNumber cannot be merged: not all check runs are successful")
            } else {
                println("PR #$prNumber is ready to merge - all checks passed")
            }

        } catch (e: IOException) {
            System.err.println("Failed to check status checks: " + e.message)
        } catch (e: InterruptedException) {
            System.err.println("Failed to check status checks: " + e.message)
        }
    }
}
