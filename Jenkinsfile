node {
  env.JAVA_HOME="${tool 'jdk-oracle-8'}"
  env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
  try {
    stage('Clone'){
      checkout scm
    }
    
    withMaven(maven: 'M3', jdk: 'jdk-oracle-8', mavenOpts: '-Xmx4G', options: [artifactsPublisher(disabled: true), junitPublisher(disabled: false)] ) {
        stage('Compile & Bootstrap') {
          sh "mvn clean compile"
        }

        stage('Generate Tutor') {
          sh "mvn -Drascal.courses=--buildCourses compile"
        }

        stage('Run Tests') {
          sh "mvn -Drascal.test.memory=4 test"
          sh "curl https://codecov.io/bash | bash -s - -K -X gcov -t e8b4481a-d178-4148-a4ff-502906390512"
        }
        
        stage('Packaging') {
          sh "mvn -DskipTests package"
        }
        
        stage('Deploy') {
          if (env.BRANCH_NAME == "master" || env.BRANCH_NAME == "jenkins-deploy") {
            sh "mvn -DskipTests deploy"
          }
        }
    }

    
    if (currentBuild.previousBuild.result == "FAILURE") { 
      slackSend (color: '#5cb85c', message: "BUILD BACK TO NORMAL: <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
    }

    build job: '../../cwi-swat/docs-site-generator/master', wait: false // trigger automatic deploy of docs site
  } catch (e) {
    slackSend (color: '#d9534f', message: "FAILED: <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
    throw e
  }
}
