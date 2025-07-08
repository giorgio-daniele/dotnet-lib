def call(Map config) {

    def QA_RESULT             = config.QA_RESULT
    def APPLICATION_VERSION   = config.APPLICATION_VERSION
    def gitlabReportUrl       = config.gitlabReportUrl
    def gitlabReportGroup     = config.gitlabReportGroup
    def gitlabReportProject   = config.gitlabReportProject

    // if (!"dev".equals(APPLICATION_VERSION)) {

        def qaResult         = QA_RESULT["quality-assurance.esito"]     ?: "N/A"
        def sonarResult      = QA_RESULT["sonarqube.esito"]             ?: "N/A"
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

        // Define the name of the file
        def timestamp     = new Date().format("yyyyMMdd_HHmmss")
        def safeQaResult  = qaResult.replaceAll(/[^a-zA-Z0-9_\-]/, "_")
        def nameFile      = "${timestamp}/${gitlabReportGroup}/${gitlabReportProject}/${APPLICATION_VERSION}/QG${safeQaResult}_report.xml"
        def dirPath       = nameFile.substring(0, nameFile.lastIndexOf("/"))

        // Create folder path if it doesn't exist (Windows)
        bat "mkdir \"${dirPath}\" >nul 2>&1"

        // Write the XML content to the file
        writeFile file: nameFile, text: xmlContent

        def projectPath = "${gitlabReportGroup}/${gitlabReportProject}"
        def encodedPath = java.net.URLEncoder.encode(projectPath, "UTF-8")

        echo "Publishing report to GitLab: ${nameFile}"

        // Unique filenames for temp files
        def uuid          = UUID.randomUUID().toString()
        def payloadFile   = "payload_${uuid}.json"
        def responseFile  = "response_${uuid}.json"

        // Create JSON payload
        def payload = [
            branch: "main",
            commit_message: "Add file ${nameFile}",
            actions: [[
                action:     "create",
                file_path:  nameFile,
                content:    readFile(nameFile)
            ]]
        ]

        writeFile file: payloadFile, text: groovy.json.JsonOutput.toJson(payload)

        // Send the request via cURL (Windows Batch)
        bat """
            @echo off
            setlocal
            set "PROJECT=${encodedPath}"
            set "URL=${gitlabReportUrl}/api/v4/projects/%PROJECT%/repository/commits"
            set "PAYLOAD=${payloadFile}"
            set "OUT=${responseFile}"

            curl -k                                       ^
                --request POST                            ^
                --header "PRIVATE-TOKEN: %GITLAB_TOKEN%"  ^
                --header "Content-Type: application/json" ^
                --data @%PAYLOAD%                         ^
                "%URL%" > %OUT%

            type %OUT%
            endlocal
        """
    // }
}
