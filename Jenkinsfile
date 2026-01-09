pipeline {
  agent none

  environment {
    JAR_NAME      = "genesis-0.0.1-SNAPSHOT.jar"
    APP_REMOTE    = "/opt/genesis/app/genesis.jar"
    CFG_DIR       = "/opt/genesis/config"
TEST_URL = "http://prd07:8080"
PROD_URL = "http://prd08:8080"
    TEST_URL      = "http://192.168.1.208:8080"
    PROD_URL      = "http://192.168.1.209:8080"
    GIT_SSH_CRED  = "jenkins_remote"
    SRV_SSH_CRED  = "linux-agentr-ssh"
  }

  stages {

    stage("Checkout") {
      agent { label "LinuxBuild" }
      steps {
        sshagent(credentials: [env.GIT_SSH_CRED]) {
          checkout scm
        }
      }
    }

    stage("Build + Unit Tests") {
      agent { label "LinuxBuild" }
      steps {
        sh(label: 'Build', script: '''#!/usr/bin/env bash
set -euxo pipefail
mvn -v
mvn clean test
''')
      }
    }

    stage("Package") {
      agent { label "LinuxBuild" }
      steps {
        sh(label: 'Package', script: """#!/usr/bin/env bash
set -euxo pipefail
mvn clean package -DskipTests
ls -lh target/${JAR_NAME}
cp target/${JAR_NAME} .
""")
        archiveArtifacts artifacts: "${JAR_NAME}", fingerprint: true
        stash name: "jar", includes: "${JAR_NAME}"
      }
    }

    stage("Deploy to TEST") {
      agent { label "LinuxTest" }
      steps {
        unstash "jar"
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh(label: 'Deploy TEST', script: """#!/usr/bin/env bash
set -euxo pipefail
scp ${JAR_NAME} jenkins_node@${TEST_HOST}:/tmp/genesis.jar

ssh jenkins_node@${TEST_HOST} "bash -lc 'set -euxo pipefail
sudo mv /tmp/genesis.jar ${APP_REMOTE}
sudo chown genesis:genesis ${APP_REMOTE}
sudo chmod 550 ${APP_REMOTE}
sudo systemctl restart genesis
sudo systemctl --no-pager --full status genesis | head -n 25
'"
""")
        }
      }
    }

    stage("Smoke TEST") {
      agent { label "LinuxTest" }
      steps {
        sh(label: 'Smoke TEST', script: """#!/usr/bin/env bash
set -euxo pipefail
curl -fsS ${TEST_URL}/actuator/health | cat

curl -fsS -X POST ${TEST_URL}/api/v1/calculations \\
  -H "Content-Type: application/json" \\
  -d '{
    "functionType":"GAUSSIAN_SIN",
    "a":-2,
    "b":2,
    "samplesN":200000,
    "seed":42
  }' | cat
""")
      }
    }

    stage("Approval to PROD") {
      agent any
      steps {
        input message: "¿Desplegar a PROD (prd08)?", ok: "Deploy"
      }
    }

    stage("Deploy to PROD") {
      agent { label "LinuxProd" }
      steps {
        unstash "jar"
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh(label: 'Deploy PROD', script: """#!/usr/bin/env bash
set -euxo pipefail
scp ${JAR_NAME} jenkins_node@${PROD_HOST}:/tmp/genesis.jar

ssh jenkins_node@${PROD_HOST} "bash -lc 'set -euxo pipefail
sudo mv /tmp/genesis.jar ${APP_REMOTE}
sudo chown genesis:genesis ${APP_REMOTE}
sudo chmod 550 ${APP_REMOTE}
sudo systemctl restart genesis
sudo systemctl --no-pager --full status genesis | head -n 25
'"
""")
        }
      }
    }

    stage("Smoke PROD") {
      agent { label "LinuxProd" }
      steps {
        sh(label: 'Smoke PROD', script: """#!/usr/bin/env bash
set -euxo pipefail
curl -fsS ${PROD_URL}/actuator/health | cat
""")
      }
    }
  }
}
