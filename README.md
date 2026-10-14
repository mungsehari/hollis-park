# Hollis Park store and admin

```
app/
  store/      Storefront: React 19 + React Router 7 + Motion, shadcn/ui (Base UI) + ReUI, Tailwind v4
  admin/      Admin panel: React 19 + shadcn/ui + ReUI + Tailwind v4, served at /admin
  backend/    Spring Boot 3.5 (Java 21) REST API, PostgreSQL, Flyway, Spring Security + Session, S3
  deploy/     aws/ = one-command AWS deploy (CloudFormation, ECS Fargate, CodePipeline), plus manual guide
  docker-compose.yml + web.Dockerfile   PostgreSQL + API + nginx, production-like
```

## Run it locally

Four terminals:

```bash
cd backend && scripts/local-db.sh          # the project's own PostgreSQL on localhost:5439
cd backend && mvn spring-boot:run          # API on :8080 (migrates, seeds, serves /media)
cd store && npm install && npm run dev     # store on http://localhost:5175
cd admin && npm install && npm run dev     # admin on http://localhost:5174/admin
```

`scripts/local-db.sh` keeps its data in `backend/.pgdata` and does not touch any
other PostgreSQL on the machine. Stop it with `scripts/local-db.sh stop`. To use
another database set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

Uploaded images are written to `backend/media-uploads/` locally and to S3 on AWS
(`MEDIA_STORAGE=s3`). The local admin sign-in is in
`backend/src/main/resources/application-local.yml`; production takes
`ADMIN_EMAIL` and `ADMIN_PASSWORD` from the environment.

## Tests and builds

```bash
cd backend && mvn test           # pricing, orders, admin, uploads: on a real embedded PostgreSQL
cd store && npm run build        # typecheck + production build
cd admin && npm run build
```

## What the admin panel manages

| Screen | Admin | Staff |
| --- | --- | --- |
| Dashboard: revenue, orders to handle, sales chart, top sellers | yes | yes |
| Orders: search, status, history, internal notes | yes | yes |
| Messages and subscribers (CSV export) | yes | yes |
| Products: details, sizes in stock, photo gallery, archive | edit | view |
| Homepage: hero photo and headline, banner, featured pair | edit | view |
| Collections: photo, text, tips, order, show / hide | edit | view |
| Media: upload, browse and delete images | yes | view |
| Care picks (Amazon / Flipkart affiliate links) | edit | view |
| Tax and delivery | edit | view |
| Promo codes, Staff accounts | yes | no |

Orders move RECEIVED → CONFIRMED → SHIPPED → DELIVERED (or CANCELLED before
shipping) and every change records who made it.

Images are checked by their real file type (JPG, PNG, WebP, AVIF; up to 8 MB),
given a random name, and cannot be deleted while a product, collection or the
homepage still uses them.

## Online payments (Razorpay)

Off until you add keys: checkout then records the order and charges nothing.
With keys, "Place order and pay" opens the Razorpay window and the order becomes
**Paid** only after the server verifies Razorpay's signature.

1. razorpay.com -> sign up -> switch the dashboard to **Test Mode** (no KYC needed).
2. Account & Settings -> API Keys -> **Generate Test Key** (copy the secret; it is shown once).
3. `cp backend/config/application-local.yml.example backend/config/application-local.yml`,
   paste the Key ID and Key Secret, and restart the API.
4. Pay with Razorpay's test card or test UPI (see Razorpay docs, "Test cards" and "Test UPI IDs").

On AWS the same values are the environment variables `RAZORPAY_KEY_ID`,
`RAZORPAY_KEY_SECRET` and `RAZORPAY_WEBHOOK_SECRET` (store the secrets in Secrets
Manager). Keys starting `rzp_test_` show a "Demo mode, no real money" notice in the
store; swap to `rzp_live_` keys when you go live.

Optional webhook (so a payment still counts if the customer closes the tab): in
Razorpay -> Webhooks add `https://YOUR-DOMAIN/api/v1/payments/razorpay/webhook`, choose
the `payment.captured` and `order.paid` events, set a secret, and use the same value
as `RAZORPAY_WEBHOOK_SECRET`. Locally this needs a tunnel (for example ngrok).

Orders placed with payment on wait as **Awaiting payment** (staff can only cancel
them) until paid; they are left out of revenue until then.

## Amazon / Flipkart links and care picks

- **Products:** in Products → a product → *Also sold on*, add the Amazon and/or Flipkart link
  (your affiliate link if you have one). The product page then shows *Also on Amazon / Flipkart*
  under the store's own Add to bag. Leave both empty to hide them.
- **Care picks:** Content → *Care picks*. Socks, cleaners, insoles and the like, each with a photo,
  a marketplace and a link. They appear on the homepage ("Care for your pair") and on `/care`.
  The four seeded picks use plain marketplace search links; replace them with your affiliate links.
- **Click tracking:** every link goes through `/api/v1/go/...`, which counts the click (what was
  clicked and where; no personal data) and then redirects. The dashboard shows clicks per
  marketplace and the most followed items for the last 30 days.
- **Safety:** only `https` links on the marketplace's own domains (amazon.in, amzn.to, flipkart.com,
  fkrt.it, ...) are accepted, so the redirect cannot be abused to send people elsewhere.
- **Rules:** the store shows an affiliate disclosure next to these links and in the footer, and the
  links carry `rel="sponsored"`. Amazon Associates also expects prices on your site to be current, so
  the picks deliberately show no price.

## Security

Staff sign-in sessions live in PostgreSQL; passwords are bcrypt-hashed; CSRF
tokens protect every admin write; an email is locked for 15 minutes after 5
wrong passwords; the API refuses to remove the last admin; orders are priced and
stock-checked on the server.

## API

All routes are under `/api/v1` and answer with one envelope,
`{success, status, message, data[, requestId]}`. Paged lists return
`data = {content, pageNumber, pageSize, totalElements, totalPages, last}`. The API description is at
`/api/v1/docs` (Swagger UI at `/api/v1/docs/ui`, local and dev only). Backend conventions and the
error rules are in `backend/CLAUDE.md` and `backend/docs/error-handling.md`.

| Method | Path | Who |
| --- | --- | --- |
| GET | `/api/v1/store`, `/products`, `/products/{slug}`, `/collections`, `/reviews`, `/care-picks`, `/media/**` | public |
| POST | `/api/v1/orders`, `/orders/{number}/payment`, `/contact`, `/newsletter`, `/promo/validate` | public |
| GET | `/api/v1/go/product/{slug}/{amazon,flipkart}`, `/go/pick/{id}` (counted redirects) | public |
| POST | `/api/v1/payments/razorpay/webhook` | Razorpay (signed) |
| GET / POST | `/api/v1/auth/csrf`, `/login`, `/logout`, `/me`, `/password` | staff |
| GET / PATCH | `/api/v1/admin/dashboard`, `/orders`, `/messages`, `/subscribers` (+ `/subscribers/export`) | staff |
| GET + writes | `/api/v1/admin/products`, `/collections`, `/home`, `/media`, `/care-picks`, `/settings` | read: staff, write: admin |
| all | `/api/v1/admin/promo-codes`, `/api/v1/admin/staff` | admin |

## Where things live

- **Schema**: `backend/src/main/resources/db/migration` (Flyway; add the next
  `V<n>__...sql` for every change, never edit an applied file).
- **Entities**: all extend `BaseEntity` (UUID id, `createdAt`, `updatedAt`, `version` for
  optimistic locking). Fields are private with Lombok getters/setters and `@SuperBuilder`.
  Things people type in URLs keep their own unique column: product / collection `slug`,
  promo `code`, page `pageKey`; customers see order numbers (`HP-24817`), never UUIDs.
- **First-start seed**: `backend/src/main/resources/seed/` (placeholder catalog,
  collections and the photos in `seed/images/`).
- **Brand and market format**: `backend/src/main/resources/application.yml`.
- **Store theme**: `store/src/index.css` (tokens: ink, chalk, sage). Admin theme:
  `admin/src/index.css`, same palette.
- **Animation**: `store/src/components/store/motion.tsx` (Reveal, Stagger,
  SplitWords, CountUp) and the Motion library; everything honours
  `prefers-reduced-motion`.

## Known gaps

- Online payment needs your Razorpay keys (see above). Refunds are done in the Razorpay dashboard.
- An abandoned checkout leaves an "Awaiting payment" order for staff to cancel.
- No emails (order confirmations, password resets); staff reply from their own mailbox.
- Store address, phone, email and reviews are placeholders.
- Both Docker images have been built (arm64, Colima) and are running on AWS. The CodeBuild pipeline in the template has not been run yet because the account's CodeBuild quota is 0; use `LOCAL_BUILD=1` (see `deploy/aws/README.md`).
