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

  environment {
    K8S_NAMESPACE = "jenkins"
  }

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Test') {
      steps {
        echo '🏗️ Compilăm și rulăm testele...'
        sh '''
          if [ -x ./mvnw ]; then
            ./mvnw -B -e -Dmaven.javadoc.skip=true -DskipTests=false clean verify
          else
            mvn -B -e -Dmaven.javadoc.skip=true -DskipTests=false clean verify
          fi
        '''
      }
      post {
        always { junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml' }
      }
    }

    stage('Package') {
      steps {
        echo '📦 Creăm fișierul .jar...'
        sh '''
          if [ -x ./mvnw ]; then
            ./mvnw -B -e -Dmaven.javadoc.skip=true -DskipTests=false clean package
          else
            mvn -B -e -Dmaven.javadoc.skip=true -DskipTests=false clean package
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

    stage('Deploy to Kubernetes (in-cluster)') {
      steps {
        echo '🚀 Deploy în cluster folosind in-cluster config (fără kubeconfig extern)...'
        sh '''
          set -e

          # 1️⃣ Alegem fișierul principal .jar
          MAIN_JAR=$(ls target/*.jar | grep -v 'sources\\|javadoc' | head -n 1)
          echo "📄 JAR detectat: $MAIN_JAR"

          # 2️⃣ Instalăm kubectl v1.29.0 local (în /tmp)
          echo "📦 Instalăm kubectl v1.29.0 (local în /tmp)..."
          curl -LO "https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl"
          chmod +x kubectl && mv kubectl /tmp/kubectl

          # 3️⃣ Creăm manifestul YAML (containerul așteaptă JAR-ul)
          cat > deploy.yaml <<'YAML'
apiVersion: v1
kind: Pod
metadata:
  name: my-app
  labels:
    app: my-app
spec:
  containers:
    - name: my-app
      image: eclipse-temurin:17-jdk-alpine
      command: ["sh","-c","while [ ! -f /app/app.jar ]; do echo '⌛ waiting for /app/app.jar'; sleep 2; done; exec java -jar /app/app.jar"]
      volumeMounts:
        - name: app
          mountPath: /app
  volumes:
    - name: app
      emptyDir: {}
YAML

          # 4️⃣ Aplicăm manifestul
          echo "📤 Aplicăm deploy.yaml..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" delete pod my-app --ignore-not-found=true
          /tmp/kubectl -n "${K8S_NAMESPACE}" apply -f deploy.yaml

          # 5️⃣ Așteptăm crearea podului
          echo "⏳ Așteptăm ca podul să fie creat..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" wait --for=condition=PodScheduled pod/my-app --timeout=60s || true
          /tmp/kubectl -n "${K8S_NAMESPACE}" get pod my-app -o wide || true

          # 6️⃣ Copiem fișierul JAR în pod (unde îl așteaptă containerul)
          echo "📥 Copiem JAR în pod..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" cp "$MAIN_JAR" my-app:/app/app.jar

          echo "✅ JAR copiat. Containerul va porni automat aplicația Spring Boot!"
        '''
      }
    }
  }

  post {
    success {
      echo '✅ Build + Deploy reușit! Aplicația rulează în Rancher Desktop.'
      echo 'ℹ️ Jenkins rulează în namespace-ul ${K8S_NAMESPACE}.'
      echo '👉 Pentru a accesa aplicația local, folosește:'
      echo '   kubectl -n ${K8S_NAMESPACE} port-forward pod/my-app 8081:8080'
      echo 'Apoi deschide: http://localhost:8081'
    }
    failure {
      echo '❌ Build sau Deploy eșuat. Verifică logurile Jenkins.'
    }
  }
}
