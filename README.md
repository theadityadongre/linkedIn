# LinkedIn Microservices

A Spring Boot microservices architecture resembling LinkedIn, featuring service-to-service communication, API gateway, and event-driven messaging.

## 🏗️ Architecture Overview

```mermaid
graph TB
    Client["👤 Client"]
    
    Client -->|HTTP Requests| Gateway["🌐 API Gateway<br/>Port: 8080<br/>Spring Cloud Gateway"]
    
    Gateway -->|Route & Auth| UserSvc["👤 User Service<br/>Port: 9020<br/>Auth & Profile"]
    Gateway -->|Route & Auth| PostsSvc["📝 Posts Service<br/>Port: 9010<br/>Posts & Likes"]
    Gateway -->|Route & Auth| ConnSvc["🔗 Connections Service<br/>Port: 9030<br/>Connections"]
    
    UserSvc --> UserDB[(userDB<br/>PostgreSQL)]
    PostsSvc --> PostsDB[(postsDB<br/>PostgreSQL)]
    ConnSvc --> ConnDB[(connectionsDB<br/>PostgreSQL)]
    
    PostsSvc -->|Publish Events| Kafka["📨 Kafka Event Bus<br/>Port: 9092"]
    ConnSvc -->|Publish Events| Kafka
    
    Kafka -->|Consume Events| NotifSvc["🔔 Notification Service<br/>Port: 9040"]
    NotifSvc --> NotifDB[(notificationDB<br/>PostgreSQL)]
    
    Eureka["🔍 Eureka Discovery<br/>Port: 8761<br/>Service Registry"]
    
    UserSvc -.->|Register| Eureka
    PostsSvc -.->|Register| Eureka
    ConnSvc -.->|Register| Eureka
    NotifSvc -.->|Register| Eureka
    Gateway -.->|Discover Services| Eureka
    
    style Gateway fill:#ff6b6b
    style UserSvc fill:#4ecdc4
    style PostsSvc fill:#45b7d1
    style ConnSvc fill:#96ceb4
    style NotifSvc fill:#ffeaa7
    style Kafka fill:#dfe6e9
    style Eureka fill:#a29bfe
```

## 📋 Microservices

### 1. **API Gateway** (Port: 8080)
- Central entry point for all client requests
- Route management and load balancing
- JWT authentication and authorization
- Request filtering and preprocessing

### 2. **User Service** (Port: 9020)
- User registration and authentication
- JWT token generation and validation
- User profile management
- Password hashing with bcrypt

### 3. **Posts Service** (Port: 9010)
- Create, read, update, delete posts
- Like functionality for posts
- Event publishing for post creation and likes
- Database: PostgreSQL

### 4. **Connections Service** (Port: 9030)
- Connection request management
- Accept/reject connection requests
- Track user connections
- Event-driven communication with other services

### 5. **Notification Service** (Port: 9040)
- Consume events from Kafka
- Handle connection requests and post notifications
- Event processing from Posts and Connections services
- Multi-service event aggregation

### 6. **Discovery Server** (Port: 8761)
- Eureka service registry
- Service registration and discovery
- Load balancing support
- Health checks

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 12+
- Apache Kafka 3.0+
- Docker (optional)

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/theadityadongre/linkedIn.git
   cd linkedInApp
   ```

2. **Start PostgreSQL**
   ```bash
   # macOS with Homebrew
   brew services start postgresql
   
   # Or manually
   postgres -D /usr/local/var/postgres
   ```

3. **Start Kafka**
   ```bash
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # Start Kafka Server
   bin/kafka-server-start.sh config/server.properties
   ```

4. **Create Database**
   ```sql
   CREATE DATABASE postsDB;
   CREATE DATABASE userDB;
   CREATE DATABASE connectionsDB;
   CREATE DATABASE notificationDB;
   ```

5. **Start Services** (in order)
   ```bash
   # Terminal 1: Discovery Server
   cd discovery-server && mvn spring-boot:run
   
   # Terminal 2: User Service
   cd user-service && mvn spring-boot:run
   
   # Terminal 3: Posts Service
   cd posts-service && mvn spring-boot:run
   
   # Terminal 4: Connections Service
   cd connections-service && mvn spring-boot:run
   
   # Terminal 5: Notification Service
   cd notification-service && mvn spring-boot:run
   
   # Terminal 6: API Gateway
   cd api-gateway && mvn spring-boot:run
   ```

## 📡 API Endpoints

### User Service
```
POST   /api/v1/users/signup       - Register new user
POST   /api/v1/users/login        - User login
GET    /api/v1/users/{id}         - Get user profile
```

### Posts Service
```
POST   /api/v1/posts              - Create post
GET    /api/v1/posts              - Get all posts
GET    /api/v1/posts/{id}         - Get post details
DELETE /api/v1/posts/{id}         - Delete post
POST   /api/v1/posts/{id}/like    - Like a post
```

### Connections Service
```
POST   /api/v1/connections/request      - Send connection request
POST   /api/v1/connections/accept/{id}  - Accept connection
GET    /api/v1/connections              - Get user connections
```

## 🔄 Inter-Service Communication

- **Sync**: REST calls via Feign Client
- **Async**: Kafka event streaming for eventual consistency

## 📊 Workflow Diagrams

### 1️⃣ User Registration & Login Flow

```mermaid
sequenceDiagram
    actor User as User (Client)
    participant Gateway as API Gateway
    participant UserSvc as User Service
    participant UserDB as User DB
    
    User->>Gateway: POST /api/v1/users/signup<br/>(email, password, name)
    Gateway->>UserSvc: Forward Request
    UserSvc->>UserSvc: Hash Password (BCrypt)
    UserSvc->>UserDB: Save New User
    UserDB-->>UserSvc: User Created
    UserSvc-->>Gateway: User Response + JWT Token
    Gateway-->>User: 201 Created + Token
    
    Note over User,UserSvc: User Login Flow
    User->>Gateway: POST /api/v1/users/login<br/>(email, password)
    Gateway->>UserSvc: Forward Request
    UserSvc->>UserDB: Fetch User by Email
    UserDB-->>UserSvc: User Record
    UserSvc->>UserSvc: Verify Password
    UserSvc->>UserSvc: Generate JWT Token
    UserSvc-->>Gateway: Login Response + Token
    Gateway-->>User: 200 OK + Token
```

### 2️⃣ Create Post & Notification Flow

```mermaid
sequenceDiagram
    actor User as User (Client)
    participant Gateway as API Gateway
    participant PostsSvc as Posts Service
    participant PostsDB as Posts DB
    participant Kafka as Kafka
    participant NotifSvc as Notification Service
    participant NotifDB as Notification DB
    
    User->>Gateway: POST /api/v1/posts<br/>with JWT Token
    Gateway->>Gateway: Validate JWT Token
    Gateway->>PostsSvc: Forward Request
    PostsSvc->>PostsDB: Create Post Record
    PostsDB-->>PostsSvc: Post Created
    
    PostsSvc->>Kafka: Publish PostCreatedEvent
    Kafka-->>PostsSvc: Event Accepted
    PostsSvc-->>Gateway: Post Response
    Gateway-->>User: 201 Created
    
    Note over Kafka,NotifSvc: Async Event Processing
    Kafka->>NotifSvc: Consume PostCreatedEvent
    NotifSvc->>NotifDB: Store Notification<br/>(post_id, user_id)
    NotifDB-->>NotifSvc: Notification Saved
```

### 3️⃣ Connection Request & Accept Flow

```mermaid
sequenceDiagram
    actor User1 as User A (Requester)
    actor User2 as User B (Recipient)
    participant Gateway as API Gateway
    participant ConnSvc as Connections Service
    participant ConnDB as Connections DB
    participant Kafka as Kafka
    participant NotifSvc as Notification Service
    
    User1->>Gateway: POST /api/v1/connections/request<br/>(targetUserId)
    Gateway->>ConnSvc: Forward Request
    ConnSvc->>ConnDB: Create Connection Request
    ConnDB-->>ConnSvc: Request Created
    ConnSvc->>Kafka: Publish SendConnectionRequestEvent
    Kafka-->>ConnSvc: Event Accepted
    ConnSvc-->>Gateway: Request Response
    Gateway-->>User1: 201 Request Sent
    
    Kafka->>NotifSvc: Consume SendConnectionRequestEvent
    NotifSvc->>NotifSvc: Generate Notification
    
    Note over User2,ConnSvc: Accept Connection
    User2->>Gateway: POST /api/v1/connections/accept/{requestId}
    Gateway->>ConnSvc: Forward Request
    ConnSvc->>ConnDB: Update Connection Status
    ConnDB-->>ConnSvc: Connection Accepted
    ConnSvc->>Kafka: Publish AcceptConnectionRequestEvent
    Kafka-->>ConnSvc: Event Accepted
    ConnSvc-->>Gateway: Accept Response
    Gateway-->>User2: 200 Connection Accepted
    
    Kafka->>NotifSvc: Consume AcceptConnectionRequestEvent
    NotifSvc->>NotifSvc: Notify User A
```

### 4️⃣ Like Post Flow

```mermaid
sequenceDiagram
    actor User as User (Client)
    participant Gateway as API Gateway
    participant PostsSvc as Posts Service
    participant PostsDB as Posts DB
    participant Kafka as Kafka
    participant NotifSvc as Notification Service
    
    User->>Gateway: POST /api/v1/posts/{postId}/like<br/>with JWT Token
    Gateway->>Gateway: Validate JWT + Extract UserId
    Gateway->>PostsSvc: Forward Request
    
    PostsSvc->>PostsDB: Check if Like Exists
    alt Like Not Exists
        PostsSvc->>PostsDB: Create PostLike Record
        PostsDB-->>PostsSvc: Like Created
        PostsSvc->>Kafka: Publish PostLikedEvent
    else Like Exists
        PostsSvc->>PostsDB: Delete PostLike Record
        PostsDB-->>PostsSvc: Like Removed
    end
    
    PostsSvc-->>Gateway: Like Response
    Gateway-->>User: 200 OK
    
    Kafka->>NotifSvc: Consume PostLikedEvent
    NotifSvc->>NotifSvc: Create Like Notification
```

### 5️⃣ Service-to-Service Communication Pattern

```mermaid
graph LR
    A["📝 Posts Service"] -->|Feign Client<br/>REST Call| B["🔗 Connections Service"]
    B -->|Response| A
    
    A -->|Event Publishing| C["📨 Kafka"]
    C -->|Event Subscription| D["🔔 Notification Service"]
    
    D -->|Feign Client<br/>REST Call| B
    B -->|User Info| D
    
    style A fill:#45b7d1
    style B fill:#96ceb4
    style C fill:#dfe6e9
    style D fill:#ffeaa7
```

## 🗄️ Database Architecture

Each microservice has its own database (Database per service pattern):
- **User Service**: userDB (User, Auth data)
- **Posts Service**: postsDB (Posts, Likes)
- **Connections Service**: connectionsDB (Connections, Requests)
- **Notification Service**: notificationDB (Notifications)

## 🔐 Security

- JWT token-based authentication
- Gateway-level request authentication
- Password hashing with BCrypt
- Encrypted inter-service communication

## 📚 Tech Stack

- **Framework**: Spring Boot 3.x
- **Cloud**: Spring Cloud (Eureka, Gateway, OpenFeign)
- **Database**: PostgreSQL
- **Messaging**: Apache Kafka
- **Build**: Maven
- **Authentication**: JWT (JSON Web Tokens)

## 🛠️ Development

### Running Tests
```bash
cd [service-name]
mvn test
```

### Build
```bash
cd [service-name]
mvn clean package
```

### Docker (Optional)
```bash
docker-compose up
```

### Development Workflow

```mermaid
graph LR
    A["📝 Code Changes"] --> B["🔨 Build<br/>mvn clean package"]
    B --> C["🧪 Unit Tests"]
    C -->|Pass| D["✅ Commit & Push"]
    C -->|Fail| A
    D --> E["🚀 Deploy to Dev"]
    E --> F["🧫 Integration Tests"]
    F -->|Pass| G["📦 Ready for Prod"]
    F -->|Fail| A
    
    style A fill:#74b9ff
    style B fill:#81ecec
    style C fill:#55efc4
    style D fill:#a29bfe
    style E fill:#fd79a8
    style F fill:#fdcb6e
    style G fill:#6c5ce7
```

## 📊 Monitoring

- **Service Registry**: http://localhost:8761 (Eureka Dashboard)
- **API Gateway**: http://localhost:8080

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Submit a pull request

## 📝 License

This project is licensed under the MIT License.

## 📧 Contact

For questions or support, contact the development team.

---

**Last Updated**: July 2026
