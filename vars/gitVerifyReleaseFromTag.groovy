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

    /**
    * If a tag is found, the pipeline will generate a report for the current build.
    * However, it's possible that the pipeline gets triggered even when the tag 
    * already exists. In this case, the pipeline would overwrite previous results, 
    * since the tag is still valid. To prevent this, we query the SCM to check 
    * whether the target location (where the pipeline is supposed to write the 
    * report) is already populated.
    **/

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

        // If anything is not found, Gitlab replies at Jenkins with {"message":"404 Tree Not Found"}
        if (jsonResponse instanceof Map && jsonResponse.message) {}
        
        // If anything is found, Gitlab replies with a list of elements
        if (jsonResponse instanceof List) {
            def versionExists = jsonResponse.any { it.type == "tree" && it.name == version }
            if (versionExists) {
                error "Version ${version} already exists in the repository. You must update the tag."
            }
        }
    } catch (Exception e) {
        error "Aborting due to JSON repsonse processing error: ${e.message}"
    }

    return version
}
