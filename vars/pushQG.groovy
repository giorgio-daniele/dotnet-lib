def call(Map config) {

    def QA_RESULT             = config.QA_RESULT
    def APPLICATION_VERSION   = config.APPLICATION_VERSION
    def gitlabReportUrl       = config.gitlabReportUrl   // e.g. https://gitlab.com (no trailing slash)
    def gitlabReportGroup     = config.gitlabReportGroup // e.g. "giorgiodaniele-main"
    def gitlabReportProject   = config.gitlabReportProject // e.g. "reports"

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

    // Compose filename with timestamp and sanitized QA result
    def timestamp     = new Date().format("yyyyMMdd_HHmmss")
    def safeQaResult  = qaResult.replaceAll(/[^a-zA-Z0-9_\-]/, "_")
    def nameFile      = "${timestamp}/${gitlabReportGroup}/${gitlabReportProject}/${APPLICATION_VERSION}/QG${safeQaResult}_report.xml"
    def dirPath       = nameFile.substring(0, nameFile.lastIndexOf("/"))

    // Create directory path on Windows (quietly)
    bat "mkdir \"${dirPath}\" >nul 2>&1"

    // Write the XML content to the file
    writeFile file: nameFile, text: xmlContent

    // Prepare project path and URL encode it for GitLab API usage
    def projectPath = "${gitlabReportGroup}/${gitlabReportProject}"
    def encodedPath = java.net.URLEncoder.encode(projectPath, "UTF-8").replaceAll('%', '%%')

    echo "Publishing report to GitLab: ${nameFile}"
    echo "Encoded project path: ${encodedPath}"

    // Generate unique filenames for temporary payload and response files
    def uuid          = UUID.randomUUID().toString()
    def payloadFile   = "payload_${uuid}.json"
    def responseFile  = "response_${uuid}.json"

    // Create JSON payload for GitLab commit API
    def payload = [
        branch: "main",
        commit_message: "Add file ${nameFile}",
        actions: [[
            action:    "create",
            file_path: nameFile,
            content:   readFile(nameFile)
        ]]
    ]

    // Write the JSON payload to a file
    writeFile file: payloadFile, text: groovy.json.JsonOutput.toJson(payload)

    // Use Windows batch with delayed variable expansion to safely handle % signs
    bat """
        @echo off
        setlocal enabledelayedexpansion

        set "PROJECT=${encodedPath}"
        set "URL=${gitlabReportUrl}/api/v4/projects/!PROJECT!/repository/commits"
        set "PAYLOAD=${payloadFile}"
        set "OUT=${responseFile}"

        curl -k ^
            --request POST ^
            --header "PRIVATE-TOKEN: %GITLAB_TOKEN%"  ^
            --header "Content-Type: application/json" ^
            --data @!PAYLOAD! ^
            "!URL!" > !OUT!

        type !OUT!

        endlocal
    """
}
