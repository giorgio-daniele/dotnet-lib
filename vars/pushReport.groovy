def call(Map args) {

    // Validate required arguments
    if (!args.results) {
        error "'results' is required."
    }
    if (!args.gitlabUrl) {
        error "'gitlabUrl' is required."
    }
    if (!args.projectPath) {
        error "'projectPath' is required."
    }

    def results     = args.results
    def gitlabUrl   = args.gitlabUrl
    def projectPath = args.projectPath
    def appVersion  = args.applicationVersion ?: "dev"
    def isDevBuild  = appVersion == "dev"

    if (!isDevBuild) {
        def qaResult         = results["quality-assurance.result"]    ?: "N/A"
        def sonarResult      = results["sonarqube.result"]            ?: "N/A"
        def sonarReportUrl   = results["sonarqube.report-url"]        ?: "N/A"
        def sonarDescription = results["sonarqube.description"]       ?: "N/A"
        def dtResult         = results["dependencytrack.result"]      ?: "N/A"
        def dtReportUrl      = results["dependencytrack.url"]         ?: "N/A"
        def dtDescription    = results["dependencytrack.description"] ?: "N/A"

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

        def timestamp      = new Date().format("yyyyMMdd_HHmmss")
        def reportFileName = "${timestamp}_metadata.xml"
        writeFile file: reportFileName, text: xmlContent

        def encodedPath  = java.net.URLEncoder.encode(projectPath, "UTF-8").replaceAll("%", "%%")

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

        bat """
            @echo off
            setlocal
            set "PROJECT=${encodedPath}"
            set "CMTS=${gitlabUrl}/api/v4/projects/%PROJECT%/repository/commits"

            curl -k                                       ^
                --request POST                            ^
                --header "PRIVATE-TOKEN: %TOKEN%"         ^
                --header "Content-Type: application/json" ^
                --data @payload.json                      ^
                "%CMTS%" > response.json

            type response.json
            endlocal
        """
    }
}
