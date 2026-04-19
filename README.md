# 🎲 BGMO: Boardgame Meeting Organizer

BGMO is a modern, mobile-first web application designed to take the friction out of organizing board game nights.
Whether you're a heavy-euro enthusiast or a casual party-game fan, BGMO helps you manage your collection and coordinate with your group without the "who's bringing what?" messaging chaos.

# ✨ Features

- 📚 Library Management: Keep track of your board game collection, including player counts, BGG (BoardGameGeek) IDs, and complexity ratings.
- 📅 Meetup Coordination: Create events with specific dates, durations, and player slot limits.
- 🤝 Flexible Signups: Supports registered users and anonymous guests (with GDPR-conscious contact info handling).
- 🕹️ "On the Table": Organizers can offer specific games from their library for each meetup, so attendees know exactly what to expect.
- 🌍 Internationalized: Full i18n support for localized gaming communities.

# 🛠️ Tech Stack

See [Architecture.md](./ARCHITECTURE.md)
- Backend: Java 21+, Spring Boot 3.4.+ (Update to spring 4.x is planned)
- Frontend: Vaadin 24 (Web Components & Java-based UI) (Update to Vaadin 25.x is planned)
- Persistence: Jakarta Persistence (JPA) / Spring Data / MapStruct
- Architecture: Hexagonal / Onion-architecture / Ports & Adapters

> Disclaimer: Of course AI was used in parts of this - Mostly the frontend vaadin code.

# 🚀 Getting Started

- Clone the repo
- Build with Maven: ./mvnw clean install
- Run the app: ./mvnw spring-boot:run
- Access the UI: Open http://localhost:8080 in your browser

# Deployment

- should happen with docker, needs some env vars:
  - APP_ADMIN_PASSWORD = plain text for `admin` account
  - TELEGRAM_BOT_TOKEN 
  - TELEGRAM_BOT_USERNAME 
  - TELEGRAM_BOT_GRP_CHAT_ID
- volumes:
  - /app/logs/
  - /app/db/