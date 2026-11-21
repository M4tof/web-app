# Web App Deployment Guide

A lightweight, secure web application with plain HTML/CSS frontend, Spring Boot backend, and PostgreSQL database.

Everything is containerized so you **do not need Java, Maven, or PostgreSQL installed** on your machine.

---

## Prerequisites

You only need **Docker** and **Docker Compose** installed:
* **Ubuntu / Debian / Linux Mint:**
  ```bash
  sudo apt update && sudo apt install -y docker.io docker-compose-plugin
  sudo systemctl start docker
  ```
* **Windows / macOS:** Install [Docker Desktop](https://www.docker.com/products/docker-desktop/).

---

## Quick Deployment (3 Steps)

### Step 1: Prepare the Deployment Folder
Create an empty folder on your target machine/VM:
```bash
mkdir my-app && cd my-app
```

Inside this directory, set up the following 4 files:

#### 1. `docker-compose.yaml`

```yaml
services:
  db:
    image: postgres:16-alpine
    container_name: local-postgres
    restart: always
    env_file:
      - db.env
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./tabelki.sql:/docker-entrypoint-initdb.d/01-tabelki.sql:ro

  app:
    image: ghcr.io/m4tof/web-app:latest
    container_name: web-app
    restart: always
    ports:
      - "8443:8443"
    env_file:
      - server.env
    depends_on:
      - db

volumes:
  pgdata:
```

#### 2. `db.env`
> ⚠️ **Important:** Ensure the password set here matches `DB_PASSWORD` in `server.env`.

```env
POSTGRES_DB=appdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_db_password_here
```

#### 3. `server.env`
> 💡 **Tip:** Generate a secure random pepper string using:  
> `openssl rand -base64 32`

```env
SERVER_PORT=8443
DB_URL=jdbc:postgresql://db:5432/appdb?currentSchema=app_auth
DB_USER=postgres
DB_PASSWORD=your_secure_db_password_here

# Cryptographic Pepper (Used in Argon2id hashing)
PEPPER_SECRET=replace_with_a_long_random_secret_string

# TLS / Keystore Settings
SSL_KEYSTORE_PATH=classpath:keystore.p12
SSL_KEYSTORE_PASSWORD=secret123
SSL_KEYSTORE_TYPE=PKCS12
SSL_KEY_ALIAS=webapp
```

### Step 2: Start the Application

In your terminal, inside that folder, run:

```bash
sudo docker compose up -d
```

Docker will download the pre-built image from GitHub Container Registry and start both containers in seconds.

---

### Step 3: Open in Browser

Open your web browser and navigate to:

  **`https://localhost:8443`**
  
> ⚠️ **Browser Warning ("Potential Security Risk Ahead"):**  
> Because the application uses a self-signed TLS certificate for HTTPS encryption, your browser will show a security warning.  
> Simply click **Advanced -> Accept the Risk and Continue**.

---

## Management Commands

* **View live server logs (e.g. simulated activation links):**
  ```bash
  sudo docker logs -f web-app
  ```

* **Check container status:**
  ```bash
  sudo docker compose ps
  ```

* **Stop the application:**
  ```bash
  sudo docker compose down
  ```

* **Update to the latest version:**
  ```bash
  sudo docker compose pull
  sudo docker compose up -d
  ```
```
