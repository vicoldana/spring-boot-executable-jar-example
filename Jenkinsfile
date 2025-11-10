pipeline {
  agent any

  tools {
    jdk 'jdk17'
    maven 'Maven_3.9.9'
  }

  options {
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build & Test') {
      steps {
        echo '🏗️ Compilăm și rulăm testele...'
        sh '''
          if [ -x ./mvnw ]; then
            ./mvnw -B -e -DskipTests=false clean verify
          else
            mvn -B -e -DskipTests=false clean verify
          fi
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Package') {
      steps {
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

    stage('Archive Artifact') {
      steps {
        echo '💾 Salvăm artefactul pentru descărcare...'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Deploy to Kubernetes (Rancher Desktop)') {
      steps {
        // folosim credentialul creat în Jenkins cu fișierul C:\Users\davicol\.kube\config
        withCredentials([file(credentialsId: 'kubeconfig-rancher', variable: 'KUBECONFIG')]) {
          echo '🚀 Deploying app to Rancher Desktop cluster...'
          sh '''
            # 1️⃣ Copiem fișierul .jar într-un folder accesibil pentru container
            mkdir -p /tmp/deploy
            cp target/*.jar /tmp/deploy/app.jar || true

            # 2️⃣ Instalăm kubectl dacă nu există
            if ! command -v kubectl &> /dev/null; then
              apt-get update && apt-get install -y curl
              curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
              chmod +x kubectl && mv kubectl /usr/local/bin/
            fi

            # 3️⃣ Folosim kubeconfig-ul local (din credential)
            export KUBECONFIG=$KUBECONFIG

            # 4️⃣ Aplicăm fișierul YAML care rulează aplicația
            kubectl delete pod my-app --ignore-not-found=true
            kubectl apply -f deploy.yaml
          '''
        }
      }
    }
  }

  post {
    success {
      echo '✅ Build + Deploy reușit! Aplicația rulează în Rancher Desktop.'
    }
    failure {
      echo '❌ Build sau Deploy eșuat. Verifică logurile Jenkins.'
    }
  }
}
