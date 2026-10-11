> **Automated version:** `deploy/aws/` has a CloudFormation template, a CodePipeline/CodeBuild CI/CD
> pipeline and a one-command `deploy.sh`. See `deploy/aws/README.md`. The manual steps below explain
> what that automation does.

# Deploying Hollis Park to AWS

```
                       ┌──────────── CloudFront (your domain, HTTPS) ────────────┐
 browser ──HTTPS──▶    │  /*        → S3 bucket: store build (private, OAC)      │
                       │  /admin/*  → same bucket, admin build under admin/      │
                       │  /api/*    → Application Load Balancer → ECS Fargate    │
                       └──────────────────────────────────────┬──────────────────┘
                                                              ▼
                                                   RDS PostgreSQL (private)
```

The browser only ever talks to one domain, so there is no CORS to configure, the
frontends need no API URL at build time, and the admin session cookie stays
first-party.

Pick one AWS region (for India, `ap-south-1` Mumbai) and use it for everything
except the CloudFront certificate, which must be in `us-east-1`.

## 1. Database: RDS PostgreSQL

1. RDS → Create database → PostgreSQL 17, template "Free tier" or "Dev/Test",
   instance `db.t4g.micro`, storage 20 GB.
2. DB name `store`, master user `store`, and let RDS manage the password in
   Secrets Manager.
3. Public access: **No**. Put it in the same VPC you will use for ECS.
4. Security group: allow port 5432 **only** from the ECS service's security group.

Flyway creates the tables on the API's first start
(`backend/src/main/resources/db/migration`), Hibernate then validates them, and
the placeholder catalog, default tax/delivery settings, promo code and first
admin are seeded into empty tables. Admin sign-in sessions are stored in
PostgreSQL too, so several API tasks can run side by side.

## 2. API: container image in ECR

```bash
export AWS_REGION=ap-south-1
export ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export REPO=$ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com/hollis-park-api

aws ecr create-repository --repository-name hollis-park-api --region $AWS_REGION
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com
docker build --platform linux/arm64 -t $REPO:1.0.0 ./backend
docker push $REPO:1.0.0
```

(`linux/arm64` pairs with Graviton Fargate tasks, which are cheaper. Use
`linux/amd64` if you choose x86 tasks.)

## 3. API: ECS Fargate service behind a load balancer

1. ECS → Create cluster (Fargate).
2. Task definition: 0.5 vCPU / 1 GB, CPU architecture ARM64, container port 8080,
   image `$REPO:1.0.0`, environment:

   | Name | Value |
   | --- | --- |
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:postgresql://<rds-endpoint>:5432/store` |
   | `DB_USERNAME` | `store` |
   | `DB_PASSWORD` | from Secrets Manager (the RDS-managed secret, key `password`) |
   | `CORS_ALLOWED_ORIGINS` | your site address, for example `https://shop.example.com` (needed because the load balancer sees a different host than the browser) |
   | `SESSION_COOKIE_SECURE` | `true` (the default in the prod profile) |
   | `RAZORPAY_KEY_ID` | `rzp_live_...` (or `rzp_test_...` while testing) |
   | `RAZORPAY_KEY_SECRET` | from Secrets Manager |
   | `RAZORPAY_WEBHOOK_SECRET` | from Secrets Manager, same value as in the Razorpay webhook |
   | `MEDIA_STORAGE` | `s3` |
   | `MEDIA_BUCKET` | the web bucket from step 4, for example `hollis-park-web-<account>` |
   | `AWS_REGION` | `ap-south-1` |
   | `ADMIN_EMAIL` | your email, for the first admin account |
   | `ADMIN_PASSWORD` | from Secrets Manager, at least 10 characters |

   Give the task role (not the execution role) permission to write images:

   ```json
   { "Effect": "Allow", "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
     "Resource": "arn:aws:s3:::hollis-park-web-<account>/media/*" }
   ```

   Images uploaded in the admin panel are stored under `media/` in the bucket and
   served by CloudFront at `/media/...`. The placeholder photos are copied there
   on first start.

   The first admin is only created when the staff table is empty. After your
   first sign-in, add staff from the admin panel and you can remove these two
   variables.

3. Service: desired count 1 (2 for redundancy), with a new Application Load
   Balancer. Target group health check path: `/actuator/health/readiness`.
4. Check it: `curl http://<alb-dns-name>/api/products` returns the catalog.

## 4. Store and admin: S3 + CloudFront

```bash
export BUCKET=hollis-park-web-$ACCOUNT
aws s3 mb s3://$BUCKET --region $AWS_REGION

# store at the bucket root. Never sync --delete over media/: that is where uploads live.
(cd store && npm ci && npm run build)
aws s3 sync store/dist/ s3://$BUCKET/ --delete --exclude "admin/*" --exclude "media/*" --exclude index.html \
  --cache-control "public, max-age=31536000, immutable"
aws s3 cp store/dist/index.html s3://$BUCKET/index.html --cache-control "no-cache"

# admin panel under admin/
(cd admin && npm ci && npm run build)
aws s3 sync admin/dist/ s3://$BUCKET/admin/ --delete --exclude index.html \
  --cache-control "public, max-age=31536000, immutable"
aws s3 cp admin/dist/index.html s3://$BUCKET/admin/index.html --cache-control "no-cache"
```

Then CloudFront → Create distribution:

1. **Origin 1:** the S3 bucket, with Origin Access Control (keep the bucket private;
   CloudFront offers to update the bucket policy for you).
2. **Origin 2:** the load balancer's DNS name.
3. **Default behavior (`*`)** → S3 origin. Redirect HTTP to HTTPS. Attach the
   function in `cloudfront-spa-rewrite.js` as a *viewer request* CloudFront
   Function, so `/catalog/tide-one` loads the store and `/admin/orders` loads
   the admin panel.
4. **Behavior `/api/*`** → load balancer origin. Allowed methods: GET, HEAD,
   OPTIONS, PUT, POST, PATCH, DELETE. Cache policy `CachingDisabled`, origin
   request policy `AllViewerExceptHostHeader`.
5. Alternate domain name + an ACM certificate (requested in `us-east-1`).
6. Default root object: `index.html`.

After every frontend release, re-run the sync commands above and invalidate the
two `index.html` files:

```bash
aws cloudfront create-invalidation --distribution-id <ID> --paths /index.html /admin/index.html
```

## 5. Before going live

- [ ] Replace the placeholder catalog and photos from the admin panel (Products,
      Collections, Homepage, Media). `backend/src/main/resources/seed/` only
      fills an empty database on first start.
- [ ] Set the real GST rate, delivery fees and promo codes in the admin panel
      (Tax and delivery, Promo codes).
- [ ] Real store address, phone and email (currently placeholders in the
      store pages and footer: store/src/pages/Info.tsx and components/store/footer.tsx).
- [ ] Razorpay: finish KYC, generate **live** keys, and add the webhook
      `https://YOUR-DOMAIN/api/payments/razorpay/webhook` (events `payment.captured`,
      `order.paid`). Place one small real order and refund it from the dashboard
      before announcing the store.
- [ ] Sign in at `/admin`, create staff accounts, then remove `ADMIN_PASSWORD`
      from the task definition.
- [ ] Every schema change goes in a new Flyway file (`V2__...sql`); never edit V1
      once production has run it.
- [ ] Add AWS WAF rate limiting on `/api/*` (orders, contact, newsletter and
      `/api/auth/login` are public POST endpoints; login also locks an email for
      15 minutes after 5 wrong passwords).
- [ ] Optionally restrict `/admin/*` and `/api/admin/*` to your office IPs with a
      WAF rule.
