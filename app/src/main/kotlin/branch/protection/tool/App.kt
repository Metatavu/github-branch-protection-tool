package branch.protection.tool

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.repository.standardization.tool.*
import io.github.cdimascio.dotenv.dotenv
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

suspend fun main() {

    val dotenv = dotenv()
    val owner = dotenv["OWNER"]
    val token = dotenv["TOKEN"]
    val scanner = Scanner(System.`in`)

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain: Interceptor.Chain ->
            val original: Request = chain.request()
            val builder: Request.Builder = original.newBuilder().method(original.method, original.body)
            builder.header("Authorization", "bearer $token")
            chain.proceed(builder.build())
        }
        .build()

    val apolloClient: ApolloClient = ApolloClient.Builder()
        .serverUrl("https://api.github.com/graphql")
        .okHttpClient(okHttpClient)
        .build()

    do {
        println("Repository name: ")
        val repositoryName = scanner.nextLine()

        val repository = apolloClient.query(RepositoryBranchesQuery(owner = owner, name = repositoryName)).execute()
        val repositoryId = repository.data?.repository?.id

        if (repositoryId !== null) {
            val rules = apolloClient.query(ShowBranchProtectionQuery(owner = owner, repo = repositoryName)).execute()
            val main = repository.data?.repository?.refs?.nodes?.find { repo -> repo?.name == "main" }

            if (main == null) {
                println("main branch is missing")
            } else {
                println("main branch is ok")
            }

            val development = repository.data?.repository?.refs?.nodes?.find { repo -> repo?.name == "develop" }

            if (development == null) {
                println("develop branch is missing")
                println("Do you wish to add a develop branch? (y/n):")
                while (true) {
                    val input = scanner.nextLine().trim().lowercase(Locale.getDefault())
                    if (input == "y" || input == "yes") {
                        val oid = apolloClient.query(
                            RepositoryBranchOidQuery(
                                owner = owner,
                                name = repositoryName
                            )
                        ).execute()

                        val issue = apolloClient.mutation(
                            CreateIssueMutation(
                                repositoryId = repositoryId,
                                title = "Create develop branch",
                                body = "Automatic creating of develop branch"
                            )
                        ).execute()

                        apolloClient.mutation(
                            CreateBranchMutation(
                                repositoryId = repositoryId,
                                issueId = issue.data!!.createIssue!!.issue!!.id,
                                name = "develop",
                                oid = oid.data!!.repository!!.ref!!.target!!.oid
                            )
                        ).execute()
                        println("develop branch created")

                        apolloClient.mutation(
                            CloseIssueMutation(
                                issueId = issue.data!!.createIssue!!.issue!!.id
                            )
                        )
                        break
                    } else if (input == "n" || input == "no") {
                        println("You chose to not to.")
                        break
                    } else {
                        println("Invalid input.")
                    }
                }
            }
            val developRule = rules.data?.repository?.branchProtectionRules?.nodes?.find { rule -> rule?.branchProtection?.pattern == "develop" }

            if (developRule == null) {
                println("develop branch protection missing")

                println("Do you wish to add branch protection rules? (y/n): ")
                while (true) {
                    val input = scanner.nextLine().trim().lowercase(Locale.getDefault())
                    if (input == "y" || input == "yes") {
                        apolloClient.mutation(
                            CreateBranchProtectionMutation(
                                repositoryId = repositoryId,
                                branchPattern = "develop"
                            )
                        ).execute()
//
                        println("develop branch protection created")
                        break
                    } else if (input == "n" || input == "no") {
                        println("You chose to not to.")
                        break
                    } else {
                        println("Invalid input.")
                    }
                }

            } else if (!checkRules(developRule)){
                println("develop branch protection not up to date")
                println("Do you wish to update branch protection rules? (y/n): ")

                while (true) {
                    val input = scanner.nextLine().trim().lowercase(Locale.getDefault())
                    if (input == "y" || input == "yes") {
                        apolloClient.mutation(
                            UpdateBranchProtectionMutation(
                                ruleId = developRule.branchProtection.id,
                                branchPattern = "develop"
                            )
                        ).execute()
//                    println(updateRule.data?.updateBranchProtectionRule?.branchProtectionRule)
                        println("develop branch protection updated")
                        break
                    } else if (input == "n" || input == "no") {
                        println("You chose to not to.")
                        break
                    } else {
                        println("Invalid input.")
                    }
                }
            } else {
                println("develop branch is ok")
            }

        }else { println("invalid info") }

        print("Do you wish to check another repository? (y/n): ")

        while (true) {
            val input = scanner.nextLine().trim().lowercase(Locale.getDefault())
            if (input == "y" || input == "yes") {
                break
            } else if (input == "n" || input == "no") {
                println("You chose to exit.")
                exitProcess()
            } else {
                println("Invalid input.")
            }
        }
    } while (true)

}

private fun exitProcess() {
    System.exit(1)
}

private fun checkRules(rules: ShowBranchProtectionQuery.Node): Boolean {
    return !rules.branchProtection.allowsDeletions &&
            !rules.branchProtection.allowsForcePushes &&
            !rules.branchProtection.dismissesStaleReviews &&
            !rules.branchProtection.isAdminEnforced &&
            rules.branchProtection.pattern == "develop" &&
            rules.branchProtection.requiresApprovingReviews &&
            rules.branchProtection.requiredApprovingReviewCount == 1 &&
            !rules.branchProtection.requiresCodeOwnerReviews &&
            rules.branchProtection.requiresStatusChecks &&
            rules.branchProtection.requiresConversationResolution &&
            !rules.branchProtection.restrictsReviewDismissals
}