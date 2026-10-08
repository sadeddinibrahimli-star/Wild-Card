# 1-ci merhele: node ile build
FROM node:22-alpine AS build

WORKDIR /build

# package.json once kopyalanir -> node_modules qatda cache olunur
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build


# 2-ci merhele: nginx. Node buferi iceride OLmur (~300MB qazanir)
FROM nginx:1.27-alpine

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]