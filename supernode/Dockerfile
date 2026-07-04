# IZ Play — Super Node P2P
# Usa o Chromium do sistema (mais leve que baixar o do Puppeteer).
FROM node:20-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
      chromium \
      ca-certificates fonts-liberation \
      libnss3 libatk-bridge2.0-0 libatk1.0-0 libcups2 libdrm2 libgbm1 \
      libgtk-3-0 libasound2 libxkbcommon0 libxcomposite1 libxdamage1 \
      libxrandr2 libxfixes3 libpango-1.0-0 libcairo2 \
 && rm -rf /var/lib/apt/lists/*

# Puppeteer nao baixa o proprio Chromium; usa o do sistema
ENV PUPPETEER_SKIP_DOWNLOAD=1 \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium \
    NODE_ENV=production

WORKDIR /app
COPY package.json ./
RUN npm install --omit=dev
COPY . .

# Roda como usuario nao-root (bom para Chromium)
RUN groupadd -r iz && useradd -r -g iz -G audio,video iz \
 && mkdir -p /home/iz && chown -R iz:iz /home/iz /app
USER iz

CMD ["node", "index.js"]
