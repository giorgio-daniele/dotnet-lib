def call(Map config) {

    def QA_RESULT             = config.QA_RESULT
    def APPLICATION_VERSION   = config.APPLICATION_VERSION
    def gitlabReportProjectId = config.gitlabReportProjectId
    def gitlabReportUrl       = config.gitlabReportUrl
    def gitlabGroup           = config.gitlabGroup
    def gitlabPproject        = config.gitlabPproject
    
    /*
    if(APPLICATION_VERSION == "dev") {
        echo "[INFO] Skipping pushQG: running in dev environment"
        return
    }*/

    if (QA_RESULT.get("sonarqube.esito") == "KO"){
        QA_RESULT.put("quality-assurance.esito", "KO")
    } else QA_RESULT.put("quality-assurance.esito", "OK")

    // Extract values from QA_RESULT map with defaults
    def qaResult         = QA_RESULT["quality-assurance.esito"]     ?: "N/A"
    def sonarResult      = QA_RESULT["sonarqube.esito"]             ?: "N/A"
    def sonarReportUrl   = QA_RESULT["sonarqube.report-url"]        ?: "N/A"
    def sonarDescription = QA_RESULT["sonarqube.description"]       ?: "N/A"
    def dtResult         = QA_RESULT["dependencytrack.result"]      ?: "N/A"
    def dtReportUrl      = QA_RESULT["dependencytrack.url"]         ?: "N/A"
    def dtDescription    = QA_RESULT["dependencytrack.description"] ?: "N/A"

    // Build XML content for the report
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

    // Compose remote file name and local file name
    def fileName = "${gitlabGroup}/${gitlabPproject}/${APPLICATION_VERSION}/QG${qaResult}_report.xml"
    def xml      = "QG${qaResult}_report.xml"
    writeFile file: xml, text: xmlContent

    // URL encode GitLab project path for API usage
    def projectPath  = "${gitlabGroup}/${gitlabPproject}"
    def encodedPath  = java.net.URLEncoder.encode(projectPath, "UTF-8").replaceAll('%', '%%')

    // Temp files
    def uuid         = UUID.randomUUID().toString()
    def payloadFile  = "payload_${uuid}.json"
    def responseFile = "response_${uuid}.json"

    // Define the payload of the request
    def payload = [
        branch: "main",
        commit_message: "Add report ${fileName}",
        actions: [[
            action:    "create",
            file_path: fileName,
            content:   readFile(xml)
        ]]
    ]

    // Write on file the payload and then issue the request to remote server
    writeFile file: payloadFile, text: groovy.json.JsonOutput.toJson(payload)
    bat """
        @echo off
        setlocal enabledelayedexpansion

        set "PROJECT=${encodedPath}"
        set "URL=${gitlabReportUrl}/api/v4/projects/!PROJECT!/repository/commits"
        set "PAYLOAD=${payloadFile}"
        set "OUT=${responseFile}"

        curl -k                                        ^
            --request POST                             ^
            --header "PRIVATE-TOKEN: %GITLAB_TOKEN%"   ^
            --header "Content-Type: application/json"  ^
            --data @!PAYLOAD!                          ^
             "!URL!" > !OUT!
        type !OUT!
        del "!PAYLOAD!"
        del "!OUT!"
        del "${xml}"
        endlocal
    """
}
