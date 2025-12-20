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
        OBSERVABILITY_NAMESPACE = 'observability'
        ZIPKIN_RELEASE_NAME = 'zipkin'
        ZIPKIN_CHART = 'oci://registry-1.docker.io/bitnamicharts/openzipkin'
        ZIPKIN_VERSION = '1.0.0'
        PROMETHEUS_STACK_RELEASE_NAME = 'kube-prometheus-stack'
        PROMETHEUS_STACK_CHART = 'prometheus-community/kube-prometheus-stack'
        PROMETHEUS_STACK_VERSION = '55.0.0'
        ELK_RELEASE_NAME = 'elk'
        ELASTICSEARCH_CHART = 'oci://registry-1.docker.io/bitnamicharts/elasticsearch'
        ELASTICSEARCH_VERSION = '21.0.0'
        KIBANA_CHART = 'oci://registry-1.docker.io/bitnamicharts/kibana'
        KIBANA_VERSION = '12.0.0'
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
                        [name: 'exchange-rates', partitions: 3, rf: 1, minInsync: 1],
                        [name: 'application-logs', partitions: 3, rf: 1, minInsync: 1]
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
                echo 'Deploying observability components (Zipkin, Prometheus, Grafana, ELK)...'
                script {
                    // Create observability namespace
                    sh """
                        kubectl create namespace ${OBSERVABILITY_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                    """
                    
                    // Add Helm repositories
                    sh """
                        helm repo add prometheus-community https://prometheus-community.github.io/helm-charts || true
                        helm repo update
                    """
                    
                    // Deploy Zipkin
                    sh """
                        helm upgrade --install ${ZIPKIN_RELEASE_NAME} ${ZIPKIN_CHART} \
                        --version ${ZIPKIN_VERSION} \
                        --namespace ${OBSERVABILITY_NAMESPACE} \
                        --create-namespace \
                        --set service.type=ClusterIP \
                        --set service.port=9411 \
                        --wait \
                        --timeout 5m || true
                    """
                    
                    // Deploy Prometheus and Grafana Stack
                    sh """
                        helm upgrade --install ${PROMETHEUS_STACK_RELEASE_NAME} ${PROMETHEUS_STACK_CHART} \
                        --version ${PROMETHEUS_STACK_VERSION} \
                        --namespace ${OBSERVABILITY_NAMESPACE} \
                        --create-namespace \
                        --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
                        --set grafana.service.type=ClusterIP \
                        --set grafana.adminPassword=admin \
                        --wait \
                        --timeout 10m || true
                    """
                    
                    // Deploy Elasticsearch
                    sh """
                        helm upgrade --install elasticsearch ${ELASTICSEARCH_CHART} \
                        --version ${ELASTICSEARCH_VERSION} \
                        --namespace ${OBSERVABILITY_NAMESPACE} \
                        --set global.kibanaEnabled=true \
                        --set master.replicas=1 \
                        --set data.replicas=1 \
                        --set coordinating.replicas=1 \
                        --wait \
                        --timeout 10m || true
                    """
                    
                    // Deploy Kibana
                    sh """
                        helm upgrade --install kibana ${KIBANA_CHART} \
                        --version ${KIBANA_VERSION} \
                        --namespace ${OBSERVABILITY_NAMESPACE} \
                        --set elasticsearch.hosts[0]=elasticsearch:9200 \
                        --set service.type=ClusterIP \
                        --wait \
                        --timeout 5m || true
                    """
                    
                    echo 'Observability stack deployment completed!'
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
                        [name: 'exchange-rates', partitions: 6, rf: 3, minInsync: 2],
                        [name: 'application-logs', partitions: 6, rf: 3, minInsync: 2]
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

