def call(Map config) {
        CHECKOUT_WD = pwd()
        def needsClean       = config.needsClean == null ? true : config.needsClean
        def defaultUrl       = config.gitlabUrl + "/" + config.gitlabGroup + "/" + config.gitlabPproject + ".git"
        def checkoutUrl      = config.checkoutUrl == null ? defaultUrl : config.checkoutUrl

        // def FOLDER_FILES = sh(script:"cd $CHECKOUT_WD && tree $CHECKOUT_WD",
        //         returnStdout: true
        // )
        //echo "FILES IN FOLDER: ${FOLDER_FILES}"
        //uso una regexp per pulire tutti i file, anche quelli nascosti (.angular)
        //eccetto specificatamente ./ e ../
        // if(needsClean == true && CHECKOUT_WD.startsWith('/home/tomcat/workspace')){
        //         sh "cd $CHECKOUT_WD && sudo rm -rf -- ..?* .[!.]* *"
        //         echo "$CHECKOUT_WD"
        // }

        checkout([
                $class: 'GitSCM',
                branches: [[name: env.gitlabBranch]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                        [
                                $class: 'SubmoduleOption',
                                disableSubmodules: false,
                                parentCredentials: true,
                                recursiveSubmodules: true,
                                reference: '',
                                trackingSubmodules: true
                        ]
                ],
                submoduleCfg: [],
                userRemoteConfigs: [
                        [
                                url: checkoutUrl
                        ]
                ]
        ])
}