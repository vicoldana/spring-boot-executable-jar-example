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
        withCredentials([file(credentialsId: 'kubeconfig-rancher', variable: 'KUBECONFIG')]) {
          echo '🚀 Deploying app to Rancher Desktop cluster...'
          sh '''
            # 1️⃣ Găsim doar fișierul principal .jar (nu sources sau javadoc)
            MAIN_JAR=$(ls target/*.jar | grep -v 'sources\\|javadoc' | head -n 1)
            echo "📄 JAR detectat: $MAIN_JAR"

            # 2️⃣ Copiem fișierul în folderul de deploy
            mkdir -p /tmp/deploy
            cp "$MAIN_JAR" /tmp/deploy/app.jar

            # 3️⃣ Descărcăm kubectl local (nu în /usr/local/bin)
            echo "📦 Instalăm kubectl v1.29.0 (local în /tmp)..."
            curl -LO "https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl"
            chmod +x kubectl
            mv kubectl /tmp/kubectl
            export PATH=$PATH:/tmp

            # 4️⃣ Setăm kubeconfig din credential
            export KUBECONFIG=$KUBECONFIG

            # 5️⃣ Deploy în Rancher Desktop
            echo "📤 Aplicăm fișierul deploy.yaml..."
            /tmp/kubectl delete pod my-app --ignore-not-found=true
            /tmp/kubectl apply -f deploy.yaml
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
