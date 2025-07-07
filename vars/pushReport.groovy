def call(Map args) {
    // Required arguments
    if (!args.results) {
        error "'results' is required."
    }

    if (!args.gitlabBaseUrl) {
        error "'gitlabBaseUrl' is required."
    }

    if (!args.projectPath) {
        error "'projectPath' is required"
    }

    // Argument unpacking
    def results       = args.results
    def gitlabBaseUrl = args.gitlabBaseUrl
    def projectPath   = args.projectPath
    def appVersion    = args.applicationVersion ?: "dev"
    def isDevBuild    = appVersion == "dev"

    if (!isDevBuild) {
        // Extract values with fallback to 'N/A'
        def qaResult         = results["quality-assurance.result"]    ?: "N/A"
        def sonarResult      = results["sonarqube.result"]            ?: "N/A"
        def sonarReportUrl   = results["sonarqube.report-url"]        ?: "N/A"
        def sonarDescription = results["sonarqube.description"]       ?: "N/A"
        def dtResult         = results["dependencytrack.result"]      ?: "N/A"
        def dtReportUrl      = results["dependencytrack.url"]         ?: "N/A"
        def dtDescription    = results["dependencytrack.description"] ?: "N/A"

        // Build XML content
        def xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
                <quality-assurance>
                    <result>${qaResult}</result>
                    <sonarqube>
                        <result>${sonarResult}</result>
                        <report-url>${sonarReportUrl}</report-url>
                        <description>${sonarDescription}</description>
                    </sonarqube>
                    <dependencytrack>
                        <result>${dtResult}</result>
                        <report-url>${dtReportUrl}</report-url>
                        <description>${dtDescription}</description>
                    </dependencytrack>
                </quality-assurance>
            </metadata>
        """.stripIndent()

        // Write the XML report file
        def timestamp      = new Date().format("yyyyMMdd_HHmmss")
        def reportFileName = "${timestamp}_metadata.xml"
        writeFile file: reportFileName, text: xmlContent

        // Encode project path for GitLab API
        def encodedPath    = java.net.URLEncoder.encode(projectPath, "UTF-8")
        def escapedPath    = encodedPath.replaceAll("%", "%%")

        // Write the GitLab commit payload
        def payload = [
            branch: "main",
            commit_message: "Add file ${reportFileName}",
            actions: [[
                action:    "create",
                file_path: "reports/${reportFileName}",
                content:   readFile(reportFileName)
            ]]
        ]
        writeFile file: "payload.json", text: groovy.json.JsonOutput.toJson(payload)

        // Send the commit to GitLab API
        bat """
            @echo off
            setlocal
            set "PROJECT_PATH=${escapedPath}"
            set "API_URL=${gitlabBaseUrl}/api/v4/projects/%PROJECT_PATH%/repository/commits"

            curl -k                                       ^
                --request POST                            ^
                --header "PRIVATE-TOKEN: %TOKEN%"         ^
                --header "Content-Type: application/json" ^
                --data @payload.json                      ^
                "%API_URL%" > response.json

            type response.json
            endlocal
        """
    }
}
