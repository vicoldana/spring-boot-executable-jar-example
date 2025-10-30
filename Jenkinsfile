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
          // folosește wrapper-ul proiectului dacă există, altfel mvn
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
  }

  post {
    success { echo '✅ Build OK. Găsești JAR-ul în Artifacts.' }
    failure { echo '❌ Build FAILED. Verifică logurile etapelor.' }
  }
}
