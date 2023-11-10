package branch.protection.tool

import com.branch.protection.tool.*
import io.github.cdimascio.dotenv.dotenv
import java.util.*
import kotlin.system.exitProcess

/**
 * Main function of the app
 */
suspend fun main() {

    val dotenv = dotenv()
    val owner = dotenv["OWNER"]
    val apolloClient = Clients().apolloClient
    val check = Checks(owner = owner, apolloClient = apolloClient)

    do {
        println("Repository name: ")
        val repositoryName = readln()
        val repository = apolloClient.query(RepositoryBranchesQuery(owner = owner, name = repositoryName)).execute()
        val repositoryId = repository.data?.repository?.id

        if (repositoryId !== null) {
            val main = repository.data?.repository?.refs?.nodes?.find { repo -> repo?.name == "main" }
            val master = repository.data?.repository?.refs?.nodes?.find { repo -> repo?.name == "master" }
            val development = repository.data?.repository?.refs?.nodes?.find { repo -> repo?.name == "develop" }
            val rules = apolloClient.query(ShowBranchProtectionQuery(owner = owner, repo = repositoryName)).execute()
            val mainRule = rules.data?.repository?.branchProtectionRules?.nodes?.find { rule -> rule?.branchProtection?.pattern == "main" }
            val masterRule = rules.data?.repository?.branchProtectionRules?.nodes?.find { rule -> rule?.branchProtection?.pattern == "master" }
            val developRule = rules.data?.repository?.branchProtectionRules?.nodes?.find { rule -> rule?.branchProtection?.pattern == "develop" }

            var oidTarget: String

            /**
             * Check master or main branch and rules
             */
            if (master != null) {
                check.checkBranch(
                    repositoryName = repositoryName,
                    repositoryId = repositoryId,
                    branchNode = master,
                    branch = "master"
                )

                check.checkBranchProtection(
                    repositoryId = repositoryId,
                    branch = "master",
                    rule = masterRule
                )
                oidTarget = "master"
            } else {
                check.checkBranch(
                    repositoryName = repositoryName,
                    repositoryId = repositoryId,
                    branchNode = main,
                    branch = "main"
                )

                check.checkBranchProtection(
                    repositoryId = repositoryId,
                    branch = "main",
                    rule = mainRule
                )
                oidTarget = "main"
            }

            /**
             * Check develop branch and rules
             */
            val checkDevelop = check.checkBranch(
                repositoryName = repositoryName,
                repositoryId = repositoryId,
                branchNode = development,
                branch = "develop",
                oidTarget = oidTarget
            )

            if (checkDevelop) {
                check.checkBranchProtection(
                    repositoryId = repositoryId,
                    branch = "develop",
                    rule = developRule
                )
            }
            println("!!! This tool can not check branch protection rules status checks. Please check these at: https://github.com/$owner/$repositoryName/settings/branches")

        }else { println("could not find repository $repositoryName with owner $owner") }

        println("Do you wish to check another repository? (y/n): ")

        while (true) {
            val input = readln().trim().lowercase(Locale.getDefault())
            if (input == "y" || input == "yes") {
                break
            } else if (input == "n" || input == "no") {
                println("You chose to exit.")
                exitProcess(0)
            } else {
                println("Invalid input.")
            }
        }
    } while (true)
}


