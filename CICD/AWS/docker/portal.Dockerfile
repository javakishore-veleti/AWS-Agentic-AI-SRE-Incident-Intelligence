FROM node:20-bookworm AS build
ARG PORTAL
WORKDIR /src
COPY Portals/${PORTAL}/package.json Portals/${PORTAL}/package-lock.json ./
RUN npm ci
COPY Portals/${PORTAL}/ ./
RUN npm run build
FROM nginx:1.27-alpine
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
