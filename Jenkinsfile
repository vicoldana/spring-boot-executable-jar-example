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

    stag
