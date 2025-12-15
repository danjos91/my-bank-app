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
        OBS_NAMESPACE = 'observability'
        OBS_NAMESPACE_PROD = 'observability-prod'
        ZIPKIN_RELEASE_NAME = 'zipkin'
        ZIPKIN_CHART = 'oci://registry-1.docker.io/bitnamicharts/zipkin'
        ZIPKIN_VERSION = '5.0.4'
        PROM_RELEASE_NAME = 'prometheus'
        PROM_CHART = 'oci://registry-1.docker.io/bitnamicharts/prometheus'
        PROM_VERSION = '24.6.0'
        PROM_VALUES = 'helm/observability/values-prometheus.yaml'
        GRAFANA_RELEASE_NAME = 'grafana'
        GRAFANA_CHART = 'oci://registry-1.docker.io/bitnamicharts/grafana'
        GRAFANA_VERSION = '8.5.8'
        GRAFANA_VALUES = 'helm/observability/values-grafana.yaml'
        ELASTIC_RELEASE_NAME = 'elasticsearch'
        ELASTIC_CHART = 'oci://registry-1.docker.io/bitnamicharts/elasticsearch'
        ELASTIC_VERSION = '21.2.8'
        ELASTIC_VALUES = 'helm/observability/values-elasticsearch.yaml'
        LOGSTASH_RELEASE_NAME = 'logstash'
        LOGSTASH_CHART = 'oci://registry-1.docker.io/bitnamicharts/logstash'
        LOGSTASH_VERSION = '8.4.2'
        LOGSTASH_VALUES = 'helm/observability/values-logstash.yaml'
        KIBANA_RELEASE_NAME = 'kibana'
        KIBANA_CHART = 'oci://registry-1.docker.io/bitnamicharts/kibana'
        KIBANA_VERSION = '16.5.5'
        KIBANA_VALUES = 'helm/observability/values-kibana.yaml'
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

        stage('Deploy Observability Stack') {
            steps {
                echo 'Deploying Zipkin, Prometheus, Grafana and ELK...'
                script {
                    sh """
                        kubectl create namespace ${OBS_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        helm upgrade --install ${ZIPKIN_RELEASE_NAME} ${ZIPKIN_CHART} \
                        --version ${ZIPKIN_VERSION} \
                        --namespace ${OBS_NAMESPACE} \
                        --create-namespace \
                        -f helm/observability/values-zipkin.yaml \
                        --wait \
                        --timeout 10m
                        helm upgrade --install ${PROM_RELEASE_NAME} ${PROM_CHART} \
                        --version ${PROM_VERSION} \
                        --namespace ${OBS_NAMESPACE} \
                        --create-namespace \
                        -f ${PROM_VALUES} \
                        --wait \
                        --timeout 10m
                        helm upgrade --install ${ELASTIC_RELEASE_NAME} ${ELASTIC_CHART} \
                        --version ${ELASTIC_VERSION} \
                        --namespace ${OBS_NAMESPACE} \
                        --create-namespace \
                        -f ${ELASTIC_VALUES} \
                        --wait \
                        --timeout 10m
                        helm upgrade --install ${LOGSTASH_RELEASE_NAME} ${LOGSTASH_CHART} \
                        --version ${LOGSTASH_VERSION} \
                        --namespace ${OBS_NAMESPACE} \
                        --create-namespace \
                        -f ${LOGSTASH_VALUES} \
                        --wait \
                        --timeout 10m
                        helm upgrade --install ${KIBANA_RELEASE_NAME} ${KIBANA_CHART} \
                        --version ${KIBANA_VERSION} \
                        --namespace ${OBS_NAMESPACE} \
                        --create-namespace \
                        -f ${KIBANA_VALUES} \
                        --wait \
                        --timeout 10m
                        helm upgrade --install ${GRAFANA_RELEASE_NAME} ${GRAFANA_CHART} \
                        --version ${GRAFANA_VERSION} \
                        --namespace ${OBS_NAMESPACE} \
                        --create-namespace \
                        -f ${GRAFANA_VALUES} \
                        --wait \
                        --timeout 10m
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

        stage('Deploy Observability Stack to Prod') {
            input {
                message "Deploy Observability Stack to Production?"
                ok "Yes, deploy"
            }
            steps {
                echo 'Deploying observability stack to Production...'
                script {
                    sh """
                        kubectl create namespace ${OBS_NAMESPACE_PROD} --dry-run=client -o yaml | kubectl apply -f -
                        helm upgrade --install ${ZIPKIN_RELEASE_NAME} ${ZIPKIN_CHART} \
                        --version ${ZIPKIN_VERSION} \
                        --namespace ${OBS_NAMESPACE_PROD} \
                        --create-namespace \
                        -f helm/observability/values-zipkin.yaml \
                        --wait \
                        --timeout 15m
                        helm upgrade --install ${PROM_RELEASE_NAME} ${PROM_CHART} \
                        --version ${PROM_VERSION} \
                        --namespace ${OBS_NAMESPACE_PROD} \
                        --create-namespace \
                        -f ${PROM_VALUES} \
                        --wait \
                        --timeout 15m
                        helm upgrade --install ${ELASTIC_RELEASE_NAME} ${ELASTIC_CHART} \
                        --version ${ELASTIC_VERSION} \
                        --namespace ${OBS_NAMESPACE_PROD} \
                        --create-namespace \
                        -f ${ELASTIC_VALUES} \
                        --wait \
                        --timeout 15m
                        helm upgrade --install ${LOGSTASH_RELEASE_NAME} ${LOGSTASH_CHART} \
                        --version ${LOGSTASH_VERSION} \
                        --namespace ${OBS_NAMESPACE_PROD} \
                        --create-namespace \
                        -f ${LOGSTASH_VALUES} \
                        --wait \
                        --timeout 15m
                        helm upgrade --install ${KIBANA_RELEASE_NAME} ${KIBANA_CHART} \
                        --version ${KIBANA_VERSION} \
                        --namespace ${OBS_NAMESPACE_PROD} \
                        --create-namespace \
                        -f ${KIBANA_VALUES} \
                        --wait \
                        --timeout 15m
                        helm upgrade --install ${GRAFANA_RELEASE_NAME} ${GRAFANA_CHART} \
                        --version ${GRAFANA_VERSION} \
                        --namespace ${OBS_NAMESPACE_PROD} \
                        --create-namespace \
                        -f ${GRAFANA_VALUES} \
                        --wait \
                        --timeout 15m
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

