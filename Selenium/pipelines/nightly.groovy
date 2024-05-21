pipeline {
    agent any
    stages {
        stage ('Nightly test run') {
            stages {
                stage('Active tests') {
                    stages {
                        stage('Release') {
                            steps {
                                build job: 'WFM_nightly_R', propagate: false
                            }
                        }
                        stage('Master') {
                            steps {
                                build job: 'WFM_nightly_M', propagate: false
                            }
                        }
                    }
                }
                stage('Tests in development') {
                    steps {
                        build job: 'WFM_nightly_R_in_progress', propagate: false
                        build job: 'WFM_nightly_M_in_progress', propagate: false
                    }

                }
            }
        }
    }
}

