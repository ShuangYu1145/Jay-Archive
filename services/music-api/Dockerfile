FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production HOST=0.0.0.0 PORT=3000 ENABLE_GENERAL_UNBLOCK=false
COPY package.json package-lock.json ./
RUN npm ci --omit=dev && mkdir -p /app/.auth-state && chown node:node /app/.auth-state
USER node
EXPOSE 3000
