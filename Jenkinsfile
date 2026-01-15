pipeline {
  agent { label "linux-build" }  // agent-linux-01

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    JAR_NAME     = "genesis-0.0.1-SNAPSHOT.jar"

    APP_SYMLINK  = "/opt/genesis/app/genesis.jar"
    RELEASES_DIR = "/opt/genesis/releases"
    CONFIG_DIR   = "/opt/genesis/config"
    SERVICE_NAME = "genesis"

    TEST_HOST    = "192.168.1.208"   // prd07
    PROD_HOST    = "192.168.1.209"   // prd08
    REMOTE_USER  = "ci_ops"

    GIT_SSH_CRED = "github_ssh_jenkins"
    SRV_SSH_CRED = "ci_ops"

    HEALTH_PATH  = "/actuator/health"
    TEST_PORT    = "8080"
    PROD_PORT    = "8080"

    READINESS_TIMEOUT_SEC = "120"
    READINESS_SLEEP_SEC   = "3"

    SSH_OPTS = "-o BatchMode=yes -o ConnectTimeout=5 -o ServerAliveInterval=10 -o ServerAliveCountMax=3"
  }

  stages {

    stage("Checkout") {
      steps {
        sshagent(credentials: [env.GIT_SSH_CRED]) {
          checkout scm
        }
      }
    }

    stage("Build + Unit Tests") {
      steps {
        sh '''
          set -eu
          bash -s <<'BASH'
            set -euo pipefail
            set -x

            java -version
            mvn -version
            mvn -U -B clean test package
            test -f "target/${JAR_NAME}"
BASH
        '''
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
        success {
          archiveArtifacts artifacts: "target/${JAR_NAME}", fingerprint: true
        }
      }
    }

    stage("Deploy to TEST") {
      steps {
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh '''
            set -eu
            bash -s <<'BASH'
              set -euo pipefail
              set -x

              HOST="${TEST_HOST}"
              BN="${BUILD_NUMBER}"

              REL_DIR="${RELEASES_DIR}/${BN}"
              REL_JAR="${REL_DIR}/${JAR_NAME}"

              echo "==> Prepare release dir on TEST: ${REL_DIR}"
              ssh ${SSH_OPTS} "${REMOTE_USER}@${HOST}" "sudo mkdir -p '${REL_DIR}' && sudo chown -R ${REMOTE_USER}:${REMOTE_USER} '${REL_DIR}'"

              echo "==> Upload jar to TEST: ${REL_JAR}"
              scp ${SSH_OPTS} "target/${JAR_NAME}" "${REMOTE_USER}@${HOST}:${REL_JAR}"

              echo "==> Switch symlink: ${APP_SYMLINK} -> ${REL_JAR}"
              ssh ${SSH_OPTS} "${REMOTE_USER}@${HOST}" "sudo ln -sfn '${REL_JAR}' '${APP_SYMLINK}' && sudo chown -h root:root '${APP_SYMLINK}'"

              echo "==> Restart service with retry+backoff"
              ssh ${SSH_OPTS} "${REMOTE_USER}@${HOST}" "bash -s" -- "${SERVICE_NAME}" <<'EOS'
set -euo pipefail
svc="$1"
backoff=(1 2 4 8 16)

for i in "${!backoff[@]}"; do
  if sudo systemctl restart "$svc"; then
    if sudo systemctl is-active --quiet "$svc"; then
      echo "restart OK"
      exit 0
    fi
  fi
  s="${backoff[$i]}"
  echo "restart failed; sleeping ${s}s"
  sleep "$s"
done

echo "restart failed after retries"
sudo systemctl status "$svc" --no-pager || true
exit 1
EOS
BASH
          '''
        }
      }
    }

    stage("Readiness + Smoke TEST") {
      steps {
        sh '''
          set -eu
          bash -s <<'BASH'
            set -euo pipefail
            set -x

            HOST="${TEST_HOST}"
            URL="http://${HOST}:${TEST_PORT}${HEALTH_PATH}"

            echo "==> Readiness loop: ${URL}"
            deadline=$(( $(date +%s) + READINESS_TIMEOUT_SEC ))

            while true; do
              if curl -fsS "$URL" | grep -q '"status":"UP"'; then
                echo "READY: status UP"
                break
              fi

              now=$(date +%s)
              if [ "$now" -ge "$deadline" ]; then
                echo "❌ Readiness timeout after ${READINESS_TIMEOUT_SEC}s"
                echo "==> curl debug:"
                curl -sv "$URL" || true
                exit 1
              fi

              sleep "${READINESS_SLEEP_SEC}"
            done

            echo "==> Smoke test: actuator health"
            curl -fsv "$URL"
BASH
        '''
      }
    }

    stage("Deploy to PROD") {
      when { expression { currentBuild.currentResult == 'SUCCESS' } }
      steps {
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh '''
            set -eu
            bash -s <<'BASH'
              set -euo pipefail
              set -x

              HOST="${PROD_HOST}"
              BN="${BUILD_NUMBER}"

              REL_DIR="${RELEASES_DIR}/${BN}"
              REL_JAR="${REL_DIR}/${JAR_NAME}"

              echo "==> Prepare release dir on PROD: ${REL_DIR}"
              ssh ${SSH_OPTS} "${REMOTE_USER}@${HOST}" "sudo mkdir -p '${REL_DIR}' && sudo chown -R ${REMOTE_USER}:${REMOTE_USER} '${REL_DIR}'"

              echo "==> Upload jar to PROD: ${REL_JAR}"
              scp ${SSH_OPTS} "target/${JAR_NAME}" "${REMOTE_USER}@${HOST}:${REL_JAR}"

              echo "==> Switch symlink: ${APP_SYMLINK} -> ${REL_JAR}"
              ssh ${SSH_OPTS} "${REMOTE_USER}@${HOST}" "sudo ln -sfn '${REL_JAR}' '${APP_SYMLINK}' && sudo chown -h root:root '${APP_SYMLINK}'"

              echo "==> Restart service with retry+backoff"
              ssh ${SSH_OPTS} "${REMOTE_USER}@${HOST}" "bash -s" -- "${SERVICE_NAME}" <<'EOS'
set -euo pipefail
svc="$1"
backoff=(1 2 4 8 16)

for i in "${!backoff[@]}"; do
  if sudo systemctl restart "$svc"; then
    if sudo systemctl is-active --quiet "$svc"; then
      echo "restart OK"
      exit 0
    fi
  fi
  s="${backoff[$i]}"
  echo "restart failed; sleeping ${s}s"
  sleep "$s"
done

echo "restart failed after retries"
sudo systemctl status "$svc" --no-pager || true
exit 1
EOS
BASH
          '''
        }
      }
    }

    stage("Readiness + Smoke PROD") {
      steps {
        sh '''
          set -eu
          bash -s <<'BASH'
            set -euo pipefail
            set -x

            HOST="${PROD_HOST}"
            URL="http://${HOST}:${PROD_PORT}${HEALTH_PATH}"

            echo "==> Readiness loop: ${URL}"
            deadline=$(( $(date +%s) + READINESS_TIMEOUT_SEC ))

            while true; do
              if curl -fsS "$URL" | grep -q '"status":"UP"'; then
                echo "READY: status UP"
                break
              fi

              now=$(date +%s)
              if [ "$now" -ge "$deadline" ]; then
                echo "❌ Readiness timeout after ${READINESS_TIMEOUT_SEC}s"
                echo "==> curl debug:"
                curl -sv "$URL" || true
                exit 1
              fi

              sleep "${READINESS_SLEEP_SEC}"
            done

            echo "==> Smoke test: actuator health"
            curl -fsv "$URL"
BASH
        '''
      }
    }
  }

  post {
    failure {
      echo "Pipeline failed. Gathering remote status/logs (best effort)."
      script {
        try {
          sshagent(credentials: [env.SRV_SSH_CRED]) {
            sh '''
              set +e
              bash -s <<'BASH'
                echo "==> TEST systemd status/logs"
                ssh ${SSH_OPTS} "${REMOTE_USER}@${TEST_HOST}" "sudo systemctl status ${SERVICE_NAME} --no-pager" || true
                ssh ${SSH_OPTS} "${REMOTE_USER}@${TEST_HOST}" "sudo journalctl -u ${SERVICE_NAME} -n 80 --no-pager" || true
BASH
            '''
          }
        } catch (e) { echo "Could not collect TEST logs: ${e}" }

        try {
          sshagent(credentials: [env.SRV_SSH_CRED]) {
            sh '''
              set +e
              bash -s <<'BASH'
                echo "==> PROD systemd status/logs"
                ssh ${SSH_OPTS} "${REMOTE_USER}@${PROD_HOST}" "sudo systemctl status ${SERVICE_NAME} --no-pager" || true
                ssh ${SSH_OPTS} "${REMOTE_USER}@${PROD_HOST}" "sudo journalctl -u ${SERVICE_NAME} -n 80 --no-pager" || true
BASH
            '''
          }
        } catch (e) { echo "Could not collect PROD logs: ${e}" }
      }
    }
  }
}
