pipeline {
    agent any

    environment {
        APP_NAME = 'my-bank-app'
        HELM_RELEASE_NAME = 'my-bank-app'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Build All') {
            steps {
                echo 'Building all services...'
                // En un pipeline real, esto lanzaría jobs paralelos para cada microservicio
                // O construiría todo con `mvn clean package` desde la raíz si es un reactor build
                sh 'mvn clean package -DskipTests=false'
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    def services = ['accounts-service', 'cash-service', 'transfer-service', 'exchange-service', 
                                   'exchange-generator-service', 'blocker-service', 'notifications-service', 
                                   'auth-server', 'front-ui']
                    
                    services.each { service ->
                        echo "Building image for ${service}..."
                        sh "docker build -t ${service}:${IMAGE_TAG} ${service}/"
                    }
                }
            }
        }

        stage('Deploy All to Test') {
            steps {
                echo 'Deploying umbrella chart to Test...'
                script {
                    sh """
                        helm upgrade --install ${HELM_RELEASE_NAME} ./helm/my-bank-app \
                        --namespace test --create-namespace \
                        --set global.env=test
                        # Nota: En un caso real, deberíamos pasar tags específicos para cada imagen
                    """
                }
            }
        }

        stage('Deploy All to Prod') {
            input {
                message "Deploy Umbrella App to Production?"
                ok "Yes, deploy"
            }
            steps {
                echo 'Deploying umbrella chart to Prod...'
                script {
                    sh """
                        helm upgrade --install ${HELM_RELEASE_NAME} ./helm/my-bank-app \
                        --namespace prod --create-namespace \
                        --set global.env=prod
                    """
                }
            }
        }
    }
}

