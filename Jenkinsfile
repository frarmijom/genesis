pipeline {
  agent none

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    // Build artifact
    JAR_NAME     = "genesis-0.0.1-SNAPSHOT.jar"

    // Remote layout
    APP_SYMLINK  = "/opt/genesis/app/genesis.jar"
    RELEASES_DIR = "/opt/genesis/releases"
    CONFIG_DIR   = "/opt/genesis/config"
    SERVICE_NAME = "genesis"

    // Targets
    TEST_HOST    = "192.168.1.208"   // prd07
    PROD_HOST    = "192.168.1.209"   // prd08
    REMOTE_USER  = "ci_ops"

    // Jenkins credentials IDs
    GIT_SSH_CRED = "github_ssh_jenkins"
    SRV_SSH_CRED = "linux-agentr-ssh"

    // Healthcheck
    HEALTH_PATH  = "/actuator/health"
    TEST_PORT    = "8080"
    PROD_PORT    = "8080"

    // Thresholds/timing
    READINESS_TIMEOUT_SEC = "120"
    READINESS_SLEEP_SEC   = "3"
  }

  stages {

    stage("Checkout") {
      agent { label "linux-build" }  // agent-linux-01 should have this label
      steps {
        sshagent(credentials: [env.GIT_SSH_CRED]) {
          checkout scm
        }
      }
    }

    stage("Build + Unit Tests") {
      agent { label "linux-build" }
      steps {
        sh '''
          set -euxo pipefail
          java -version
          mvn -version
          mvn -U -B clean test package
          test -f "target/${JAR_NAME}"
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
      agent { label "linux-build" }
      steps {
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh '''
            set -euxo pipefail

            HOST="${TEST_HOST}"
            BN="${BUILD_NUMBER}"

            REL_DIR="${RELEASES_DIR}/${BN}"
            REL_JAR="${REL_DIR}/${JAR_NAME}"

            echo "==> Prepare release dir on TEST: ${REL_DIR}"
            ssh "${REMOTE_USER}@${HOST}" "sudo mkdir -p '${REL_DIR}' && sudo chown -R ${REMOTE_USER}:${REMOTE_USER} '${REL_DIR}'"

            echo "==> Upload jar to TEST: ${REL_JAR}"
            scp "target/${JAR_NAME}" "${REMOTE_USER}@${HOST}:${REL_JAR}"

            echo "==> Switch symlink: ${APP_SYMLINK} -> ${REL_JAR}"
            ssh "${REMOTE_USER}@${HOST}" "sudo ln -sfn '${REL_JAR}' '${APP_SYMLINK}' && sudo chown -h root:root '${APP_SYMLINK}'"

            echo "==> Restart service with retry+backoff"
            ssh "${REMOTE_USER}@${HOST}" "bash -s" <<'EOS'
              set -euo pipefail
              svc="${SERVICE_NAME:-genesis}"
              backoff=(1 2 4 8 16)
              for i in "${!backoff[@]}"; do
                if sudo systemctl restart "$svc"; then
                  sudo systemctl is-active --quiet "$svc"
                  echo "restart OK"
                  exit 0
                fi
                s="${backoff[$i]}"
                echo "restart failed; sleeping ${s}s"
                sleep "$s"
              done
              echo "restart failed after retries"
              sudo systemctl status "$svc" --no-pager || true
              exit 1
EOS
          '''
        }
      }
    }

    stage("Readiness + Smoke TEST") {
      agent { label "linux-build" }
      steps {
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh '''
            set -euxo pipefail

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
                exit 1
              fi
              sleep "${READINESS_SLEEP_SEC}"
            done

            echo "==> Smoke test: actuator health"
            curl -fsv "$URL"
          '''
        }
      }
    }

    stage("Deploy to PROD") {
      agent { label "linux-build" }
      when { expression { currentBuild.currentResult == 'SUCCESS' } }
      steps {
        sshagent(credentials: [env.SRV_SSH_CRED]) {
          sh '''
            set -euxo pipefail

            HOST="${PROD_HOST}"
            BN="${BUILD_NUMBER}"

            REL_DIR="${RELEASES_DIR}/${BN}"
            REL_JAR="${REL_DIR}/${JAR_NAME}"

            echo "==> Prepare release dir on PROD: ${REL_DIR}"
            ssh "${REMOTE_USER}@${HOST}" "sudo mkdir -p '${REL_DIR}' && sudo chown -R ${REMOTE_USER}:${REMOTE_USER} '${REL_DIR}'"

            echo "==> Upload jar to PROD: ${REL_JAR}"
            scp "target/${JAR_NAME}" "${REMOTE_USER}@${HOST}:${REL_JAR}"

            echo "==> Switch symlink: ${APP_SYMLINK} -> ${REL_JAR}"
            ssh "${REMOTE_USER}@${HOST}" "sudo ln -sfn '${REL_JAR}' '${APP_SYMLINK}' && sudo chown -h root:root '${APP_SYMLINK}'"

            echo "==> Restart service with retry+backoff"
            ssh "${REMOTE_USER}@${HOST}" "bash -s" <<'EOS'
              set -euo pipefail
              svc="${SERVICE_NAME:-genesis}"
              backoff=(1 2 4 8 16)
              for i in "${!backoff[@]}"; do
                if sudo systemctl restart "$svc"; then
                  sudo systemctl is-active --quiet "$svc"
                  echo "restart OK"
                  exit 0
                fi
                s="${backoff[$i]}"
                echo "restart failed; sleeping ${s}s"
                sleep "$s"
              done
              echo "restart failed after retries"
              sudo systemctl status "$svc" --no-pager || true
              exit 1
EOS
          '''
        }
      }
    }

    stage("Readiness + Smoke PROD") {
      agent { label "linux-build" }
      steps {
        sh '''
          set -euxo pipefail
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
              exit 1
            fi
            sleep "${READINESS_SLEEP_SEC}"
          done

          echo "==> Smoke test: actuator health"
          curl -fsv "$URL"
        '''
      }
    }
  }

  post {
    failure {
      echo "Pipeline failed. Check the stage logs above."
    }
  }
}
