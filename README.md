# 🎲 BGMO: Boardgame Meeting Organizer

BGMO is a modern, mobile-first web application designed to take the friction out of organizing board game nights.
Whether you're a heavy-euro enthusiast or a casual party-game fan, BGMO helps you manage your collection and coordinate
with your group without the "who's bringing what?" messaging chaos.

# ✨ Features

- 📚 Library Management: Keep track of your board game collection, including player counts, BGG (BoardGameGeek) IDs, and
  complexity ratings.
    - Automatic lookup of details based on provided bgg ids
- 📅 Meetup Coordination: Create events with specific dates, durations, and player slot limits.
    - players can see post code so they know in which area the meeting is.
    - Organizers see who requested to join, can choose people or just roll the dice and choose randomly
- 🤝 Flexible Signups: Supports registered users and anonymous guests (with GDPR-conscious contact info handling).
- 🕹️ "On the Table": Organizers can offer specific games from their library for each meetup, so attendees know exactly
  what to expect.
- 🌍 Internationalized: Full i18n support for localized gaming communities.
- User notifications: The app is integrated with chatbots, so once something of interest to a user happens and if they
  have linked their account to the app, the user will receive a private message by the bot
    - e.g. new join request for an event where the user is the organizer
    - e.g. join request approved - the user got one of the slots of an event

# 🛠️ Tech Stack

See [Architecture.md](./ARCHITECTURE.md)

- Backend: Java 21+, Spring Boot 4.+
- Frontend: Vaadin 25 (Web Components & Java-based UI)
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
  - APP_BASE_URL
  - APP_ADMIN_PASSWORD = plain text for `admin` account
  - APP_PASSWORD_PEPPER = global password hasher pepper
  - DB_USER
  - DB_PASS
  - TELEGRAM_BOT_TOKEN
  - TELEGRAM_BOT_USERNAME
  - TELEGRAM_BOT_DISPLAYNAME
  - TELEGRAM_BOT_GRP_CHAT_ID
- volumes:
    - /app/logs/
    - /app/db/

# ToDos - Bugs

- Locale switcher geht nicht/wird nicht angewandt
- translations de/en angleichen
- game urls not shown on meetup detail page view
- game complexity capped at 1 decimal

# ToDos - Features

- Security upgrade
    - pw reset flow
    - rate limiting
- meetup MeetupEditView
    - Reschedule events
- meetup details expanded
  - Add link text for game urls
  - Gamecards could include tags like "worker placement"
  - Event game poll
  - meetup links: use something else than db uuid as identifier
- FavIcon
- baseUrl as mini adapter so it's editable in admin site
- Registration closed date on meetup detail view
- add github link to impressum