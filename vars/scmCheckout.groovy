def call()
{
    checkout([
    $class: 'GitSCM',
    branches: [[name: env.REPO_BRANCH]],
    doGenerateSubmoduleConfigurations: false,
    userRemoteConfigs: [[url: env.REPO_URL]],
    extensions: [[
        $class:             'SubmoduleOption',
        disableSubmodules:   false,
        parentCredentials:   true,
        recursiveSubmodules: true,
        reference:           '',
        trackingSubmodules:  true
        ]]
    ])
}