# Frontend RECOVR: Angular compilado dentro de Docker y servido con serve-prod.mjs (reenvía /api al backend).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --engine-strict=false
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY serve-prod.mjs .
ENV PORT=4200 API_TARGET=http://backend:8095
USER node
EXPOSE 4200
CMD ["node", "serve-prod.mjs"]
