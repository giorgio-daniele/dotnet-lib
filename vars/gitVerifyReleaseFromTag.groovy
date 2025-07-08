def call(Map config) {

    def gitlabUrl              = config.gitlabUrl
    def gitlabReportProjectId  = config.gitlabReportProjectId
    def gitlabReportGroup      = config.gitlabReportGroup
    def gitlabReportProject    = config.gitlabReportProject
    def version                = "dev"

    /**
      * The following script will inspect last commit with a tag. Once
      * the commit is selected, the tag is returned
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
      * version will be set equal to such tag. Any other condition lets
      * the version be equal to "dev"
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

    /*
    if (version == "dev") {
        return
    }*/

    // Only run this block if version is a proper release
    def apiUrl = "${gitlabUrl}/api/v4/projects/${gitlabReportProjectId}/repository/tree?path=${gitlabReportGroup}/${gitlabReportProject}/&ref=master"

    def response = bat(
        script: """
        @echo off
        setlocal enabledelayedexpansion

        set "TOKEN=!GITLAB_TOKEN!"
        set "URL=${apiUrl}"

        curl -k -s -H "PRIVATE-TOKEN: !TOKEN!" "!URL!"

        endlocal
        """,
        returnStdout: true
    ).trim()

    echo "GitLab API response: ${response}"

    try {
        def jsonResponse  = readJSON text: response
        def versionExists = jsonResponse.any { it.type == "tree" && it.name == version }

        if (versionExists) {
            error "Version ${version} was already built previously. You need to update it."
        } else {
            echo "Version '${version}' is available."
        }

    } catch (Exception e) {
        echo "Error parsing JSON response: ${e.message}"
        error "Aborting due to JSON repsonse processing error"
    }

    return version
}
