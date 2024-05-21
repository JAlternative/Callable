def clear_old_results() {
    // cleanWs()
    sh '''
        mkdir -p build/allure-results/
        find build/allure-results -type f -delete
        '''
}

def clear_test_roles(workspace_name, stand_address) {
    ws("${workspace_name}") {
        dir('Selenium') {
            try {
                sh "./gradlew clear_roles -PQA=https://${stand_address} -Pbr=grid -PRETRY=$RETRY"
            } catch (err) {
                echo err.getMessage()
            }
        }
    }
}

def stash_results(name) {
    ws("${name}") {
        try {
            sh "mv Selenium/build/allure-results/environment.xml Selenium/build/allure-results/environment-${name}.xml"
        } catch (err) {
            echo err.getMessage()
        }
        stash includes: 'Selenium/build/allure-results/*', name: "${name}", allowEmpty: true
    }
}

def prep_ws() {
    git branch: '$BRANCH', credentialsId: 'abc.delivery', url: 'https://abc.delivery@git.goodt.tech/scm/abc/testit.git'
    dir('Selenium') {
        clear_old_results()
        sh '''
        mkdir -p datainput
        > datainput/setUpDataLoginWFM.csv
        echo 'zozo	\'$ZOZO_PASSWORD > datainput/setUpDataLoginWFM.csv
        echo 'pochta	\'$POCHTA_PASSWORD >> datainput/setUpDataLoginWFM.csv
        echo 'magnit	\'$MAGNIT_PASSWORD >> datainput/setUpDataLoginWFM.csv
        echo 'efes	\'$EFES_PASSWORD >> datainput/setUpDataLoginWFM.csv
        '''
        sh '''
        > datainput/setUpDataLoginDataBase.csv
        echo "qa_tests_db_user	fbX2#z7w)Q\\w" > datainput/setUpDataLoginDataBase.csv
        chmod +x ./gradlew
        '''

        // > datainput/integration/integrationLogin.csv
        // echo "superuser    qwe" >> datainput/integration/integrationLogin.csv
    }
}

def testing_step(stage_name, stand_address, tasks) {
    ws("${stage_name}") {
        prep_ws()
        warnError('Step failed') {
            dir('Selenium') {
                sh "./gradlew -DXloggc:/abc/logs/gc.log ${tasks} -PQA=https://${stand_address} -Pbr=grid -PRETRY=$RETRY"
            }
        }
    }
}

def try_to_unstash(stash_name) {
    try {
        unstash "${stash_name}"
    } catch (err) {
        echo err.getMessage()
    }
}

def pl_step(ws, stand, tasks) {
    clear_test_roles(ws, stand)
    testing_step(ws, stand, tasks)
}

pipeline {
    agent {
        label 'mcs_goodt-wfm-front-builder'
    }
    stages {
        stage('Efes') {
            steps {
                pl_step('efes', "$EFES_ADDRESS", 'in_progress_efes')
            }
            post {
                always {
                    stash_results("efes")
                }
            }
        }
        stage('Magnit') {
            steps {
                pl_step('magnit', "$MAGNIT_ADDRESS", 'in_progress_magnit')
            }
            post {
                always {
                    stash_results("magnit")
                }
            }
        }
        stage('Pochta') {
            steps {
                pl_step('pochta', "$POCHTA_ADDRESS", 'in_progress_pochta')
            }
            /*
            steps {
                script {
                    def testResult =  pl_step('pochta', "$POCHTA_ADDRESS", 'login_test')
                    echo testResult
                    if (testResult == 'Failed') {
                        error "login failed"
                    }
                }
                steps {
                    pl_step('pochta', "$POCHTA_ADDRESS", 'in_progress_pochta')
                }
            }
             */
            post {
                always {
                    stash_results("pochta")
                }
            }
        }
        stage('Zozo') {
            when { expression { "$ZOZO_ADDRESS".trim() } }
            steps {
                pl_step('zozo', "$ZOZO_ADDRESS", 'in_progress_zozo')
            }
            post {
                always {
                    stash_results("zozo")
                }
            }
        }
}

post {
    always {
        // cleanWs()
        sh 'mkdir -p $WORKSPACE/allure-results/'
        sh 'find $WORKSPACE/allure-results -type f -delete'
        try_to_unstash('magnit')
        try_to_unstash('efes')
        try_to_unstash('zozo')
        try_to_unstash('pochta')
        dir("Selenium/build/allure-results/") {
            sh '''
                export LC_ALL=ru_RU.utf8
                echo '<?xml version="1.0" encoding="UTF-8" standalone="no"?><environment>' > environment.xml
                echo '<parameter><key>Дата прогона</key><value>' >> environment.xml
                echo "$(TZ=UTC date '+%d %B %Y %T %Z')" >> environment.xml
                echo '</value></parameter>' >> environment.xml
                for i in environment-*.xml; do grep -oP '<parameter><key>.*?[a-z].*?</key><value>.*?</value></parameter>' $i >> environment.xml; done
                echo '</environment>' >> environment.xml
                for i in environment-*.xml; do rm $i; done
                '''
        }
        sh 'cp -r $WORKSPACE/Selenium/build/allure-results/ $WORKSPACE'
        sh 'rm -rf $WORKSPACE/Selenium/*'
        sh 'mkdir -p $WORKSPACE/allure-report/history/'
        sh 'cp -r $WORKSPACE/allure-report/history/ $WORKSPACE/allure-results/'
        allure includeProperties: false, jdk: '', results: [[path: 'allure-results']]
    }
}
}
