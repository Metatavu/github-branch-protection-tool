# branch-protection-tool

This tool is my for thesis work.
This is a tool for checking and updating GitHub branches and branch protection rules.

## Running the tool
To run project first  generate Apollo sources
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

You will have to give your personal access token as TOKEN and owner of the repository you wish to check as OWNER in environment variables.

This tool lack ability to check or set branch protection rules status checks. Please set these manually