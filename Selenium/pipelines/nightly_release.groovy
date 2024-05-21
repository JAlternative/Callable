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
                sh "./gradlew --configure-on-demand -DXmx1g -DXX:MaxMetaspaceSize=512m ${tasks} -PQA=https://${stand_address} -Pbr=grid -PRETRY=$RETRY"
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
        stage('Release tests') {
            parallel {

                stage('SCHED33 (efes)') {
                    options {
                        timeout(time: 1, unit: 'HOURS')
                    }
                    steps {
                        pl_step('efes', 'efes-wfm-qa.goodt.tech', 'specific_efes')
                    }
                    post {
                        always {
                            stash_results("efes")
                        }
                    }
                }

                stage('SCHED8 (Magnit), SCHED13 (magnit), SCHED26 (magnit), SCHED37 (magnit), LIST20 (Magnit), INTEGRATION (Magnit), OTHER4 (Magnit)') {
                    steps {
                        pl_step('magnit', 'magnitqa-wfm.goodt.tech', 'specific_magnit_LIST specific_magnit_SCHED specific_magnit_SCHED37 specific_magnit_INTEGRATION specific_magnit_OTHER4')
                    }
                    post {
                        always {
                            stash_results("magnit")
                        }
                    }
                }

                stage('SCHED2, SCHED9, SCHED10, SCHED11, SCHED12, SCHED21, SCHED22, SCHED24, SCHED36, SCHED32, SCHED41, OTHER13, OTHER20') {
                    options {
                        timeout(time: 5, unit: 'HOURS')
                    }
                    steps {
                        pl_step('pochta1', 'pochta-wfm-release-qa.goodt.tech', 'SCHED2 SCHED10 SCHED11 SCHED12 SCHED21 SCHED22 SCHED24 SCHED36 SCHED41 SCHED32 OTHER13 OTHER20 specific_pochta')
                    }
                    post {
                        always {
                            stash_results("pochta1")
                        }
                    }
                }
                stage('SCHED7-8, SCHED16-17, SCHED20, SCHED23, SCHED33, SCHED35, SCHED37, SCHED39') {
                    options {
                        timeout(time: 5, unit: 'HOURS')
                    }
                    steps {
                        pl_step('zozo1', 'zozo-wfm-qa.goodt.tech',
                                'wfm_schedule_g0 SCHED7 SCHED8 SCHED9 SCHED16 SCHED17 SCHED20 SCHED23 SCHED33 SCHED35 SCHED37 SCHED39')
                    }
                    post {
                        always {
                            stash_results("zozo1")
                        }
                    }
                }
                stage('INTEGRATION, MIX, N2, PS2, AUTH1') {
                    options {
                        timeout(time: 3, unit: 'HOURS')
                    }
                    steps {
                        testing_step('pochta2', 'pochta-wfm-release-qa.goodt.tech', 'wfm_mix_g0 INTEGRATION MIX1 MIX2 MIX3 N2 PS2 AUTH1')
                    }
                    post {
                        always {
                            stash_results("pochta2")
                            clear_test_roles("pochta2", 'pochta-wfm-release-qa.goodt.tech')
                        }
                    }
                }
                stage('BC1, FTE, LIST, REP1') {
                    options {
                        timeout(time: 4, unit: 'HOURS')
                    }
                    steps {
                        testing_step('zozo2', 'zozo-wfm-qa.goodt.tech',
                                'wfm_analytics_g0 BC1 FTE2 FTE3 FTE4 FTE7 LIST1 LIST2 LIST6 LIST7 LIST16 REP1')
                    }
                    post {
                        always {
                            stash_results("zozo2")
                            clear_test_roles("zozo2", 'zozo-wfm-qa.goodt.tech')
                        }
                    }
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
            try_to_unstash('zozo1')
            try_to_unstash('pochta1')
            try_to_unstash('zozo2')
            try_to_unstash('pochta2')
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