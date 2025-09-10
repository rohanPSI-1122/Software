pipeline {
    agent any
    
    tools {
        maven 'mavenx'   // Jenkins Maven tool (must be configured in Jenkins Global Tools)
    }

    environment {
        APP_NAME = 'software-marketplace-backend'
        VERSION = "${env.BUILD_ID}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                git(
                    url: 'https://github.com/rohanPSI-1122/Software.git',
                    branch: 'main',
                    changelog: true
                )
            }
        }
        
        stage('Setup Environment') {
            steps {
                sh '''
                    echo "=== Environment Setup ==="
                    echo "Java version:"
                    java -version
                    echo "Maven version:"
                    mvn -version
                    echo "Working directory:"
                    pwd
                    echo "Git commit: ${GIT_COMMIT}"
                '''
            }
        }
        
        stage('Dependency Check') {
            steps {
                dir('backend') {
                    sh 'mvn dependency:tree -q'
                }
            }
        }
        
        stage('Compile Code') {
            steps {
                dir('backend') {
                    sh 'mvn clean compile -q'
                    echo "✅ Backend code compiled successfully"
                }
            }
        }
        
       stage('Run Unit Tests') {
    steps {
        dir('backend') {
            sh 'mvn test -q || true'   // runs tests, won't fail if none
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/surefire-reports/*.xml', allowEmptyArchive: true
        }
    }
}


        
        stage('Code Coverage') {
            steps {
                dir('backend') {
                    sh 'mvn jacoco:report -q'
                    echo "✅ Code coverage report generated"
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'backend/target/site/jacoco/**/*', allowEmptyArchive: true
                }
            }
        }
        
        stage('Static Code Analysis') {
            steps {
                dir('backend') {
                    sh 'mvn checkstyle:checkstyle -q || true'
                    sh 'mvn pmd:pmd -q || true'
                    echo "✅ Static code analysis completed"
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'backend/target/checkstyle-result.xml', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'backend/target/pmd.xml', allowEmptyArchive: true
                }
            }
        }
        
        stage('Build Package') {
            steps {
                dir('backend') {
                    sh 'mvn package -DskipTests -q'
                    echo "✅ JAR package built successfully"
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                    archiveArtifacts artifacts: 'backend/target/classes/**/*.class', allowEmptyArchive: true
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                dir('backend') {
                    sh 'mvn org.owasp:dependency-check-maven:check -q || true'
                    echo "✅ Dependency security scan completed"
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'backend/target/dependency-check-report.html', allowEmptyArchive: true
                }
            }
        }
    }
    
    post {
        always {
            sh """
            echo "Build: ${env.BUILD_NUMBER}" > build-info.txt
            echo "Status: ${currentBuild.currentResult}" >> build-info.txt
            echo "Duration: ${currentBuild.durationString}" >> build-info.txt
            echo "Commit: ${env.GIT_COMMIT}" >> build-info.txt
            """
            archiveArtifacts artifacts: 'build-info.txt', fingerprint: true
            cleanWs()
        }
        
        success {
            echo "✅ CI Pipeline completed successfully!"
        }
        
        failure {
            echo "❌ CI Pipeline failed!"
        }
        
        unstable {
            echo "⚠️ CI Pipeline completed with warnings"
        }
    }
}
