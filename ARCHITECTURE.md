# Architecture: BGMO

This project follows the Hexagonal Architecture (also known as Ports and Adapters). This pattern decouples the core
business logic from external concerns like databases, UI frameworks, and messaging bots.

# 📐 Overall Structure

The project is divided into three primary layers: Domain, Use Cases, and Adapters.

1. The Domain (The Core)
   Located in org.bytewright.bgmo.domain. This is the "inside" of the hexagon.

    - Model: Pure business objects like Game, MeetupEvent, and RegisteredUser.
    - Service (Ports): Interfaces that define how the core needs to interact with the outside world (e.g., ModelDao for
      data persistence but also AuthenticationService).

2. Use Cases (Application Layer)
   Located in org.bytewright.bgmo.usecases.
   These are the Interactors. They orchestrate the flow of data to and from the domain entities.

    - UserWorkflows: Handles library additions and user profile management.
    - MeetupWorkflows: Manages the lifecycle of an event, from creation to attendee confirmation.

3. Adapters (The "Outside")
   Located in org.bytewright.bgmo.adapter. These handle specific parts, the rest of the application does not need to
   know about. These are only allowed to interact with the domain and app layer, not other adapters.

    - api.frontend: A Vaadin-based implementation of the web interface. It translates user clicks into calls to the Use
      Cases. Also allows the frontend to be defined in java instead of the usual web tech stack.
    - persistence: Implementation of the ModelDao ports, handling the actual SQL/database transactions. This works quite
      generic using a base entity and a base mapper from domain model to entities. The mappers are at the same time the
      DAO impls. THis might seem strange at first but feels quite seamless as its making heavy use of MapStruct.
    - bot (Planned): Future adapters for Telegram, Signal, and Mail. Because of the hexagonal approach, these can be
      added without changing the core meetup logic.

# 🛠️ Key Design Patterns

- DTOs: Use of MeetupCreation to decouple the UI form data from the persisted MeetupEvent.
- Models contain ids for relations: If a model has a foreign key, it only contains that entities id which must be
  fetched using the Daos. This avoids over-fetching and nicely defines transaction borders.
- Generics: ModelDao<MODEL_TYPE> provides a consistent, type-safe interface for all CRUD operations. Specific models can
  add their own DaoInterface which extends the base dao to add custom lookup methods which will later be implemented in
  the persistence adapter.
- Lombok: Extensively used to reduce boilerplate in domain models via @Data and @Builder.
- Services use constructor injection by using lombok RequiredArgsConstructor annotation
- Use cases should be the component opening and closing transactions. lookups can be done by just using the appropriate
  dao anywhere but write operations should go through a use case

# 🔄 Data Flow

Interactions with the system usually flow from the frontend through a use case and the database back to the frontend.

- User Action: A user clicks "Create Meetup" in the Vaadin DashboardView.
- UI Adapter: The MeetupCreateDialog collects data into a MeetupCreation DTO.
- Use Case: The UI calls MeetupWorkflows.create(creation).
- Domain Port: The workflow interacts with the MeetupDao interface.
- Persistence Adapter: The implementation of the DAO saves the data to the database.