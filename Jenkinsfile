pipeline {
  agent none

  environment {
    JAR_NAME    = "genesis-0.0.1-SNAPSHOT.jar"
    APP_REMOTE  = "/opt/genesis/app/genesis.jar"
    RELEASES_DIR = "/opt/genesis/releases"

    TEST_HOST   = "192.168.1.208"   // prd07
    PROD_HOST   = "192.168.1.209"   // prd08

    GIT_SSH_CRED   = "github_ssh_jenkins"
    SSH_TEST_CRED  = "ssh-prd07"
    SSH_PROD_CRED  = "ssh-prd08"
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
set -eux
mvn -v
mvn clean test
''')
      }
    }

    stage("Package") {
      agent { label "LinuxBuild" } // prd06
      steps {
        sh(label: 'Package', script: """#!/usr/bin/env bash
set -eux
mvn clean package -DskipTests
ls -lh target/${JAR_NAME}
cp target/${JAR_NAME} .
""")
        archiveArtifacts artifacts: "${JAR_NAME}", fingerprint: true
        stash name: "jar", includes: "${JAR_NAME}"
      }
    }

    stage("Deploy to TEST (with releases + retry/backoff)") {
      agent { label "LinuxBuild" } // deploy desde build (prd06)
      steps {
        unstash "jar"
        sshagent(credentials: [env.SSH_TEST_CRED]) {
          sh(label: 'Deploy TEST', script: """#!/usr/bin/env bash
set -eux

BUILD_NUM="${BUILD_NUMBER}"
REMOTE_RELEASE_DIR="${RELEASES_DIR}/${BUILD_NUM}"
REMOTE_RELEASE_JAR="${REMOTE_RELEASE_DIR}/genesis.jar"

# 1) Copiar a /tmp
scp -o BatchMode=yes ${JAR_NAME} jenkins_node@${TEST_HOST}:/tmp/genesis.jar

# 2) Mover a releases/<build-number>/ y desplegar (atomico-ish)
ssh -o BatchMode=yes jenkins_node@${TEST_HOST} "bash -lc 'set -eux
sudo -n mkdir -p ${RELEASES_DIR}
sudo -n mkdir -p ${REMOTE_RELEASE_DIR}

sudo -n mv /tmp/genesis.jar ${REMOTE_RELEASE_JAR}
sudo -n chown -R genesis:genesis ${REMOTE_RELEASE_DIR}
sudo -n chmod 550 ${REMOTE_RELEASE_JAR}

# link/copy al path actual que systemd usa
sudo -n ln -sfn ${REMOTE_RELEASE_JAR} ${APP_REMOTE}
sudo -n chown -h genesis:genesis ${APP_REMOTE}

# 3) Restart con retry/backoff
attempt=1
max=5
sleep_s=2
while true; do
  if sudo -n systemctl restart genesis; then
    break
  fi
  if [ "$attempt" -ge "$max" ]; then
    echo "ERROR: restart failed after $attempt attempts"
    sudo -n systemctl --no-pager --full status genesis || true
    exit 5
  fi
  echo "Restart failed (attempt $attempt/$max). Sleeping ${sleep_s}s..."
  sleep "${sleep_s}"
  attempt=$((attempt+1))
  sleep_s=$((sleep_s*2))
done

sudo -n systemctl --no-pager --full status genesis | head -n 25
'"
""")
        }
      }
    }

    stage("Readiness TEST (local loop)") {
      agent { label "LinuxTest" } // prd07
      steps {
        sh(label: 'Readiness TEST', script: """#!/usr/bin/env bash
set -eux

# Espera hasta 90s a que responda health
for i in $(seq 1 90); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "Genesis TEST is UP"
    curl -fsS http://localhost:8080/actuator/health | cat
    echo
    exit 0
  fi
  sleep 1
done

echo "ERROR: Genesis TEST not ready after 90s"
curl -v http://localhost:8080/actuator/health || true
exit 7
""")
      }
    }

    stage("Smoke TEST (local)") {
      agent { label "LinuxTest" } // prd07
      steps {
        sh(label: 'Smoke TEST', script: """#!/usr/bin/env bash
set -eux

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

    stage("Deploy to PROD (with releases + retry/backoff)") {
      agent { label "LinuxBuild" } // deploy desde build (prd06)
      steps {
        unstash "jar"
        sshagent(credentials: [env.SSH_PROD_CRED]) {
          sh(label: 'Deploy PROD', script: """#!/usr/bin/env bash
set -eux

BUILD_NUM="${BUILD_NUMBER}"
REMOTE_RELEASE_DIR="${RELEASES_DIR}/${BUILD_NUM}"
REMOTE_RELEASE_JAR="${REMOTE_RELEASE_DIR}/genesis.jar"

scp -o BatchMode=yes ${JAR_NAME} jenkins_node@${PROD_HOST}:/tmp/genesis.jar

ssh -o BatchMode=yes jenkins_node@${PROD_HOST} "bash -lc 'set -eux
sudo -n mkdir -p ${RELEASES_DIR}
sudo -n mkdir -p ${REMOTE_RELEASE_DIR}

sudo -n mv /tmp/genesis.jar ${REMOTE_RELEASE_JAR}
sudo -n chown -R genesis:genesis ${REMOTE_RELEASE_DIR}
sudo -n chmod 550 ${REMOTE_RELEASE_JAR}

sudo -n ln -sfn ${REMOTE_RELEASE_JAR} ${APP_REMOTE}
sudo -n chown -h genesis:genesis ${APP_REMOTE}

attempt=1
max=5
sleep_s=2
while true; do
  if sudo -n systemctl restart genesis; then
    break
  fi
  if [ "$attempt" -ge "$max" ]; then
    echo "ERROR: restart failed after $attempt attempts"
    sudo -n systemctl --no-pager --full status genesis || true
    exit 5
  fi
  echo "Restart failed (attempt $attempt/$max). Sleeping ${sleep_s}s..."
  sleep "${sleep_s}"
  attempt=$((attempt+1))
  sleep_s=$((sleep_s*2))
done

sudo -n systemctl --no-pager --full status genesis | head -n 25
'"
""")
        }
      }
    }

    stage("Readiness PROD (local loop)") {
      agent { label "LinuxProd" } // prd08
      steps {
        sh(label: 'Readiness PROD', script: """#!/usr/bin/env bash
set -eux

for i in $(seq 1 90); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "Genesis PROD is UP"
    curl -fsS http://localhost:8080/actuator/health | cat
    echo
    exit 0
  fi
  sleep 1
done

echo "ERROR: Genesis PROD not ready after 90s"
curl -v http://localhost:8080/actuator/health || true
exit 7
""")
      }
    }

    stage("Smoke PROD (local)") {
      agent { label "LinuxProd" } // prd08
      steps {
        sh(label: 'Smoke PROD', script: """#!/usr/bin/env bash
set -eux
curl -fsS http://localhost:8080/actuator/health | cat
""")
      }
    }
  }
}
