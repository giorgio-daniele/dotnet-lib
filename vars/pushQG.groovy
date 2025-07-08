def call(Map config) {

    def QA_RESULT             = config.QA_RESULT
    def APPLICATION_VERSION   = config.APPLICATION_VERSION
    def gitlabReportUrl       = config.gitlabReportUrl
    def gitlabReportGroup     = config.gitlabReportGroup
    def gitlabReportProject   = config.gitlabReportProject
    
    if (!APPLICATION_VERSION.equals("dev")) {

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

        def timestamp = new Date().format("yyyyMMdd_HHmmss")

        // Define the name of the file
        def nameFile = "${timestamp}" + 
                                  "/" +
                    gitlabReportGroup + 
                                  "/" + 
                  gitlabReportProject + 
                                  "/" + 
                  APPLICATION_VERSION + 
                                "/QG" + 
                        "${qaResult}" + "_report"

        // Write the XML content within the file                
        writeFile file: nameFile, text: xmlContent

        // Define the path on which posting the result
        def projectPath  = gitlabReportGroup + "/" + gitlabReportProject
        def encodedPath  = java.net.URLEncoder.encode(projectPath, "UTF-8").replaceAll("%", "%%")

        // Define the payload of the request
        def payload = [
            branch: "main",
            commit_message: "Add file ${nameFile}",
            actions: [[
                action:    "create",
                file_path: "reports/${nameFile}",
                content:    readFile(nameFile)
            ]]
        ]
        writeFile file: "payload.json", text: groovy.json.JsonOutput.toJson(payload)

        bat """
            @echo off
            setlocal
            set "PROJECT=${encodedPath}"
            set "URL=${gitlabReportUrl}/api/v4/projects/%PROJECT%/repository/commits"

            curl -k                                       ^
                --request POST                            ^
                --header "PRIVATE-TOKEN: %TOKEN%"         ^
                --header "Content-Type: application/json" ^
                --data @payload.json                      ^
                "%URL%" > response.json
            type response.json
            endlocal
        """
    }
}
