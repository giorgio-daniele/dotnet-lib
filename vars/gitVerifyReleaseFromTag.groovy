def call(Map args) {

    def gitlabReportUrl       = config.gitlabReportUrl
    def gitlabReportGroup     = config.gitlabReportGroup
    def gitlabReportProject   = config.gitlabReportProject
    def version               = "dev"
    //def gitlabReportProjectID = "${gitlabReportGroup}/${gitlabReportProject}"

    /**
      * The following script will inspect last commit with a tag. Once
      * the commit is selected, the tag is returned
      *
    **/

    def tag = bat(
        script: """
        @echo off
        setlocal enabledelayedexpansion

        set TAG=

        for /f "delims=" %%H in ('git rev-parse HEAD') do set HASH=%%H
        for /f "delims=" %%T in ('git describe --exact-match --tags !HASH! 2^>nul') do set TAG=%%T
        echo !TAG!
        endlocal
        """,
        returnStdout: true
    ).trim()

    /**
      * If the tag is compliant to x.y.z-release, then the application
      * verison will be set equal to such tag. Any other condition let
      * the version to be equal to "dev"
    **/

    if (tag) {
        if (tag ==~ /^\d+\.\d+\.\d+-release$/) {
            version = tag.split("-")[0]
        } else {
            version = "dev"
        }
    } else {
        version = "dev"
    }

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
