# No Dues Portal

This is a Spring Boot server-side rendered application for the No Dues process.
It uses SQLite, JPA, and Thymeleaf.

## Requirements

- Java 21 or newer
- Maven (the project also includes `mvnw`)
- Internet access on the first build to download Maven dependencies

## Start the project

1. Open a terminal in this folder.
2. Copy the example environment file:

```bash
cp .env.example .env
```

3. Open `.env` and set a secure Admin email and password.

Use plain values in `.env`. Do not write `export` and do not add quotes:

```properties
NO_DUES_ADMIN_EMAIL=admin@cse.iitm.ac.in
NO_DUES_ADMIN_PASSWORD=your-secure-password
```

The Admin account is created only when the database has no Admin account.
Changing `.env` does not change an existing Admin password.

4. Start the application:

```bash
./mvnw spring-boot:run
```

5. Open http://localhost:8080.
6. Use `/login` for login and `/register` for public registration.
7. Use `/dashboard` after login.

Spring Boot loads `.env` automatically. The `.env` file is ignored by Git.

The SQLite database is created at `data/no-dues.db`. Uploaded files are stored
under `data/uploads`. These folders are ignored by Git.

## Registration and user access

Only students can create an account. Their email must end with
`@smail.iitm.ac.in`. Students can register with a password or use Google login.

Staff and Faculty cannot self-register. An Admin adds them from the Admin
controls page. They can then use Google login, or the Admin can set a password
when creating the account. The Admin role is created only during the first
secure setup and cannot be changed from the user table.

## Email provider setup

The business rules are ready for email delivery. A real provider must be chosen
before production use. Never put an API key in source code or commit it.

### SMTP provider

1. Create an account with an SMTP provider, such as SendGrid, Mailgun, or the
   institute mail service.
2. Create an SMTP API key or app password in that provider's dashboard.
3. Set these values in `.env` as plain values. Do not include `export` or wrapping
quotes, because quotes become part of the value:

```bash
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=apikey-or-smtp-user
MAIL_PASSWORD=your-secret-value
APP_EMAIL_FROM=no-dues@example.edu
```

4. Add the Spring Mail dependency and mail sender adapter when email delivery is
   enabled. Keep the secret in environment variables or a secret manager.
5. Test with a non-production account first.

Use plain values in `.env`; do not add `export` or wrapping quotes. For example,
write `MAIL_PASSWORD=app-password`, not `MAIL_PASSWORD="app-password"`.

The provider's exact screens and API key name can change. Follow its current
official SMTP/API-key guide.

## Google login setup

1. Open https://console.cloud.google.com/.
2. Create or select a Google Cloud project.
3. Open **APIs & Services -> OAuth consent screen**.
4. Choose the correct user type and add the application name.
5. Add the institute users or domain if the app is in testing mode.
6. Open **APIs & Services -> Credentials**.
7. Select **Create credentials -> OAuth client ID**.
8. Choose **Web application**.
9. Add this local redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

10. Copy the Client ID and Client Secret.
11. Set them as environment variables:

```bash
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
APP_GOOGLE_ENABLED=true
```

12. Add the Spring OAuth2 client dependency and Google login adapter when this
    integration is enabled.
13. Add production HTTPS redirect URIs before deployment.

Google login allows existing Staff and Faculty accounts. It creates a new
account only when the Google email ends with `@smail.iitm.ac.in`, and new
accounts are always Students. Google login never creates an Admin account.

## Test

```bash
./mvnw test
```

## Current scope

The database, workflow rules, file storage, notifications, authentication,
request pages, dashboards, admin controls, archiving, and deletion rules are
implemented. CSS and visual UX improvements are intentionally left for the
approval phase.
