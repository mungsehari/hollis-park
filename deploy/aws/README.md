# Deploying Hollis Park to AWS (ECS Fargate + CodePipeline)

`deploy.sh` builds everything below with one command. Region `ap-south-1` (Mumbai).

```
browser ──HTTP──▶ ALB ──▶ ECS Fargate task ┬─ nginx   : store (/) and admin (/admin)
                                           └─ Spring Boot API (/api, /media) ──▶ RDS PostgreSQL
                                                                              └▶ S3 (uploaded images)

source.zip ▶ S3 ▶ CodePipeline ▶ CodeBuild: test ▶ build 2 images ▶ ECR ▶ roll the ECS service
   (or, without CodeBuild: your Mac builds the 2 images ▶ ECR ▶ roll the ECS service; see "Local build")
```

Small / demo size: one 1 vCPU / 2 GB ARM64 (Graviton) task (nginx + API), `db.t4g.micro` single-AZ PostgreSQL, no NAT gateway
(the task sits in a public subnet, reachable only from the load balancer). About 45 to 65 US dollars a month,
mostly the load balancer, the task and the database.

## This setup has no CloudFront and no HTTPS

It was chosen because a new AWS account must be verified before it may create CloudFront distributions. It is
fine for a demo and testing, with these limits:

- **Plain HTTP.** Staff pages (`/admin`, `/api/admin/*`, `/api/auth/*`) are only reachable from the IP address you
  deploy from (`AdminCidr`); everyone else gets a 403. If your IP changes, run `deploy.sh` again, or set
  `ADMIN_CIDR=x.x.x.x/32`.
- **Do not take real customer payments or personal data over it.** Razorpay test mode is fine. For real
  customers add a domain and a certificate first (below).
- No CDN: images and the site are served by the task itself.

## Deploy

1. Sign the CLI in to your account once (this is the step that must be you):
   ```bash
   aws login            # opens the browser; or: aws configure sso
   aws sts get-caller-identity   # shows your account id when it worked
   ```
2. Run:
   ```bash
   cd ~/Desktop/hollis-park/app
   deploy/aws/deploy.sh your-email@example.com
   ```
   It creates the infrastructure (about 15 minutes, RDS is the slow part), copies your Razorpay test keys from
   `backend/config/application-local.yml` into Secrets Manager without printing them, uploads the source, and
   waits for the pipeline.
3. It prints the store address, the admin address and the command that shows the generated admin password.

## Local build (no CodeBuild needed)

New AWS accounts often have CodeBuild concurrency set to 0, so the pipeline cannot run until AWS Support raises
it. `LOCAL_BUILD=1` skips CodeBuild and builds the two images on your own machine instead. The images are
`linux/arm64` (the task runs on Graviton), which is native on Apple Silicon.

One-time setup on the Mac:

```bash
brew install colima docker docker-buildx
# let Docker find the buildx plugin; if ~/.docker/config.json has "credsStore": "desktop", remove that line
#   "cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"]
colima start --cpu 4 --memory 4 --disk 20 --vm-type vz
docker context use colima
```

Then, every time you want to release:

```bash
cd ~/Desktop/hollis-park/app
LOCAL_BUILD=1 deploy/aws/deploy.sh your-email@example.com   # email only matters on the first run
```

It creates or updates the infrastructure, copies the Razorpay keys, builds both images (the first build downloads
Maven and npm packages, a few minutes), pushes them to ECR as `local-<timestamp>` and `latest`, sets the service to
one task, forces a new deployment and waits until it is healthy. The tests are not run in this mode, so run
`cd backend && mvn test` yourself first. Stop the VM when done with `colima stop`.

Once CodeBuild is allowed (AWS Support case: Service Quotas, CodeBuild "concurrent builds"), run `deploy.sh`
without `LOCAL_BUILD` and the pipeline takes over; the template is already ARM64 for both.

## GitHub Actions (deploy on every push to main)

`.github/workflows/deploy.yml` runs the backend tests, builds both `linux/arm64` images, pushes them to ECR and
rolls the ECS service, with no AWS keys stored in GitHub (OIDC). One-time setup, as the AWS user that owns the stack:

```bash
# 1. add the GitHub role to the stack (keeps every other parameter as it is)
aws cloudformation update-stack --region ap-south-1 --stack-name store \
  --template-body file://deploy/aws/template.yaml --capabilities CAPABILITY_IAM \
  --parameters ParameterKey=AdminEmail,UsePreviousValue=true ParameterKey=AdminCidr,UsePreviousValue=true \
               ParameterKey=ProjectName,UsePreviousValue=true ParameterKey=TaskCpu,UsePreviousValue=true \
               ParameterKey=TaskMemory,UsePreviousValue=true ParameterKey=DbInstanceClass,UsePreviousValue=true \
               ParameterKey=DbMultiAz,UsePreviousValue=true ParameterKey=GitHubRepo,ParameterValue=OWNER/REPO
aws cloudformation wait stack-update-complete --region ap-south-1 --stack-name store

# 2. read the role ARN
aws cloudformation describe-stacks --region ap-south-1 --stack-name store \
  --query "Stacks[0].Outputs[?OutputKey=='GitHubDeployRoleArn'].OutputValue" --output text
```

3. In GitHub: repository -> Settings -> Secrets and variables -> Actions -> **Variables** -> New repository
   variable `AWS_ROLE_ARN` = that ARN. Then push to `main`, or run the workflow from the Actions tab.

Only the `main` branch of that repository can assume the role, and it can only push the two images and roll this
one service. The workflow has the cluster, service and repository names of this stack in its `env:` block; change
them if the stack is ever recreated.

## How releases work (CI/CD)

Every new `source.zip` in the source bucket starts the pipeline. Re-run `deploy/aws/deploy.sh` to ship a change.
CodeBuild then (see `buildspec.yml`):

1. runs the 35 backend tests on a real PostgreSQL,
2. builds the API image and the web image (nginx with the store and the admin; their type checks run in the build),
3. pushes both to ECR (tagged with the build number and `latest`),
4. rolls the ECS service. If the new task never becomes healthy the deployment circuit breaker rolls back to the
   previous version by itself.

Any failing step stops the release; the running version keeps serving.

To release from GitHub on every push instead, replace the Source action in `template.yaml` with a CodeConnections
GitHub source; the build and deploy steps stay as they are.

## Day to day

| Task | How |
| --- | --- |
| Addresses and names | `aws cloudformation describe-stacks --stack-name store --query 'Stacks[0].Outputs'` |
| API and nginx logs | CloudWatch Logs, the group in the `LogGroupName` output |
| Admin password | the `AdminPasswordSecretArn` output, then `aws secretsmanager get-secret-value --secret-id <arn> --query SecretString --output text` |
| Change Razorpay keys | edit the `RazorpaySecretArn` secret (JSON keys `key_id`, `key_secret`, `webhook_secret`), then `aws ecs update-service --cluster <ClusterName> --service <ServiceName> --force-new-deployment` |
| Roll back | re-run the pipeline with the previous source, or retag an older image (`local-<timestamp>` tags stay in ECR) as `latest` and force a new deployment |
| Stop paying for the task | `aws ecs update-service --cluster <ClusterName> --service <ServiceName> --desired-count 0` (the database and load balancer still cost) |
| Remove everything | empty the media, source and artifact buckets, then `aws cloudformation delete-stack --stack-name store`. The database leaves a final snapshot. |

## Going live later

- **HTTPS and a domain:** request an ACM certificate (in `ap-south-1`) for your domain, add an HTTPS listener
  (443) to the load balancer that forwards to the same target group, redirect port 80 to it, point a Route 53
  record at the load balancer, set `SESSION_COOKIE_SECURE` to `true` and `CORS_ALLOWED_ORIGINS` to the
  `https://` address. After that `AdminCidr` can be opened up if you want staff to sign in from anywhere.
- **CloudFront:** once AWS has verified the account, a CDN in front of the load balancer is an optional extra.
- **Razorpay:** finish KYC, put the live key id and secret in the Razorpay secret, and add the webhook
  `https://YOUR-DOMAIN/api/payments/razorpay/webhook` (events `payment.captured`, `order.paid`) with the same
  secret as `webhook_secret`. Place one real small order and refund it from the dashboard before announcing.
- Turn on AWS WAF rate limiting for `/api/*`, and create staff accounts from the admin panel.
- Set a monthly budget alert in AWS Budgets.
