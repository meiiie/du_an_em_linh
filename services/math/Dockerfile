FROM python:3.12-slim

WORKDIR /srv
COPY services/math /srv
RUN pip install --no-cache-dir . \
  && useradd --system --uid 10001 --home-dir /srv --shell /usr/sbin/nologin app

# F-01: không chạy bằng root
USER app
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
