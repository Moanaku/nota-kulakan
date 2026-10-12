# Image untuk Cloud Run. Belum pernah dibuild di lingkungan penulis (Docker Hub diblokir di sana);
# build pertama dilakukan di laptop atau langsung oleh Cloud Build lewat `gcloud run deploy --source`.
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY src/ ./src/
ENV PYTHONPATH=/app/src

# Jangan jalan sebagai root.
RUN useradd --create-home --uid 10001 nota
USER nota

# Cloud Run mengisi $PORT. Foto tidak disimpan ke disk; semua di memori.
CMD ["sh", "-c", "exec uvicorn nota.api:app --host 0.0.0.0 --port ${PORT:-8080} --workers 1"]
