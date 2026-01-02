package org.llschall.continuous.light.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class RestRequest(val token: String) {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val mapper = ObjectMapper()
    private val githubApiVersion = "2022-11-28"

    fun getAllUserRepos(username: String): List<String> {
        val repos = mutableListOf<String>()
        var page = 1

        while (true) {
            val url = "https://api.github.com/users/$username/repos?per_page=100&page=$page&sort=updated&direction=desc"
            val response = try {
                sendRequest(url)
            } catch (_: Exception) {
                break
            }


            val tree = mapper.readTree(response)
            if (tree !is ArrayNode || tree.isEmpty) {
                break
            }

            tree.mapNotNull { it.get("full_name")?.asText() }
                .forEach { repos.add(it) }

            page++
        }

        return repos
    }

    fun send(repos: List<String>): Int {
        return repos.sumOf { checkPullRequests(it) }
    }

    private fun checkPullRequests(repo: String): Int {
        val url = "https://api.github.com/repos/$repo/pulls?state=open&sort=created&direction=desc"
        val response = sendRequest(url)

        val tree = mapper.readTree(response) as? ArrayNode ?: return 0

        tree.forEachIndexed { _, prNode ->
            val prNumber = prNode.get("number").asInt()
            val prTitle = prNode.get("title").asText()
            val headSha = prNode.get("head").get("sha").asText()

            checkStatusChecks(repo, headSha, prNumber, prTitle)
        }

        return tree.size()
    }

    private fun checkStatusChecks(repo: String, headSha: String, prNumber: Int, prTitle: String) {
        val url = "https://api.github.com/repos/$repo/commits/$headSha/check-runs"
        val response = try {
            sendRequest(url)
        } catch (e: Exception) {
            System.err.println("Failed to fetch check runs for PR #$prNumber: ${e.message}")
            return
        }

        val jsonResponse = mapper.readTree(response)
        val checkRuns = jsonResponse.get("check_runs") as? ArrayNode ?: return

        if (checkRuns.isEmpty) {
            System.err.println("PR #$prNumber ($repo): No check runs found - cannot merge")
            return
        }

        val allSuccessful = checkRuns.all { it.get("conclusion").asText() == "success" }

        checkRuns.forEach { checkRun ->
            val status = checkRun.get("status").asText()
            val conclusion = checkRun.get("conclusion").asText()
            val name = checkRun.get("name").asText()
            println("PR #$prNumber ($repo) ($prTitle) - $name: $status / $conclusion")
        }

        when {
            !allSuccessful -> System.err.println("PR #$prNumber ($repo) cannot be merged: not all check runs are successful")
            else -> println("PR #$prNumber ($repo) is ready to merge - all checks passed")
        }
    }

    private fun sendRequest(url: String): String {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer $token")
                .header("X-GitHub-Api-Version", githubApiVersion)
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val body = response.body()

            when {
                response.statusCode() != 200 -> {
                    throw RuntimeException("GitHub API returned non-OK status: ${response.statusCode()}")
                }

                body.isNullOrEmpty() || body == "[]" -> {
                    throw RuntimeException("GitHub API returned empty response")
                }

                else -> body
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to query GitHub API: ${e.message}", e)
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to query GitHub API: ${e.message}", e)
        }
    }
}
