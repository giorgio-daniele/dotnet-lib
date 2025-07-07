def call(Map args) {

    // Validate required arguments
    if (!args.repoUrl) {
        error "repoUrl is required."
    }
    if (!args.scanner) {
        error "scanner is required."
    }
    if (!args.msbuild) {
        error "msbuild is required."
    }
    if (!args.nuget) {
        error "nuget is required."
    }

    def repoUrl    = args.repoUrl
    def netVersion = args.netVersion ?: params.NET_VERSION
    def scanner    = args.scanner
    def msbuild    = args.msbuild
    def nuget      = args.nuget

    def repoName    = repoUrl.tokenize('/').last().replace('.git', '')
    def projectKey  = repoName
    def projectName = repoName

    def frameworkVersion = "v4.8.1"
    switch (netVersion) {
        case "net35":  frameworkVersion = "v3.5";   break
        case "net452": frameworkVersion = "v4.5.2"; break
        case "net462": frameworkVersion = "v4.6.2"; break
        case "net472": frameworkVersion = "v4.7.2"; break
        case "net481": frameworkVersion = "v4.8.1"; break
        default:       frameworkVersion = "v4.8.1"
    }
    
    bat """
        @echo off
        setlocal

        set "SCANNER=${scanner}"
        set "MSBUILD=${msbuild}"
        set "NUGET=${nuget}"
        set "PROJECT_KEY=${projectKey}"
        set "PROJECT_NAME=${projectName}"
        set "FRAMEWORK_TARGET_VERSION=${frameworkVersion}"

        %SCANNER% begin /k:%PROJECT_KEY% /n:%PROJECT_NAME%
        %NUGET% restore
        %MSBUILD% /t:Rebuild /p:TargetFrameworkVersion=%FRAMEWORK_TARGET_VERSION%
        %SCANNER% end

        endlocal
    """
}
