#!/usr/bin/env bash
# Lets this computer's current public IP open the admin panel (/admin) again.
# Run it when /admin says "Staff pages are limited to the office network." (your internet IP changed).
#
#   deploy/aws/allow-my-ip.sh
#
# Needs a signed-in AWS CLI (run `aws login` first). Changes only the AdminCidr parameter of the stack.
set -euo pipefail

STACK="${STACK:-store}"
export AWS_DEFAULT_REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-ap-south-1}}"

aws sts get-caller-identity > /dev/null 2>&1 || { echo "The AWS CLI is not signed in. Run: aws login" >&2; exit 1; }

IP="$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')"
CURRENT="$(aws cloudformation describe-stacks --stack-name "$STACK" \
  --query "Stacks[0].Parameters[?ParameterKey=='AdminCidr'].ParameterValue|[0]" --output text)"
if [[ "$CURRENT" == "$IP/32" ]]; then
  echo "Already allowed: $IP. If /admin still shows 403, wait a minute and reload."
  exit 0
fi

# Keep every other parameter exactly as it is.
PARAMS=("ParameterKey=AdminCidr,ParameterValue=$IP/32")
for key in $(aws cloudformation describe-stacks --stack-name "$STACK" --query 'Stacks[0].Parameters[].ParameterKey' --output text); do
  [[ "$key" == AdminCidr ]] || PARAMS+=("ParameterKey=$key,UsePreviousValue=true")
done

echo "Allowing $IP (was $CURRENT)"
aws cloudformation update-stack --stack-name "$STACK" --use-previous-template --capabilities CAPABILITY_IAM \
  --parameters "${PARAMS[@]}" > /dev/null
aws cloudformation wait stack-update-complete --stack-name "$STACK"
echo "Done. /admin opens from $IP in about a minute."
