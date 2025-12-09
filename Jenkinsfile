pipeline {
    agent any

    environment {
        APP_NAME = 'my-bank-app'
        HELM_RELEASE_NAME = 'my-bank-app'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        KAFKA_NAMESPACE = 'kafka'
        KAFKA_RELEASE_NAME = 'kafka'
    }

    stages {
        stage('Deploy Kafka Platform') {
            steps {
                echo 'Deploying Apache Kafka platform...'
                script {
                    // Add Bitnami Helm repository
                    sh '''
                        helm repo add bitnami https://charts.bitnami.com/bitnami || true
                        helm repo update
                    '''
                    
                    // Create Kafka namespace
                    sh """
                        kubectl create namespace ${KAFKA_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                    """
                    
                    // Deploy Kafka using values.yaml
                    sh """
                        helm upgrade --install ${KAFKA_RELEASE_NAME} bitnami/kafka \
                        --version 30.1.5 \
                        --namespace ${KAFKA_NAMESPACE} \
                        --create-namespace \
                        -f helm/kafka/values.yaml \
                        --wait \
                        --timeout 10m
                    """
                    
                    // Wait for Kafka to be ready
                    sh """
                        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka \
                        --namespace ${KAFKA_NAMESPACE} \
                        --timeout=300s || true
                    """
                }
            }
        }

        stage('Create Kafka Topics') {
            steps {
                echo 'Creating Kafka topics...'
                script {
                    def topics = [
                        'account-created',
                        'account-updated',
                        'deposit-completed',
                        'withdrawal-completed',
                        'transfer-initiated',
                        'transfer-completed',
                        'transfer-failed',
                        'notification-event'
                    ]
                    
                    // Wait a bit to ensure Kafka is fully ready
                    sh 'sleep 10'
                    
                    topics.each { topic ->
                        sh """
                            kubectl exec -n ${KAFKA_NAMESPACE} ${KAFKA_RELEASE_NAME}-controller-0 -- \
                            kafka-topics.sh --create \
                            --if-not-exists \
                            --bootstrap-server localhost:9092 \
                            --topic ${topic} \
                            --partitions 3 \
                            --replication-factor 1 \
                            --config retention.ms=604800000 \
                            --config min.insync.replicas=1 || true
                        """
                    }
                    
                    // Verify topics
                    sh """
                        kubectl exec -n ${KAFKA_NAMESPACE} ${KAFKA_RELEASE_NAME}-controller-0 -- \
                        kafka-topics.sh --list --bootstrap-server localhost:9092
                    """
                }
            }
        }

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
                        --set global.env=test \
                        --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092
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
                        --set global.env=prod \
                        --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092
                    """
                }
            }
        }

        stage('Deploy Kafka to Production') {
            input {
                message "Deploy Kafka to Production (with high availability)?"
                ok "Yes, deploy"
            }
            steps {
                echo 'Deploying Kafka to Production with HA configuration...'
                script {
                    sh """
                        helm upgrade --install ${KAFKA_RELEASE_NAME} bitnami/kafka \
                        --version 30.1.5 \
                        --namespace ${KAFKA_NAMESPACE} \
                        -f helm/kafka/values.yaml \
                        -f helm/kafka/values-prod.yaml \
                        --wait \
                        --timeout 15m
                    """
                    
                    // Wait for production Kafka to be ready
                    sh """
                        kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka \
                        --namespace ${KAFKA_NAMESPACE} \
                        --timeout=600s
                    """
                    
                    // Create production topics with higher replication
                    def topics = [
                        'account-created',
                        'account-updated',
                        'deposit-completed',
                        'withdrawal-completed',
                        'transfer-initiated',
                        'transfer-completed',
                        'transfer-failed',
                        'notification-event'
                    ]
                    
                    sh 'sleep 20'
                    
                    topics.each { topic ->
                        sh """
                            kubectl exec -n ${KAFKA_NAMESPACE} ${KAFKA_RELEASE_NAME}-controller-0 -- \
                            kafka-topics.sh --create \
                            --if-not-exists \
                            --bootstrap-server localhost:9092 \
                            --topic ${topic} \
                            --partitions 6 \
                            --replication-factor 3 \
                            --config retention.ms=604800000 \
                            --config min.insync.replicas=2 || true
                        """
                    }
                    
                    echo 'Production Kafka deployment completed!'
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline completed.'
        }
        success {
            echo 'All deployments successful!'
        }
        failure {
            echo 'Pipeline failed! Check logs for details.'
        }
    }
}

