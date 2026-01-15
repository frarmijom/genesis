pipeline {
  agent none

  environment {
    JAR_NAME      = "genesis-0.0.1-SNAPSHOT.jar"
    APP_REMOTE    = "/opt/genesis/app/genesis.jar"

    TEST_HOST     = "192.168.1.208"   // prd07
    PROD_HOST     = "192.168.1.209"   // prd08

    GIT_SSH_CRED  = "github_ssh_jenkins"
    SRV_SSH_CRED_PRD07  = "ssh-prd07"
    SRV_SSH_CRED_PRD08  = "ssh-prd08"
  }

  stages {

    stage("Checkout") {
      agent { label "LinuxBuild" } // prd06
      steps {
        sshagent(credentials: [env.GIT_SSH_CRED]) {
          checkout scm
        }
      }
    }

    stage("Build + Unit Tests") {
      agent { label "LinuxBuild" } // prd06
      steps {
        sh(label: 'Build', script: '''#!/usr/bin/env bash
set -euxo pipefail
mvn -v
mvn clean test
''')
      }
    }

    stage("Package") {
      agent { label "LinuxBuild" } // prd06
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
      agent { label "LinuxBuild" } // DEPLOY desde BUILD (prd06)
      steps {
        unstash "jar"
        sshagent(credentials: [env.SRV_SSH_CRED_PRD07]) {
          sh(label: 'Deploy TEST', script: """#!/usr/bin/env bash
set -euxo pipefail
scp -o BatchMode=yes ${JAR_NAME} jenkins_node@${TEST_HOST}:/tmp/genesis.jar

ssh -o BatchMode=yes jenkins_node@${TEST_HOST} "bash -lc 'set -euxo pipefail
sudo -n mv /tmp/genesis.jar ${APP_REMOTE}
sudo -n chown genesis:genesis ${APP_REMOTE}
sudo -n chmod 550 ${APP_REMOTE}
sudo -n systemctl restart genesis
sudo -n systemctl --no-pager --full status genesis | head -n 25
'"
""")
        }
      }
    }

    stage("Smoke TEST (local)") {
      agent { label "LinuxTest" } // prd07
      steps {
        sh(label: 'Smoke TEST', script: """#!/usr/bin/env bash
set -euxo pipefail
curl -fsS http://localhost:8080/actuator/health | cat

curl -fsS -X POST http://localhost:8080/api/v1/calculations \\
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
        input message: "¿Desplegar a PROD (prd08 / 192.168.1.209)?", ok: "Deploy"
      }
    }

    stage("Deploy to PROD") {
      agent { label "LinuxBuild" } // DEPLOY desde BUILD (prd06)
      steps {
        unstash "jar"
        sshagent(credentials: [env.SRV_SSH_CRED_PRD08]) {
          sh(label: 'Deploy PROD', script: """#!/usr/bin/env bash
set -euxo pipefail
scp -o BatchMode=yes ${JAR_NAME} jenkins_node@${PROD_HOST}:/tmp/genesis.jar

ssh -o BatchMode=yes jenkins_node@${PROD_HOST} "bash -lc 'set -euxo pipefail
sudo -n mv /tmp/genesis.jar ${APP_REMOTE}
sudo -n chown genesis:genesis ${APP_REMOTE}
sudo -n chmod 550 ${APP_REMOTE}
sudo -n systemctl restart genesis
sudo -n systemctl --no-pager --full status genesis | head -n 25
'"
""")
        }
      }
    }

    stage("Smoke PROD (local)") {
      agent { label "LinuxProd" } // prd08
      steps {
        sh(label: 'Smoke PROD', script: """#!/usr/bin/env bash
set -euxo pipefail
curl -fsS http://localhost:8080/actuator/health | cat
""")
      }
    }
  }
}
