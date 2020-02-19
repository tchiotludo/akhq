#!/usr/bin/env groovy

pipeline{

    agent any
    options{
        skipDefaultCheckout()
    }

    tools {
        jdk 'jdk11'
    }

    environment {
        JAVA_HOME = "${tool 'jdk11'}"
    }

    stages {

        stage('checkout') {
            steps{
                echo "Using branch ${BRANCH_NAME}"
                checkout scm
            }
        }

        stage('clean') {
            steps{
                sh "chmod +x gradlew"
                sh "./gradlew clean"
            }
        }

        stage('build') {
            steps{
                sh "./gradlew build"
            }
        }

    }

}
