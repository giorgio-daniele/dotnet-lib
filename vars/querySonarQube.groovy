def call(Map args) {

    // Validate required arguments
    if (!args.repoUrl) {
       error "repoUrl is required. Please provide the URL of the repository to scan."
    }

    if (!args.token) {
       error "token is required. Please provide the SonarQube token for authentication."
    }

    def repoUrl        = args.repoUrl
    def soqubeUrl      = args.soqube         ?: env.SOQUBE
    def reportFileName = args.reportFileName ?: env.REPORT
    def sonarToken    = args.token

    // Get repository name from the URL (es. "repo.git" → "repo")
    def repositoryName = repoUrl.tokenize('/').last().replace('.git', '')
    def projectKey     = repositoryName

    // Define the SonarQube API URL to query metrics
    def metrics        = ["coverage", "sqale_debt_ratio", "security_rating"]
    def encodedMetrics = metrics.join(",")

    // Generate the SonarQube API URL
    def sonarApiUrl = "${soqubeUrl}/api/measures/component" +
                      "?component=${projectKey}"            +
                      "&metricKeys=${encodedMetrics}"

    // Run the command to query SonarQube API
    bat """
        @echo off
        setlocal

        set "URI=${sonarApiUrl}"
        set "TKN=${sonarToken}"
        set "OUT=${reportFileName}"

        curl -s -H "Authorization: Bearer %TKN%" -H "Accept: application/json" -L "%URI%" -o "%OUT%"
        endlocal
    """

    def coverageFile = readFile(reportFileName)
    def coverageJson = readJSON text: coverageFile

    Float coverage       = 0
    Float techDeb        = 0
    Float securityRating = 0

    if (coverageJson?.component?.measures) {
        coverageJson.component.measures.each { measure ->
            if (measure.metric == "coverage"         && measure.value) coverage       = measure.value as Float
            if (measure.metric == "sqale_debt_ratio" && measure.value) techDeb        = measure.value as Float
            if (measure.metric == "security_rating"  && measure.value) securityRating = measure.value as Float
        }
    }

    // Return the results as a map
    return [
        coverage:       coverage,
        techDebt:       techDeb,
        securityRating: securityRating,
        reportUrl:      "${soqubeUrl}/dashboard?id=${projectKey}"
    ]
}
