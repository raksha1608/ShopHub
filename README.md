# ShopHub: Architecture


## System Architecture Overview
```mermaid
graph TD
  Customer -->|"Browse / Login / Purchase"| FE["Frontend: React + Vite + TailwindCSS\nAxios HTTP (.env URLs)"]
  Merchant -->|"Manage Products / Orders"| FE
  FE -->|"Register / Login / Profile"| US["User Service (Spring Boot + MySQL)"]
  FE -->|"Merchant APIs"| MS["Merchant Service (Spring Boot + MySQL + MongoDB)"]
  FE -->|"Cart & Order APIs"| COS["Order & Cart Service (Spring Boot + MySQL)"]

  subgraph Databases
    MYSQL["MySQL: Users, Orders, Merchants"]
    MONGO["MongoDB: Products, Merchant Stats"]
  end

  US --> MS
  MS --> PS["Product Service (Spring Boot + MongoDB)"]
  PS --> COS
  COS --> PS

  US --> MYSQL
  MS --> MYSQL
  MS --> MONGO
  PS --> MONGO
  COS --> MYSQL
```

---

## User Login Flow
```mermaid
sequenceDiagram
  actor User
  participant FE as "Frontend (React)"
  participant US as "User Service"
  participant DB as "MySQL (users)"
  User->>FE: Enter credentials
  FE->>US: POST /api/auth/login or /register
  US->>DB: Validate or insert user
  US-->>FE: JWT + Refresh Token
```

---

## Merchant Product Flow
```mermaid
sequenceDiagram
  actor Merchant
  participant FE as "Frontend"
  participant MS as "Merchant Service"
  participant PS as "Product Service"
  participant DB as "MongoDB (products)"
  Merchant->>FE: Fill product form
  FE->>MS: POST /api/merchant/products
  MS->>PS: POST /api/products
  PS->>DB: Insert product document
  PS-->>FE: Product added
```

---

## Cart Flow
```mermaid
sequenceDiagram
  actor User
  participant FE as "Frontend"
  participant COS as "Cart & Order Service"
  participant PS as "Product Service"
  participant CARTDB as "MySQL (cart)"
  User->>FE: Add to cart
  FE->>COS: POST /api/cart/add
  COS->>PS: GET /api/products/{id}
  PS-->>COS: Product details
  COS->>CARTDB: Save cart item
  COS-->>FE: OK
```

---

##  Order Checkout Flow
```mermaid
sequenceDiagram
  actor User
  participant FE as "Frontend"
  participant COS as "Cart & Order Service"
  participant PS as "Product Service"
  participant ODB as "MySQL (orders)"
  User->>FE: Place Order
  FE->>COS: POST /api/orders/checkout
  COS->>PS: Check stock
  PS-->>COS: Stock verified
  COS->>ODB: Insert order record
  COS-->>FE: Confirm order
```

---

##  Entity-Relationship Diagram (ERD)
```mermaid
erDiagram
  USERS {
    int id
    string email
    string password
    string role
  }
  MERCHANTS {
    int id
    int user_id
  }
  CARTS {
    int id
    int user_id
  }
  CART_ITEMS {
    int id
    int cart_id
    int product_id
    int merchant_id
    int price
    int quantity
  }
  ORDERS {
    int id
    int user_id
    float total_amount
  }
  ORDER_ITEMS {
    int id
    int order_id
    int product_id
    double price
    int quantity
    int merchant_id
  }

  USERS ||--o{ MERCHANTS : owns
  USERS ||--o{ CARTS : has
  CARTS ||--o{ CART_ITEMS : contains
  USERS ||--o{ ORDERS : places
  ORDERS ||--o{ ORDER_ITEMS : includes
```

---

## User Service Component Diagram
```mermaid
graph TD
  subgraph Controller
    AC["AuthController"]
    UC["UserController"]
    UDC["UserDataController"]
  end
  subgraph Service
    US["UserService"]
    AS["AuthService"]
    RS["RefreshTokenService"]
  end
  subgraph Repository
    UR["UserRepository"]
    RR["RefreshTokenRepository"]
  end
  subgraph Security
    JF["JwtFilter"]
    JU["JwtUtil"]
    SC["SecurityConfig"]
  end
  Controller --> Service
  Service --> Repository
  Security --> Service
```

---

## Merchant Service Component Diagram
```mermaid
graph TD
  subgraph Controller
    MC["MerchantController"]
  end
  subgraph Service
    MS["MerchantService"]
  end
  subgraph Repository
    MR["MerchantRepository"]
    PR["ProductRepository"]
    MSR["MerchantStatsRepository"]
  end
  subgraph Entity
    ME["Merchant"]
    MST["MerchantStats"]
  end
  MC --> MS
  MS --> MR
  MS --> PR
  MS --> MSR
```

---

## Product Service Component Diagram
```mermaid
graph TD
  subgraph Controller
    PC["ProductController"]
  end
  subgraph Service
    PS["ProductService"]
    FS["FileStorageService"]
  end
  subgraph Repository
    PR["ProductRepository"]
  end
  subgraph Util
    AV["AuthValidator"]
    IS["InputSanitizer"]
  end
  PC --> PS
  PS --> PR
  AV --> PC
  IS --> PS
```

---

## Cart & Order Service Component Diagram
```mermaid
graph TD
  subgraph Controller
    CIC["CartItemsController"]
    OC["OrderController"]
  end
  subgraph Service
    CS["CartService"]
    CIS["CartItemsService"]
    OS["OrderService"]
  end
  subgraph Repository
    CIR["CartItemsRepository"]
    ORR["OrderRepository"]
  end
  subgraph Util
    NV["NotificationUtil"]
    AV["AuthValidator"]
  end
  CIC --> CIS
  OC --> OS
  CIS --> CIR
  CS --> CIR
  OS --> ORR
  NV --> OS
  AV --> CIC
```

---

## JWT & Refresh Token Flow
```mermaid
sequenceDiagram
  actor User
  participant FE as "Frontend"
  participant AC as "AuthController"
  participant US as "UserService"
  participant JWT as "JwtUtil"
  participant RTS as "RefreshTokenService"
  participant DB as "MySQL"
  User->>FE: POST /auth/login
  FE->>AC: Credentials
  AC->>US: Validate user
  US->>JWT: Generate JWT
  US->>RTS: Create refresh token
  JWT-->>US: Access token
  RTS-->>US: Refresh token
  US-->>FE: Tokens (access + refresh)
```

---

## Refresh Token Renewal Flow
```mermaid
sequenceDiagram
  actor User
  participant FE as "Frontend"
  participant AC as "AuthController"
  participant RTS as "RefreshTokenService"
  participant UR as "UserRepository"
  participant JWT as "JwtUtil"
  participant DB as "MySQL"
  User->>FE: POST /auth/refresh
  FE->>AC: Send refresh token
  AC->>RTS: Validate refresh token
  RTS->>UR: Find user by email
  UR-->>RTS: User object
  RTS->>JWT: Generate new access token
  JWT-->>RTS: New token
  RTS-->>FE: Return new access + refresh tokens
```
