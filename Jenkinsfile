pipeline {
    agent any

    tools {
        maven 'maven-3.9'
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    // Short commit id, used to tag every image
                    env.IMAGE_TAG = env.GIT_COMMIT.take(7)
                }
                echo "Image tag for this build: ${env.IMAGE_TAG}"
            }
        }

        stage('Build and test') {
            parallel {
                stage('restaurant-service') {
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
                stage('order-service') {
                    steps {
                        dir('order-service') {
                            sh 'mvn -B verify'
                        }
                    }
                    post {
                        always {
                            junit testResults: 'order-service/target/surefire-reports/*.xml',
                                  allowEmptyResults: true
                        }
                    }
                }
                stage('api-gateway') {
                    steps {
                        dir('api-gateway') {
                            sh 'mvn -B verify'
                        }
                    }
                    post {
                        always {
                            junit testResults: 'api-gateway/target/surefire-reports/*.xml',
                                  allowEmptyResults: true
                        }
                    }
                }
            }
        }

        stage('Build Docker images') {
            parallel {
                stage('restaurant-service image') {
                    steps {
                        sh "docker build -t fooddash/restaurant-service:${env.IMAGE_TAG} restaurant-service"
                    }
                }
                stage('order-service image') {
                    steps {
                        sh "docker build -t fooddash/order-service:${env.IMAGE_TAG} order-service"
                    }
                }
                stage('api-gateway image') {
                    steps {
                        sh "docker build -t fooddash/api-gateway:${env.IMAGE_TAG} api-gateway"
                    }
                }
                stage('frontend image') {
                    steps {
                        sh "docker build -t fooddash/frontend:${env.IMAGE_TAG} frontend"
                    }
                }
            }
        }
    }
}
