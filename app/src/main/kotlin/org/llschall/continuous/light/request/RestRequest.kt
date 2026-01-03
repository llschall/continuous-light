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
            val response = sendRequest(url) ?: break

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

    fun send(repos: List<String>): List<Status> {
        val list = mutableListOf<Status>()
        for (repo in repos) {
            list.addAll(checkPullRequests(repo))
        }
        return list
    }

    private fun checkPullRequests(repo: String): List<Status> {
        val url = "https://api.github.com/repos/$repo/pulls?state=open&sort=created&direction=desc"
        val response = sendRequest(url) ?: return emptyList()

        val tree = mapper.readTree(response) as? ArrayNode ?: return emptyList()

        val list = mutableListOf<Status>()

        tree.forEachIndexed { _, prNode ->
            val prNumber = prNode.get("number").asInt()
            val prTitle = prNode.get("title").asText()
            val headSha = prNode.get("head").get("sha").asText()

            list.addAll(checkStatusChecks(repo, headSha, prNumber, prTitle))
        }
        return list
    }

    private fun checkStatusChecks(repo: String, headSha: String, prNumber: Int, prTitle: String): List<Status> {
        val url = "https://api.github.com/repos/$repo/commits/$headSha/check-runs"
        val response = sendRequest(url) ?: run {
            System.err.println("Failed to fetch check runs for PR #$prNumber")
            return emptyList()
        }

        val jsonResponse = mapper.readTree(response)
        val checkRuns = jsonResponse.get("check_runs") as? ArrayNode ?: return emptyList()

        if (checkRuns.isEmpty) {
            System.err.println("PR #$prNumber ($repo): No check runs found - cannot merge")
            return emptyList()
        }

        val list = mutableListOf<Status>()

        checkRuns.forEach { checkRun ->
            val status = checkRun.get("status").asText()
            val conclusion = checkRun.get("conclusion").asText()
            val name = checkRun.get("name").asText()
            println("PR #$prNumber ($repo) ($prTitle) - $name: [$status]  $conclusion")
            list.add(Status.UNKNOWN.fromString(status))
        }
        return list
    }

    private fun sendRequest(url: String): String? {
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
                    System.err.println("GitHub API returned non-OK status: ${response.statusCode()}")
                    null
                }

                body.isNullOrEmpty() || body == "[]" -> null
                else -> body
            }
        } catch (e: IOException) {
            System.err.println("Failed to query GitHub API: ${e.message}")
            null
        } catch (e: InterruptedException) {
            System.err.println("Failed to query GitHub API: ${e.message}")
            null
        }
    }
}
