# 🔌 PORT CONFIGURATION REFERENCE

## 📊 Service Port Mapping

| Service | Old Port | New Port | Status |
|---------|----------|----------|--------|
| **User Service** | 8092 | **9010** | ✅ Updated |
| **Product Service** | 8080 | **9011** | ✅ Updated |
| **Merchant Service** | 8093 | **9012** | ✅ Updated |
| **Order/Cart Service** | 8084 | **9013** | ✅ Updated |
| **Frontend (Vite)** | 5173 | 5173 | ✅ No change |

---

## 🌐 Service URLs

### Frontend Environment Variables (`.env`)
```env
VITE_USER_SERVICE_URL=http://localhost:9010
VITE_PRODUCT_SERVICE_URL=http://localhost:9011
VITE_MERCHANT_SERVICE_URL=http://localhost:9012
VITE_CHECKOUT_SERVICE_URL=http://localhost:9013
```

### Backend Service Communication

#### User Service (Port 9010)
- **No external service dependencies**
- Provides authentication and user management
- Endpoints:
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /auth/validate`
  - `GET /users/{id}`

#### Product Service (Port 9011)
- **Depends on**: User Service (9010)
- Configuration:
  ```properties
  user.service.url=http://localhost:9010
  ```
- Endpoints:
  - `GET /products`
  - `GET /products/{id}`
  - `POST /products` (Merchant only)
  - `PUT /products/{id}` (Merchant only)
  - `DELETE /products/{id}` (Merchant only)

#### Merchant Service (Port 9012)
- **Depends on**: User Service (9010)
- Configuration:
  ```properties
  user.service.url=http://localhost:9010
  ```
- Endpoints:
  - `GET /merchants/{id}`
  - `POST /merchants`
  - `PUT /merchants/{id}`
  - `GET /merchants/{id}/stats`

#### Order/Cart Service (Port 9013)
- **Depends on**: User Service (9010), Product Service (9011)
- Configuration:
  ```properties
  user.service.url=http://localhost:9010
  product.service.url=http://localhost:9011
  ```
- Endpoints:
  - `GET /cart/{userId}`
  - `POST /cart`
  - `PUT /cart`
  - `DELETE /cart/{userId}/{productId}/{merchantId}`
  - `POST /checkout`
  - `GET /orders/{userId}`

---

## 🔄 Service Dependency Graph

```
┌─────────────────┐
│  User Service   │
│   Port: 9010    │
└────────┬────────┘
         │
         │ Validates JWT tokens
         │
    ┌────┴────┬────────────┬──────────────┐
    │         │            │              │
    ▼         ▼            ▼              ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌──────────────┐
│Product │ │Merchant│ │ Order  │ │   Frontend   │
│Service │ │Service │ │Service │ │  Port: 5173  │
│  9011  │ │  9012  │ │  9013  │ └──────────────┘
└────────┘ └────────┘ └───┬────┘
                          │
                          │ Validates products
                          │
                      ┌───▼────┐
                      │Product │
                      │Service │
                      │  9011  │
                      └────────┘
```

---

## 🗄️ Database Configuration

### MySQL (Port 3306)
| Service | Database Name | Tables |
|---------|---------------|--------|
| User Service | `user_service` | `users`, `roles` |
| Merchant Service | `merchants` | `merchant` |
| Order Service | `checkout_service` | `cart_items`, `orders`, `order_items` |

### MongoDB (Port 27017)
| Service | Database Name | Collections |
|---------|---------------|-------------|
| Product Service | `products_db` | `products` |
| Merchant Service | `merchant_db` | `merchant_stats` |

---

## 🔐 CORS Configuration

All backend services allow requests from:
```
http://localhost:5173
```

Some services use `@CrossOrigin(origins = "*")` for development convenience.

---

## 🚦 Startup Order

**IMPORTANT**: Start services in this order to ensure dependencies are available:

1. **Databases** (MySQL + MongoDB)
2. **User Service** (Port 9010) - No dependencies
3. **Product Service** (Port 9011) - Depends on User Service
4. **Merchant Service** (Port 9012) - Depends on User Service
5. **Order Service** (Port 9013) - Depends on User + Product Services
6. **Frontend** (Port 5173) - Depends on all backend services

---

## 🧪 Health Check URLs

After starting each service, verify it's running:

```bash
# User Service
curl http://localhost:9010/actuator/health

# Product Service
curl http://localhost:9011/actuator/health

# Merchant Service
curl http://localhost:9012/actuator/health

# Order Service
curl http://localhost:9013/actuator/health

# Frontend
curl http://localhost:5173
```

---

## 🔧 Troubleshooting

### Port Already in Use
If you get "Port already in use" error:

```bash
# Find process using the port (example for 9010)
lsof -i :9010

# Kill the process
kill -9 <PID>
```

### Service Can't Connect to Another Service
1. Check if the dependency service is running
2. Verify port numbers in `application.properties`
3. Check firewall settings
4. Verify CORS configuration

### Frontend Can't Connect to Backend
1. Check `.env` file has correct URLs
2. Restart Vite dev server after changing `.env`
3. Check browser console for CORS errors
4. Verify backend services are running

---

## 📝 Quick Start Commands

```bash
# Start all backend services (run in separate terminals)
cd /Users/trishiry/Downloads/user-service && ./gradlew bootRun
cd /Users/trishiry/Downloads/product-services && ./gradlew bootRun
cd /Users/trishiry/ecommerce-platform/backend/merchant-service/merchant-service-final && ./gradlew bootRun
cd /Users/trishiry/Downloads/order-service && ./gradlew bootRun

# Start frontend
cd /Users/trishiry/ecommerce-frontend && npm run dev
```

---

## ✅ Verification Checklist

After starting all services:

- [ ] User Service responds on http://localhost:9010
- [ ] Product Service responds on http://localhost:9011
- [ ] Merchant Service responds on http://localhost:9012
- [ ] Order Service responds on http://localhost:9013
- [ ] Frontend loads on http://localhost:5173
- [ ] Can register a new user
- [ ] Can login
- [ ] Can view products
- [ ] Can add to cart
- [ ] Can checkout
- [ ] Can view orders

---

**All port configurations are now consistent across the entire application!** ✅

