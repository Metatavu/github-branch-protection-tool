# branch-protection-tool

This tool is my for thesis work.
This is a tool for checking and updating GitHub branches and branch protection rules.

## Running the tool
To run project first set environment variables
OWNER = name of the owner whose repositories are being checked
TOKEN = GitHub personal access token with access to repositories

generate Apollo sources
```shell script
./gradlew generateApolloSources
```
Then build the project
```shell script
./gradlew build
```
and run it
```shell script
./gradlew run
```

!!! This tool lack ability to check or set branch protection rules status checks. Please set these manually
!!! The tool might have problems checking private or forked repositories 