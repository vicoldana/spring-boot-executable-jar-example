pipeline {
  agent {
    kubernetes {
      label "maven-agent"
      defaultContainer 'maven'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: maven
      image: maven:3.9.9-eclipse-temurin-17
      command: ['cat']
      tty: true
      volumeMounts:
        - name: maven-cache
          mountPath: /root/.m2
  volumes:
    - name: maven-cache
      emptyDir: {}
"""
    }
  }

  options { timestamps(); ansiColor('xterm'); buildDiscarder(logRotator(numToKeepStr: '20')) }

  stages {
    stage('Checkout') {
      steps { checkout(scm) }
    }

    stage('Build & Test') {
      steps {
        container('maven') {
          sh '''
            if [ -x ./mvnw ]; then
              ./mvnw -B -e -DskipTests=false clean verify
            else
              mvn -B -e -DskipTests=false clean verify
            fi
          '''
        }
      }
      post {
        always { junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml' }
      }
    }

    stage('Package') {
      steps {
        container('maven') {
          sh '''
            if [ -x ./mvnw ]; then ./mvnw -B -e package; else mvn -B -e package; fi
          '''
        }
      }
    }

    stage('Archive JAR') {
      steps {
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    // 🆕 NOUL PAS — DEPLOY
    stage('Deploy to Kubernetes') {
      steps {
        container('maven') {
          sh '''
            echo "🚀 Deploying app into Kubernetes (Rancher Desktop)..."
            mkdir -p /tmp/deploy
            cp target/*.jar /tmp/deploy/app.jar || true

            if ! command -v kubectl &> /dev/null; then
              apt-get update && apt-get install -y curl
              curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
              chmod +x kubectl && mv kubectl /usr/local/bin/
            fi

            kubectl config set-cluster rancher --server=https://kubernetes.default.svc --insecure-skip-tls-verify=true
            kubectl config set-context rancher --cluster=rancher
            kubectl config use-context rancher

            kubectl delete pod my-app --ignore-not-found=true
            kubectl apply -f deploy.yaml
          '''
        }
      }
    }
  }

  post {
    success { echo '✅ Build + Deploy OK. Aplicația e pornită în Rancher Desktop.' }
    failure { echo '❌ Eroare. Verifică logurile Jenkins.' }
  }
}
