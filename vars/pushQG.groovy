def call(Map args) {

    def QA_RESULT                 = config.QA_RESULT
    def APPLICATION_VERSION       = config.APPLICATION_VERSION
    //def GITLAB_REPORTS_PROJECT_ID = config.GITLAB_REPORTS_PROJECT_ID
    def gitlabReportUrl           = config.gitlabReportUrl
    
    if (!APPLICATION_VERSION.equals("dev")) {

        def qaResult         = QA_RESULT["quality-assurance.result"]    ?: "N/A"
        def sonarResult      = QA_RESULT["sonarqube.result"]            ?: "N/A"
        def sonarReportUrl   = QA_RESULT["sonarqube.report-url"]        ?: "N/A"
        def sonarDescription = QA_RESULT["sonarqube.description"]       ?: "N/A"
        def dtResult         = QA_RESULT["dependencytrack.result"]      ?: "N/A"
        def dtReportUrl      = QA_RESULT["dependencytrack.url"]         ?: "N/A"
        def dtDescription    = QA_RESULT["dependencytrack.description"] ?: "N/A"

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
