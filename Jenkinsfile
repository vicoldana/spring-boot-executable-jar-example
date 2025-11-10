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

  // 🔧 Opțiuni pipeline – curățate și corecte
  options {
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {

    stage('Checkout') {
      steps {
        checkout(scm)
      }
    }

    stage('Build & Test') {
      steps {
        container('maven') {
          echo '🏗️ Compilăm și rulăm testele...'
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
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Package') {
      steps {
        container('maven') {
          echo '📦 Creăm fișierul .jar...'
          sh '''
            if [ -x ./mvnw ]; then
              ./mvnw -B -e package
            else
              mvn -B -e package
            fi
          '''
        }
      }
    }

    stage('Archive Artifact') {
      steps {
        echo '💾 Salvăm artefactul pentru descărcare...'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        container('maven') {
          echo '🚀 Deploy în Rancher Desktop...'
          sh '''
            # 1️⃣ Copiem artefactul în folderul de deploy
            mkdir -p /tmp/deploy
            cp target/*.jar /tmp/deploy/app.jar || true

            # 2️⃣ Instalăm kubectl dacă nu e deja
            if ! command -v kubectl &> /dev/null; then
              apt-get update && apt-get install -y curl
              curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
              chmod +x kubectl && mv kubectl /usr/local/bin/
            fi

            # 3️⃣ Ne conectăm la clusterul Rancher Desktop (kubernetes.default.svc funcționează din interiorul clusterului)
            kubectl config set-cluster rancher --server=https://kubernetes.default.svc --insecure-skip-tls-verify=true
            kubectl config set-context rancher --cluster=rancher
            kubectl config use-context rancher

            # 4️⃣ Ștergem vechiul pod și lansăm aplicația din deploy.yaml
            kubectl delete pod my-app --ignore-not-found=true
            kubectl apply -f deploy.yaml
          '''
        }
      }
    }
  }

  post {
    success {
      echo '✅ Build + Deploy OK. Aplicația rulează în Rancher Desktop!'
    }
    failure {
      echo '❌ Build sau Deploy a eșuat. Verifică logurile Jenkins.'
    }
  }
}
