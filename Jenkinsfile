@Library('hibernate-jenkins-pipeline-helpers@1.6') _

pipeline {
    agent {
        label 'Worker'
    }
    tools {
        maven 'Apache Maven 3.8'
        jdk 'OpenJDK 17 Latest'
    }
    stages {
        stage('Build') {
            steps {
                checkout scm
                sh """ \
                    mvn -B clean verify
                """
            }
        }
    }
    post {
        always {
            notifyBuildResult maintainers: 'yoann@hibernate.org'
        }
    }
}
