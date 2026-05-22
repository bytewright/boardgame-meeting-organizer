# 🎲 BGMO: Boardgame Meeting Organizer

BGMO is a modern, mobile-first web application designed to take the friction out of organizing board game nights.
Whether you're a heavy-euro enthusiast or a casual party-game fan, BGMO helps you manage your collection and coordinate
with your group without the "who's bringing what?" messaging chaos.

# ✨ Features

- 📅 Meetup Coordination: Create events with specific dates, durations, and player slot limits.
    - Players can see post code so they know in which area the meeting is before signing up
    - Organizers see who requested to join, can choose people or just roll the dice and choose randomly
- 📚 Library Management: Keep track of your board game collection, including player counts, BGG (BoardGameGeek) IDs, and
  complexity ratings.
    - Automatic lookup of details based on provided bgg ids
- 🤝 Flexible Signups: Supports registered users and anonymous guests (with GDPR-conscious contact info handling).
- 🕹️ "On the Table": Organizers can offer specific games from their library for each meetup, so attendees know exactly
  what to expect.
- 🌍 Internationalized: Full i18n support for localized gaming communities.
- User notifications: The app is integrated with different notification channels, all optional.
    - Email, telegram and signal integration
    - Announcements into group chats for new meetups 
    - e.g. new join request for an event where the user is the organizer
    - e.g. join request approved - the user got one of the slots of an event

# 🛠️ Tech Stack

See [Architecture.md](./ARCHITECTURE.md)

- Backend: Java 21+, Spring Boot 4.+
- Frontend: Vaadin 25 (Web Components & Java-based UI)
- Persistence: Jakarta Persistence (JPA) / Spring Data / MapStruct
- Architecture: Hexagonal / Onion-architecture / Ports & Adapters

> Disclaimer: Of course AI was used in parts of this. Especially the frontend vaadin code adapter was created with
> help of AI but nothing generated unobserved or was commited unreviewed.

# 🚀 Getting Started

- Clone the repo
- Build with Maven: ./mvnw clean install
- Run the app: ./mvnw spring-boot:run
- Access the UI: Open http://localhost:8080 in your browser

# Deployment

Dockerfile is included, that is the easiest way.
- Required .env env vars:
  - APP_ADMIN_PASSWORD: Plain text for `admin` account, can be changed in app after deployment
  - APP_PASSWORD_PEPPER: Global password hasher pepper must be kept secret or is worthless
  - DB_USER: how to connect to db
  - DB_PASS
> Telegram notification adapter
  - TELEGRAM_BOT_TOKEN
  - TELEGRAM_BOT_USERNAME: used to generate Deeplinks to bot
  - TELEGRAM_BOT_DISPLAYNAME
  - TELEGRAM_BOT_GRP_CHAT_ID: Notifications of type 'GROUP' will be pushed there, optional
> Email notification adapter
  - BGMO_EMAIL_FROM_ADR=bgmo@yourdomain.com
> BoardGameGeek integration adapter
  - BGG_API_TOKEN: Must be requested on BGG site
> Spring JavaMailSender
  - SPRING_MAIL_HOST=smtp.provider.com
  - SPRING_MAIL_PORT=587
  - SPRING_MAIL_USERNAME=your-smtp-login
  - SPRING_MAIL_PASSWORD=your-smtp-secret

- volumes:
    - /app/logs/
    - /app/db/
