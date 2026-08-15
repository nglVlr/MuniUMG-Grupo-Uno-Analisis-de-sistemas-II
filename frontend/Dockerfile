# ---- Etapa 1: build ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

# ---- Etapa 2: servir con nginx ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist/sistema-quejas-frontend /usr/share/nginx/html
COPY nginx-frontend.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
