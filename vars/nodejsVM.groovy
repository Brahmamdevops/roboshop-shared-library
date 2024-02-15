def call(Map configMap) {
pipeline{
     agent {
        node {
            label 'AGENT-1'
            
        }
    }
    
     environment { 
        packageVersion = ""
        //this nexusURL taking from pipelinegolbals
        // nexusUrl ="172.31.5.38:8081"
    }

    options {
        ansiColor('xterm')
        // timeout(time: 1, unit: 'HOURS')
        // disableConcurrentBuilds()
    }

    parameters {
        // string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

        // text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

        booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')

        // choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

        // password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    }
    
    stages {
        stage('get the version') {
            steps {
                script {
                    def packageJson = readJSON file: 'package.json'
                        packageVersion  = packageJson.version
                        echo " apllication $packageVersion " 
                }
        
            }
        }

        stage('Install Dependencies') {
            steps {
                sh """
                    npm install 
                """
            }
        }

        stage('Unit tests') {
            steps {
                sh """
                echo : "unit tests run here"
                """
            }
        }

        stage('Sonar Scan') {
            steps {
                sh """
                    echo : "sonar-scan"
                """
            }
        }


        stage('Build') {
            steps {
                sh """  
                ls -la
                zip -q -r catalogue.zip ./* -x ".git" -x "*.zip"
                ls -ltr 
                """
            }
        }

        stage('Publish  Artifact') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: pipelinegolbals.nexusURL(),
                    groupId: 'com.roboshop',
                    version: "${packageVersion}",
                    repository: "${configMap.component}",
                    credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: "${configMap.component}",
                            classifier: '',
                            file: "${configMap.component}.zip",
                            type: 'zip']
                        ]
                )
            }
        }

         stage ('Deploy Invoke_pipelineA') {
             when {
                expression{
                    params.Deploy
                }
            }

            steps {
                build job: "../${configMap.component}-deploy", wait: true , parameters: [
                string(name: 'version', value: "$packageVersion"),
                string(name: 'environment', value: "dev"),
                ]
            }
        }


        
    }

     // post build
    post { 
        always { 
            echo 'I will always say Hello again!'
            deleteDir()
        }
        failure { 
            echo 'this runs when pipeline is failed, used generally to send some alerts'
        }
        success{
            echo 'I will say Hello when pipeline is success'
        }
    }
}

}