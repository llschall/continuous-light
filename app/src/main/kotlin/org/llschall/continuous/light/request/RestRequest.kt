package org.llschall.continuous.light.request

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class RestRequest(val token: String) {

    fun getAllUserRepos(username: String): List<String> {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val repos = mutableListOf<String>()
        var page = 1

        while (true) {
            val url = "https://api.github.com/users/$username/repos?per_page=100&page=$page&sort=updated&direction=desc"

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
                    System.err.println("Failed to fetch repos for $username: ${response.statusCode()}")
                    break
                }

                val body = response.body()
                if (body == null || body.isEmpty() || body == "[]") {
                    break
                }

                val tree = ObjectMapper().readTree(body)
                if (tree.isEmpty) {
                    break
                }

                for (repoNode in tree) {
                    val fullName = repoNode.get("full_name").asText()
                    repos.add(fullName)
                }

                page++
            } catch (e: IOException) {
                System.err.println("Failed to fetch repos for $username: ${e.message}")
                break
            } catch (e: InterruptedException) {
                System.err.println("Failed to fetch repos for $username: ${e.message}")
                break
            }
        }

        return repos
    }

    fun send(repos: List<String>): Int {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        var totalPRCount = 0
        for (repo in repos) {
            totalPRCount += checkPullRequests(repo, client)
        }
        return totalPRCount
    }

    private fun checkPullRequests(repo: String, client: HttpClient): Int {
        // Build the request URL to fetch open pull requests
        val url = "https://api.github.com/repos/$repo/pulls?state=open&sort=created&direction=desc"

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
                System.err.println("GitHub API returned non-OK status for $repo: $statusCode")
                System.err.println(response.body())
                return 0
            }

            val body = response.body()
            if (body == null) {
                System.err.println("GitHub API returned empty body for $repo")
                return 0
            }

            val tree = ObjectMapper().readTree(body)
            var prCount = 0
            for (prNode in tree) {
                prCount++
                val prNumber = prNode.get("number").asInt()
                val prTitle = prNode.get("title").asText()
                val headSha = prNode.get("head").get("sha").asText()

                // Check status checks for this PR
                checkStatusChecks(repo, headSha, prNumber, prTitle, client)
            }
            return prCount

        } catch (e: IOException) {
            // Print a helpful message for the user and propagate an exit code
            System.err.println("Failed to query GitHub API for $repo: " + e.message)
            return 0
        } catch (e: InterruptedException) {
            System.err.println("Failed to query GitHub API for $repo: " + e.message)
            return 0
        }
    }

    private fun checkStatusChecks(repo: String, headSha: String, prNumber: Int, prTitle: String, client: HttpClient) {
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

                println("PR #$prNumber ($repo) ($prTitle) - $name: $status / $conclusion")

                // Check run is successful only if conclusion is "success"
                if (conclusion != "success") {
                    allSuccessful = false
                }
            }

            if (!hasStatusChecks) {
                System.err.println("PR #$prNumber ($repo): No check runs found - cannot merge")
            } else if (!allSuccessful) {
                System.err.println("PR #$prNumber ($repo) cannot be merged: not all check runs are successful")
            } else {
                println("PR #$prNumber ($repo) is ready to merge - all checks passed")
            }

        } catch (e: IOException) {
            System.err.println("Failed to check status checks: " + e.message)
        } catch (e: InterruptedException) {
            System.err.println("Failed to check status checks: " + e.message)
        }
    }
}
