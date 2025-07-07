def call(Maps args) {

    String projectID        = args.projectID
    String gitlabReportUrl  = args.gitlabReportUrl
    String gitLabToken      = args.gitLabToken
    String gitlabGroup      = args.gitlabGroup
    String gitlabProject    = args.gitlabProject

    // Default version if no valid tag is found
    def version = "dev"

    // Get the exact git tag associated with the latest commit
    def tag = bat(
        script: '''
        @echo off
        setlocal enabledelayedexpansion

        set TAG=

        for /f "delims=" %%H in ('git rev-parse HEAD') do set HASH=%%H
        for /f "delims=" %%T in ('git describe --exact-match --tags !HASH! 2^>nul') do set TAG=%%T

        endlocal
        ''',
        returnStdout: true
    ).trim()

    if (tag) {
        if (tag ==~ /^\d+\.\d+\.\d+-release$/) {
            version = tag.split("-")[0]
        } else {
            version = 'dev'
        }
    } else {
        version = 'dev'
    }

    // --- If the version is not 'dev', check if it already exists on GitLab ---
    if (version != "dev") {
        
        /*
        def apiUrl = "${gitlabReportUrl}/api/v4/projects/${GITLAB_REPORTS_PROJECT_ID}/repository/tree?path=${gitlabGroup}/${gitlabProject}/&ref=master"

        // Call the API via curl and capture the JSON response
        def response = bat(
            script: """
            @echo off
            curl -k -s -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" "${apiUrl}"
            """,
            returnStdout: true
        ).trim()

        try {
            // Parse the JSON response using Jenkins built-in method
            def jsonResponse = readJSON text: response

            // Check if a folder with the version name already exists
            def versionExists = jsonResponse.any { it.type == "tree" && it.name == version }

            if (versionExists) {
                error "Version ${version} was already built previously. You need to update it."
            } else {
                echo "Version '${version}' is available."
            }

        } catch (Exception e) {
            echo "Error parsing JSON response: ${e.message}"
            error("Aborting due to JSON response processing error.")
        }*/
    }

    return version
}
