#!/usr/bin/env bash
# One command to create (or update) the whole AWS setup and push the current code through the pipeline.
#
#   deploy/aws/deploy.sh you@example.com        # AdminEmail for the first admin account
#   ADMIN_CIDR=203.0.113.7/32 deploy/aws/deploy.sh you@example.com   # override the detected office IP
#   LOCAL_BUILD=1 deploy/aws/deploy.sh you@example.com   # build the images here (Colima/Docker), push to ECR,
#                                                        # no CodeBuild needed (arm64 images, Fargate ARM64)
#
# Needs: an authenticated AWS CLI (run `aws login`, or `aws configure sso`, first), zip, python3.
# Safe to re-run: it updates the stack and sends a new release through the pipeline.
set -euo pipefail

ADMIN_EMAIL="${1:-}"
STACK="${STACK:-store}"
REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-ap-south-1}}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
export AWS_DEFAULT_REGION="$REGION"

if [[ -z "$ADMIN_EMAIL" && -z "$(aws cloudformation describe-stacks --stack-name "$STACK" --query 'Stacks[0].StackId' --output text 2>/dev/null || true)" ]]; then
  echo "Usage: $0 admin-email@example.com" >&2; exit 2
fi
if ! ACCOUNT="$(aws sts get-caller-identity --query Account --output text 2>/dev/null)"; then
  cat >&2 <<MSG
The AWS CLI is not signed in. Sign in once, then run this again:

  aws login                       # opens your browser (AWS CLI v2.32+), or
  aws configure sso               # if your account uses IAM Identity Center

MSG
  exit 1
fi
echo "Account $ACCOUNT, region $REGION, stack '$STACK'"

# A stack whose first creation failed cannot be updated, only replaced.
STATUS="$(aws cloudformation describe-stacks --stack-name "$STACK" --query 'Stacks[0].StackStatus' --output text 2>/dev/null || true)"
if [[ "$STATUS" == ROLLBACK_COMPLETE ]]; then
  echo "Removing the earlier failed stack first"
  aws cloudformation delete-stack --stack-name "$STACK"
  aws cloudformation wait stack-delete-complete --stack-name "$STACK"
fi

# Staff pages are limited to this address while the site has no HTTPS.
if [[ -z "${ADMIN_CIDR:-}" ]]; then
  MYIP="$(curl -fsS https://checkip.amazonaws.com 2>/dev/null | tr -d '[:space:]' || true)"
  [[ -n "$MYIP" ]] && ADMIN_CIDR="$MYIP/32"
fi
if [[ -z "${ADMIN_CIDR:-}" ]]; then echo "Could not detect your public IP. Run with ADMIN_CIDR=x.x.x.x/32" >&2; exit 2; fi
echo "Staff pages (/admin) will be open to: $ADMIN_CIDR"

echo "1/5  Creating or updating the infrastructure (first run takes about 15 minutes, RDS is slow)"
PARAMS=(--parameter-overrides "AdminCidr=$ADMIN_CIDR")
[[ -n "$ADMIN_EMAIL" ]] && PARAMS=(--parameter-overrides "AdminCidr=$ADMIN_CIDR" "AdminEmail=$ADMIN_EMAIL")
aws cloudformation deploy --stack-name "$STACK" --template-file "$ROOT/deploy/aws/template.yaml" \
  --capabilities CAPABILITY_IAM --no-fail-on-empty-changeset "${PARAMS[@]}"

out() { aws cloudformation describe-stacks --stack-name "$STACK" --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text; }
SOURCE_BUCKET="$(out SourceBucketName)"; PIPELINE="$(out PipelineName)"; SITE="$(out SiteUrl)"
RZP_SECRET="$(out RazorpaySecretArn)"; ADMIN_SECRET="$(out AdminPasswordSecretArn)"

echo "2/5  Razorpay keys"
LOCAL_CFG="$ROOT/backend/config/application-local.yml"
if [[ -f "$LOCAL_CFG" ]]; then
  TMP="$(mktemp)"; chmod 600 "$TMP"; trap 'rm -f "$TMP"' EXIT
  python3 - "$LOCAL_CFG" > "$TMP" <<'PY'
import json, re, sys
t = open(sys.argv[1]).read()
def g(k):
    m = re.search(r"^\s*" + k + r":\s*(\S*)\s*$", t, re.M)
    return m.group(1).strip("\"'") if m else ""
print(json.dumps({"key_id": g("key-id"), "key_secret": g("key-secret"), "webhook_secret": g("webhook-secret")}))
PY
  if python3 -c "import json,sys; d=json.load(open('$TMP')); sys.exit(0 if d['key_id'] and d['key_secret'] else 1)"; then
    aws secretsmanager put-secret-value --secret-id "$RZP_SECRET" --secret-string "file://$TMP" > /dev/null
    echo "     test keys copied from backend/config/application-local.yml into Secrets Manager (not printed)"
  else
    echo "     no keys in backend/config/application-local.yml; online payment stays off"
  fi
else
  echo "     no backend/config/application-local.yml; online payment stays off"
fi

if [[ "${LOCAL_BUILD:-}" == 1 ]]; then
  repo_uri() {
    local name; name="$(aws cloudformation describe-stack-resource --stack-name "$STACK" --logical-resource-id "$1" --query StackResourceDetail.PhysicalResourceId --output text)"
    echo "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/$name"
  }
  API_URI="$(repo_uri ApiRepo)"; WEB_URI="$(repo_uri WebRepo)"
  CLUSTER="$(out ClusterName)"; SERVICE="$(out ServiceName)"
  TAG="local-$(date +%Y%m%d%H%M%S)"
  echo "3/5  Building the images locally (linux/arm64), tag $TAG"
  aws ecr get-login-password | docker login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com" > /dev/null
  docker build --platform linux/arm64 -t "$API_URI:$TAG" -t "$API_URI:latest" "$ROOT/backend"
  docker build --platform linux/arm64 -f "$ROOT/web.Dockerfile" --build-arg NGINX_CONF=deploy/nginx.aws.conf \
    -t "$WEB_URI:$TAG" -t "$WEB_URI:latest" "$ROOT"
  echo "4/5  Pushing to ECR"
  for u in "$API_URI" "$WEB_URI"; do docker push "$u:$TAG" > /dev/null; docker push "$u:latest" > /dev/null; done
  echo "5/5  Starting the service and waiting until it is healthy"
  aws ecs update-service --cluster "$CLUSTER" --service "$SERVICE" --desired-count 1 --force-new-deployment > /dev/null
  aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE"
  echo "Released."
  cat <<MSG

Store : $SITE          (plain HTTP until a domain and certificate are added)
Admin : $SITE/admin    (only from $ADMIN_CIDR; email: ${ADMIN_EMAIL:-the AdminEmail you set})
Admin password, shown once on your screen only:
  aws secretsmanager get-secret-value --secret-id $ADMIN_SECRET --query SecretString --output text
MSG
  exit 0
fi

echo "3/5  Packaging the source (secrets, node_modules and build output are left out)"
ZIP="$(mktemp -d)/source.zip"
( cd "$ROOT" && zip -qr "$ZIP" . \
    -x "*/node_modules/*" "*/dist/*" "backend/target/*" "backend/.pgdata/*" "backend/config/*" \
       "backend/media-uploads/*" "*/.git/*" ".claude/*" "*/.claude/*" "*.DS_Store" )
echo "     $(du -h "$ZIP" | cut -f1)"

echo "4/5  Uploading to s3://$SOURCE_BUCKET/source.zip (this starts the pipeline)"
aws s3 cp "$ZIP" "s3://$SOURCE_BUCKET/source.zip" --only-show-errors
rm -f "$ZIP"

echo "5/5  Waiting for the pipeline (tests, build, deploy: about 10 minutes)"
sleep 25
for _ in $(seq 1 90); do
  STATE="$(aws codepipeline get-pipeline-state --name "$PIPELINE" --query 'stageStates[?stageName==`BuildTestDeploy`].latestExecution.status' --output text 2>/dev/null || true)"
  case "$STATE" in
    Succeeded) echo; echo "Released."; break ;;
    Failed|Stopped|Cancelled) echo; echo "The pipeline $STATE. Open the log: CodePipeline -> $PIPELINE -> Build -> View logs" >&2; exit 1 ;;
  esac
  printf '.'; sleep 20
done

cat <<MSG

Store : $SITE          (plain HTTP until a domain and certificate are added)
Admin : $SITE/admin    (only from $ADMIN_CIDR; email: ${ADMIN_EMAIL:-the AdminEmail you set})
Admin password, shown once on your screen only:
  aws secretsmanager get-secret-value --secret-id $ADMIN_SECRET --query SecretString --output text
Change it after the first sign-in (your name, bottom left -> Change password).
MSG
