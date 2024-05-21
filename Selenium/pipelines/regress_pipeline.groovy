pipeline {
    agent any
    stages {
        stage('Grade super0') {
            steps {
                build job: 'WFM_regress0'
            }
        }
        stage('Grades 1-2') {
            steps {
                steps {
                    build job: 'WFM_regress1-2', propagate: false
                }
            }
        }
    }
}