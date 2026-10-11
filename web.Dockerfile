# Store and admin panel built into one nginx image.
# Used by docker-compose for a production-like local run. On AWS the two dist/
# folders go to S3 + CloudFront instead (see deploy/README.md).
# Official images from the ECR Public mirror: Docker Hub rate-limits shared CI addresses.
ARG REGISTRY=public.ecr.aws/docker/library
# The two npm builds produce plain static files, so they run on the build machine's own architecture.
FROM --platform=$BUILDPLATFORM ${REGISTRY}/node:22-alpine AS store
WORKDIR /src
COPY store/package.json store/package-lock.json ./
RUN npm ci
COPY store/ .
RUN npm run build

FROM --platform=$BUILDPLATFORM ${REGISTRY}/node:22-alpine AS admin
WORKDIR /src
COPY admin/package.json admin/package-lock.json ./
RUN npm ci
COPY admin/ .
RUN npm run build

FROM ${REGISTRY}/nginx:1.27-alpine
# docker-compose proxies to the "api" container; on ECS the API is a sidecar on localhost.
ARG NGINX_CONF=deploy/nginx.conf
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf
COPY --from=store /src/dist /usr/share/nginx/html
COPY --from=admin /src/dist /usr/share/nginx/html/admin
EXPOSE 80
