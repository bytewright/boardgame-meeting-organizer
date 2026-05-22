# Architecture: BGMO

This project follows the Hexagonal Architecture (also known as Ports and Adapters). This pattern decouples the core
business logic from external concerns like databases, UI frameworks, and messaging bots. The project is thus divided
into three primary layers: Domain, Use Cases, and Adapters.

1. The Domain (The Core)
   Located in org.bytewright.bgmo.domain. This is the "inside" of the hexagon.
    - Model: Pure business objects like Game, MeetupEvent, and RegisteredUser.
    - Service: Interfaces that define how the core needs to interact with the outside world (e.g., `ModelDao` for
      data persistence and `NotificationTaskExecutor` for integrating chatbots). Also, this is the place for central
      services like security handling.

2. Use Cases (Application Layer)
   Located in org.bytewright.bgmo.usecases.
   These are the 'Interactors'. They orchestrate the flow of data to and from the domain entities.
   To be honest the distinction between use cases and services is vague and not clear-cut.
    - AdminWorkflows: User account approval, site wide settings. Usually has better audited logs.
    - UserWorkflows: Handles library additions and user profile management. Also manages notification settings.
    - MeetupWorkflows: Manages the lifecycle of an event, from creation to attendee confirmation.

3. Adapters (The "Outside")
   Located in org.bytewright.bgmo.adapter. These handle specific parts, the rest of the application does not need to
   know about. These are only allowed to interact with the domain and app layer, not other adapters.
   Internally, adapters sometimes mimic the same structure, also containing use cases and adapter specific domain models
   and services.
    - api.frontend: A Vaadin-based implementation of the web interface. It translates user clicks into calls to the Use
      Cases. Also allows the frontend to be defined in java instead of the usual web tech stack.
        - Frontend should make heavy use of vaadin i18n integration.
    - persistence: Implementation of the ModelDao ports, handling the actual SQL/database transactions. This works quite
      generic using a base entity and a base mapper from domain model to entities. The mappers are at the same time the
      DAO impls. This might seem strange at first but feels quite seamless as its making heavy use of MapStruct.
        - An example:
            - in domain is the model for users.
            - The ModelDao for this is called `UserDao extends ModelDao<RegisteredUser>`
            - in persistence is
              `RegisteredUserEntityMapper extends BaseEntityMapper<RegisteredUser, RegisteredUserEntity> implements RegisteredUserDao`
                - Has a JpaRepository injected
                  `RegisteredUserRepository extends JpaRepository<RegisteredUserEntity, UUID>, JpaSpecificationExecutor<RegisteredUserEntity>`
                - Uses mapstruct to implement mapping from entity to domain model, mostly making use of the
                  BaseEntityMapper. This class also contains special lookup methods defined in the domain dao interface,
                  e.g. `findAllByStatus`
    - notification: Adapters for Telegram, Signal, and E-Mail integration. Because of the hexagonal approach, these can
      be added without changing the core meetup logic.

# 🛠️ Key Design Patterns

- DTOs: Domain models often contain a UUID database id. When creating new models, e.g. a new meetup, smaller
  creation-DTOs should be used. For example in `MeetupEvent` exists a nested `MeetupCreation` class to decouple the UI
  form data from the persisted MeetupEvent.
- Models contain ids for relations: If a model has a foreign key, it only contains that entity's id which must be
  fetched using the DAOs. This avoids over-fetching and nicely defines transaction borders.
- Generics: ModelDao<MODEL_TYPE> provides a consistent, type-safe interface for all CRUD operations. Specific models can
  add their own DaoInterface which extends the base dao to add custom lookup methods which will later be implemented in
  the persistence adapter.
- Lombok: Extensively used to reduce boilerplate in domain models via `@Data` and `@Builder`. Service beans should be
  created with `@RequiredArgsConstructor` to use constructor injection.
- Use cases should be the component opening and closing transactions. Model lookups can be done by just using the
  appropriate DAO anywhere but write operations should go through a use case.
- Adapters can store settings in the DB using `AdapterSettingsProvider` (interface which defines adapter name, settings
  validator and default settings) and `AdapterSettingsDao` to retrieve the settings.

# 🔄 Data Flow

Interactions with the system usually flow from the frontend through a use case and the database back to the frontend.

- User Action: A user clicks "Create Meetup" in the Vaadin DashboardView.
- UI Adapter: The MeetupCreateDialog collects data into a MeetupCreation DTO.
- Use Case: The UI calls MeetupWorkflows.create(creation).
- Domain Port: The workflow interacts with the MeetupDao interface.
- Persistence Adapter: The implementation of the DAO saves the data to the database.

# Database

App uses PostgreSQL V18. Some datatypes used in entities might be db specific, e.g. JSONB column type.
For DB migrations Liquibase is used: mostly yaml format but the default admin is created using a xml file and a java
migration
`<customChange class="org.bytewright.bgmo.adapter.persistence.migrations.AddAdminTask"/>`