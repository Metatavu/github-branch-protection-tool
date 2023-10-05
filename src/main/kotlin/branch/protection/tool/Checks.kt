package branch.protection.tool

import com.apollographql.apollo3.ApolloClient
import com.branch.protection.tool.*
import java.util.*

/**
 * Class for checks
 */
class Checks ( private val owner: String,  private val apolloClient: ApolloClient) {
    /**
     * Function to check if a branch exists and create it if necessary
     *
     * @param repositoryName repository name
     * @param repositoryId repository id
     * @param branchNode branch query node
     * @param branch branch name
     * @param oidTarget target branches name
     */


    suspend fun checkBranch(
        repositoryName: String,
        repositoryId: String,
        branchNode: RepositoryBranchesQuery.Node?,
        branch: String,
        oidTarget: String? = null
    ): Boolean {
        if (branchNode == null) {
            println("$branch branch is missing")
            if (branch != "main" && branch != "master") {
                println("Do you wish to add a $branch branch? (y/n):")
                while (true) {
                    val input = readln().trim().lowercase(Locale.getDefault())
                    if (input == "y" || input == "yes") {
                        val oid = apolloClient.query(
                            RepositoryBranchOidQuery(
                                owner = owner,
                                name = repositoryName,
                                branch = "refs/heads/$oidTarget"
                            )
                        ).execute()

                        val issue = apolloClient.mutation(
                            CreateIssueMutation(
                                repositoryId = repositoryId,
                                title = "Create $branch branch",
                                body = "Automatic creating of $branch branch"
                            )
                        ).execute()

                        apolloClient.mutation(
                            CreateBranchMutation(
                                repositoryId = repositoryId,
                                issueId = issue.data!!.createIssue!!.issue!!.id,
                                name = branch,
                                oid = oid.data!!.repository!!.ref!!.target!!.oid
                            )
                        ).execute()
                        println("$branch branch created")

                        apolloClient.mutation(
                            CloseIssueMutation(
                                issueId = issue.data!!.createIssue!!.issue!!.id
                            )
                        ).execute()
                        return true
                    } else if (input == "n" || input == "no") {
                        println("You chose not to create $branch branch.")
                        return false
                    } else {
                        println("Invalid input.")
                    }
                }
            } else {
                println("!!! Please set default branch to main or master at: https://github.com/$owner/$repositoryName/branches before you continue")
                return false
            }
        } else {
            println("$branch branch found")
            return true
        }
    }

    /**
     * Function to check branch protection rules and update them if needed
     *
     * @param repositoryId branch id
     * @param branch branch name
     * @param rule branch protection query node
     */
    suspend fun checkBranchProtection(
        repositoryId: String,
        branch: String,
        rule: ShowBranchProtectionQuery.Node?
    ) {
        if (rule == null) {
            println("$branch branch protection missing")
            println("Do you wish to add $branch branch protection rules? (y/n): ")
            while (true) {
                val input = readln().trim().lowercase(Locale.getDefault())
                if (input == "y" || input == "yes") {

                    if (branch == "main" || branch == "master") {
                        apolloClient.mutation(
                            CreateBranchProtectionMutation(
                                repositoryId = repositoryId,
                                branchPattern = branch,
                                statusCheck = false
                            )
                        ).execute()
                    } else {
                        apolloClient.mutation(
                            CreateBranchProtectionMutation(
                                repositoryId = repositoryId,
                                branchPattern = branch,
                                statusCheck = true
                            )
                        ).execute()
                    }
                    println("$branch branch protection created")
                    break
                } else if (input == "n" || input == "no") {
                    println("You chose not to create branch protection rule for $branch branch.")
                    break
                } else {
                    println("Invalid input.")
                }
            }

        } else if (!checkRules(rule)) {
            println("$branch branch protection is not up to date")
            println("Do you wish to update $branch branch protection rules? (y/n): ")

            while (true) {
                val input = readln().trim().lowercase(Locale.getDefault())

                if (input == "y" || input == "yes") {
                    if (branch == "main") {
                        apolloClient.mutation(
                            UpdateBranchProtectionMutation(
                                ruleId = rule.branchProtection.id,
                                branchPattern = branch,
                                statusCheck = false
                            )
                        ).execute()
                    } else {
                        apolloClient.mutation(
                            UpdateBranchProtectionMutation(
                                ruleId = rule.branchProtection.id,
                                branchPattern = branch,
                                statusCheck = true
                            )
                        ).execute()
                    }

                    println("$branch branch protection updated")
                    break
                } else if (input == "n" || input == "no") {
                    println("You chose not to update branch protection rule for $branch branch.")
                    break
                } else {
                    println("Invalid input.")
                }
            }
        } else {
            println("$branch branch protection is up to date")
        }
    }

    /**
     * Function to check if branch protection rules are in compliance
     *
     * @param rules branch protection query node
     * @return Status of rules
     */
    private fun checkRules(rules: ShowBranchProtectionQuery.Node): Boolean {
        val ruleErrors = mutableListOf<String>()

        if (!rules.branchProtection.requiresApprovingReviews) {
            ruleErrors.add("rules should require approving reviews")
        }
        if (rules.branchProtection.requiredApprovingReviewCount != 1) {
            ruleErrors.add("required approving review count should be 1")
        }
        if (!rules.branchProtection.requiresConversationResolution) {
            ruleErrors.add("require conversation resolution should be true")
        }
        if (rules.branchProtection.allowsDeletions) {
            ruleErrors.add("allows deletions should be false")
        }
        if (rules.branchProtection.dismissesStaleReviews) {
            ruleErrors.add("dismisses stale reviews should be false")
        }
        if (rules.branchProtection.isAdminEnforced) {
            ruleErrors.add("is admin enforced should be false")
        }
        if (rules.branchProtection.requiresCodeOwnerReviews) {
            ruleErrors.add("requires code owner reviews should be false")
        }
        if (rules.branchProtection.restrictsReviewDismissals) {
            ruleErrors.add("restricts review dismissals should be false")
        }
        if (rules.branchProtection.lockBranch) {
            ruleErrors.add("lock branch should be false")
        }
        if (rules.branchProtection.requiresDeployments ) {
            ruleErrors.add("requires deployments should be false")
        }
        if (rules.branchProtection.requiresLinearHistory) {
            ruleErrors.add("requires linear history should be false")
        }
        if (rules.branchProtection.requiresCommitSignatures) {
            ruleErrors.add("requires commit signatures should be false")
        }

        return if (ruleErrors.isNotEmpty()) {
            println(ruleErrors.joinToString(", "))
            false
        } else {
            true
        }
    }
}