# stage 1: build with node
FROM node:22-alpine AS build

WORKDIR /build

# package.json is copied first -> node_modules are cached in a layer
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build


# stage 2: nginx. The node base image is not included (~300MB saved)
FROM nginx:1.27-alpine

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]