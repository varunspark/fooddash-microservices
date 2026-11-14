pipeline {
    agent any

    tools {
        maven 'maven-3.9'
    }

    stages {
        stage('Build and test: restaurant-service') {
            steps {
                dir('restaurant-service') {
                    sh 'mvn -B verify'
                }
            }
            post {
                always {
                    junit testResults: 'restaurant-service/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }
    }
}
