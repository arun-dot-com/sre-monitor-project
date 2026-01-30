pipeline {
    agent any
    tools {
        maven 'Maven3' 
    }
    stages {
        stage('Build') {
            steps {
                echo 'Cleaning and Packaging with Maven...'
                bat 'mvn clean package'
            }
        }
        stage('Deploy & Monitor') {
            steps {
                echo 'App is ready for Prometheus scraping.'
                // This starts the app in a new window so Jenkins doesn't hang
                bat 'start java -jar target/sre-monitor-1.0-SNAPSHOT.jar'
            }
        }
    }
}