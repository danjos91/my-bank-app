pipeline {
    agent any

    environment {
        APP_NAME = 'my-bank-app'
        HELM_RELEASE_NAME = 'my-bank-app'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        KAFKA_NAMESPACE = 'kafka'
        KAFKA_RELEASE_NAME = 'kafka'
        KAFKA_CHART = 'oci://registry-1.docker.io/bitnamicharts/kafka'
        KAFKA_VERSION = '30.1.5'
        KAFKA_VALUES = 'helm/kafka/values-standalone.yaml'
        KAFKA_VALUES_PROD = 'helm/kafka/values-standalone-prod.yaml'
    }

    stages {
        stage('Deploy Kafka Platform') {
            steps {
                echo 'Deploying Apache Kafka platform...'
                script {
                    // Create Kafka namespace
                    sh """
                        kubectl create namespace ${KAFKA_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                    """
                    
                    // Deploy Kafka using values.yaml
                    sh """
                        helm upgrade --install ${KAFKA_RELEASE_NAME} ${KAFKA_CHART} \
                        --version ${KAFKA_VERSION} \
                        --namespace ${KAFKA_NAMESPACE} \
                        --create-namespace \
                        -f ${KAFKA_VALUES} \
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
                        [name: 'account-created', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'account-updated', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'deposit-completed', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'withdrawal-completed', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'transfer-initiated', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'transfer-completed', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'transfer-failed', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'notification-event', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'exchange-rates', partitions: 3, rf: 1, minInsync: 1]
                    ]
                    
                    // Wait a bit to ensure Kafka is fully ready
                    sh 'sleep 10'
                    
                    topics.each { topicCfg ->
                        sh """
                            kubectl exec -n ${KAFKA_NAMESPACE} \$(kubectl get pod -n ${KAFKA_NAMESPACE} -l app.kubernetes.io/name=kafka,app.kubernetes.io/instance=${KAFKA_RELEASE_NAME} -o jsonpath='{.items[0].metadata.name}') -- \\
                            kafka-topics.sh --create \\
                            --if-not-exists \\
                            --bootstrap-server localhost:9092 \\
                            --topic ${topicCfg.name} \\
                            --partitions ${topicCfg.partitions} \\
                            --replication-factor ${topicCfg.rf} \\
                            --config retention.ms=604800000 \\
                            --config min.insync.replicas=${topicCfg.minInsync} || true
                        """
                    }
                    
                    // Verify topics
                    sh """
                        kubectl exec -n ${KAFKA_NAMESPACE} \$(kubectl get pod -n ${KAFKA_NAMESPACE} -l app.kubernetes.io/name=kafka,app.kubernetes.io/instance=${KAFKA_RELEASE_NAME} -o jsonpath='{.items[0].metadata.name}') -- \\
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
                        --set kafka.enabled=false \
                        --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092
                        # Nota: En un caso real, deberíamos pasar tags específicos para cada imagen
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
                        helm upgrade --install ${KAFKA_RELEASE_NAME} ${KAFKA_CHART} \
                        --version ${KAFKA_VERSION} \
                        --namespace ${KAFKA_NAMESPACE} \
                        -f ${KAFKA_VALUES} \
                        -f ${KAFKA_VALUES_PROD} \
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
                        [name: 'account-created', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'account-updated', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'deposit-completed', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'withdrawal-completed', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'transfer-initiated', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'transfer-completed', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'transfer-failed', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'notification-event', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'exchange-rates', partitions: 6, rf: 3, minInsync: 2]
                    ]
                    
                    sh 'sleep 20'
                    
                    topics.each { topicCfg ->
                        sh """
                            kubectl exec -n ${KAFKA_NAMESPACE} \$(kubectl get pod -n ${KAFKA_NAMESPACE} -l app.kubernetes.io/name=kafka,app.kubernetes.io/instance=${KAFKA_RELEASE_NAME} -o jsonpath='{.items[0].metadata.name}') -- \\
                            kafka-topics.sh --create \\
                            --if-not-exists \\
                            --bootstrap-server localhost:9092 \\
                            --topic ${topicCfg.name} \\
                            --partitions ${topicCfg.partitions} \\
                            --replication-factor ${topicCfg.rf} \\
                            --config retention.ms=604800000 \\
                            --config min.insync.replicas=${topicCfg.minInsync} || true
                        """
                    }
                    
                    echo 'Production Kafka deployment completed!'
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
                        --set kafka.enabled=false \
                        --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092
                    """
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

