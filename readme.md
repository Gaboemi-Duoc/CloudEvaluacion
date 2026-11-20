# CloudEvaluacion - Backend BFF (Spring Boot)

Backend **BFF (Backend For Frontend)** del sistema **CloudEvaluacion** para la asignatura **DSY1107 Desarrollo Cloud Native**. Expone una API REST protegida con **AWS Cognito** (IDaaS) y validación por **JWT**, actúa como **productor de eventos** hacia **AWS SNS** y como **cliente de invocación síncrona** de **AWS Lambda**, y delega la persistencia en **Supabase (PostgreSQL)** a través de los microservicios serverless.

> **Nota:** Este repositorio contiene **solo el BFF**. Los consumidores (Lambda functions), las colas (SQS) se documentan en el repositorio [`CloudLambda`](https://github.com/gaboemi-duoc/CloudLambda). El frontend se documenta en [`CloudFrontend`](https://github.com/gaboemi-duoc/CloudFrontend).

---

## Descripción

El backend implementa la lógica de negocio de un sistema de gestión académica con tres roles:

| Rol | Responsabilidades |
|---|---|
| **ADMIN** | Crear cursos, asignar profesores, inscribir y eliminar alumnos, generar reportes |
| **TEACHER** | Ver sus cursos, gestionar alumnos inscritos, crear tareas |
| **STUDENT** | Ver cursos inscritos, auto-inscribirse, ver tareas asignadas |

El sistema **no gestiona el login**: delega la autenticación a **AWS Cognito**. El backend solo valida el JWT que recibe en cada petición y extrae la identidad y los roles desde los claims del token.

El BFF cumple dos roles de integración con la capa serverless:

- **Productor de eventos (asíncrono)**: publica comandos en **AWS SNS** para operaciones de escritura (crear curso, inscribir alumno, crear tarea, generar reporte). El BFF responde **`202 Accepted`** de inmediato y el procesamiento real ocurre en las Lambdas consumidoras de **SQS**.
- **Cliente de lectura (síncrono)**: invoca directamente funciones **AWS Lambda** para consultas (listar cursos, obtener alumnos, listar tareas), obteniendo la respuesta en el mismo request.

---

## Arquitectura

```
+--------------+    HTTPS     +--------------+    HTTP     +------------------+
|              |------------->|              |------------>|                  |
|  React SPA   |  Bearer JWT  | AWS API      |  + header   |  EC2             |
|  (Vercel)    |              | Gateway      |  X-Secret-  |  +------------+  |
|              |<-------------| (HTTP API)   |  Gateway    |  | Spring Boot|  |
|              |              |              |             |  | BFF (Docker)|  |
+--------------+              +--------------+             |  +-----+------+  |
       |                                                     +--------+--------+
       |                                                              |
       |  Authorization Code                                          |
       v                                                              |
+--------------+                                                      |
| AWS Cognito  |                                                      |
| (User Pool)  |                                                      |
+--------------+                                                      |
                                                                      |
                                        +-----------------------------+
                                        |
                                        | (Asíncrono)
                                        v
                               +------------------+
                               |    AWS SNS       |
                               |  (Topic:         |
                               | CloudStudyEvents)|
                               +--------+---------+
                                        |
                                        | Filter Policy
                                        v
                               +------------------+
                               |    AWS SQS       |
                               |  DbWriteQueue    |
                               |  NotificationQueue|
                               +--------+---------+
                                        |
                                        | (max 5 reintentos)
                                        v
                               +------------------+                            +------------------+
                               |   AWS Lambda     |                            |   Notification   |
                               |  (Consumer)      |--------------------------->|       API        |
                               +--------+---------+                            +--------+---------+
                                        |
                                        |
                                        v
     +--------------------------------------------------+
     |              Supabase (PostgreSQL)               |
     +--------------------------------------------------+
```

### Flujo de una petición

1. El frontend obtiene un **`id_token`** de Cognito mediante el flujo Authorization Code + PKCE.
2. Envía la petición a API Gateway con `Authorization: Bearer <id_token>`.
3. API Gateway inyecta el header `X-Secret-Gateway` (desde una variable de stage) y reenvía a EC2.
4. Spring Boot verifica en orden:
   - **`SecretGatewayFilter`**: rechaza con 403 si falta `X-Secret-Gateway`.
   - **`BearerTokenAuthenticationFilter`**: valida la firma del JWT contra el JWKS de Cognito, el issuer y la expiración.
   - **`CognitoJwtAuthenticationConverter`**: traduce `cognito:groups` a authorities `ROLE_*`.
   - **`@PreAuthorize`** en el controlador: autoriza según el rol.
5. Según el verbo HTTP:
   - **GET** → invocación síncrona a **Lambda** (lectura) → respuesta inmediata.
   - **POST / PUT / DELETE** → publicación en **SNS** (escritura) → respuesta **`202 Accepted`**.

---

## Patrón de mensajería asíncrona (EP3)

El BFF implementa el rol de **productor** dentro de una arquitectura **orientada a eventos** basada en servicios gestionados de AWS. La mensajería desacoplada permite que las operaciones de escritura no bloqueen al cliente y que cada microservicio consuma únicamente los eventos que le competen.

### Equivalencias conceptuales (mensajería → AWS)

| Concepto de mensajería | Implementación AWS |
|---|---|
| **Exchange** | **AWS SNS Topic** (`CloudStudyEvents`) |
| **Queue** | **AWS SQS** (`DbWriteQueue`, `NotificationQueue`) |
| **Binding** | Suscripción SNS con **Filter Policy** por atributo `action` |
| **Producer** | **Este BFF** (`AwsSnsClient`) |
| **Consumer** | **AWS Lambda** (repositorio `CloudLambda`) |


### Clases responsables

| Clase | Rol en la arquitectura |
|---|---|
| `AwsSnsClient` | **Productor**. Serializa el evento a JSON, lo publica en el topic SNS con atributos para el enrutamiento por *filter policy*. |
| `AwsLambdaClient` | **Cliente síncrono**. Invoca funciones Lambda de lectura y mapea la respuesta (`statusCode`, `body`) a un `ResponseEntity`. |
| `AdminCourseController`, `TeacherCourseController`, `StudentController`, `ReportController` | **Puntos de entrada**. Delegan en `AwsSnsClient` (escrituras) o `AwsLambdaClient` (lecturas). |

### Contrato de eventos

Cada evento publicado en SNS sigue el formato:

```json
{
  "action": "ADMIN_CREATE_COURSE",
  "data": { "...": "..." }
}
```

Adicionalmente, el atributo de mensaje `action` se envía como **MessageAttribute** (`DataType=String`), lo que permite a SQS aplicar **filter policies** para enrutar el evento únicamente a las colas interesadas:

| `action` | Cola destino | Lambda consumidora |
|---|---|---|
| `ADMIN_CREATE_COURSE` | `DbWriteQueue` | `lambda-course-service` |
| `ADMIN_ASSIGN_TEACHER` | `DbWriteQueue` | `lambda-course-service` |
| `ADMIN_DELETE_COURSE` | `DbWriteQueue` | `lambda-course-service` |
| `ADMIN_ENROLL_STUDENT` | `DbWriteQueue` | `lambda-enrollment-service` |
| `TEACHER_ENROLL_STUDENT` | `DbWriteQueue` | `lambda-enrollment-service` |
| `TEACHER_CREATE_TASK` | `DbWriteQueue` | `lambda-task-service` |
| `STUDENT_ENROLL` | `DbWriteQueue` | `lambda-enrollment-service` |
| `GENERATE_REPORT` | `DbWriteQueue` + `NotificationQueue` | `lambda-report-generator` + `lambda-notification-worker` |

### Operaciones asíncronas expuestas al frontend

Los endpoints de escritura responden con **`202 Accepted`** y un cuerpo informativo:

```json
{ "message": "Course creation initiated" }
```

El frontend está preparado para este patrón mediante **actualizaciones optimistas** y temporizadores de recarga (`setTimeout`) que sincronizan la vista una vez que la Lambda ha persistido el cambio en Supabase. Para más detalle, ver el README del frontend.

---

## Stack tecnológico

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Security | 7.x |
| Spring Data JPA | (incluido) |
| Hibernate | 7.x |
| PostgreSQL | 16+ (Supabase) |
| AWS SDK for Java v2 | `lambda`, `sns` |
| Docker | Última estable |
| Gradle | Wrapper incluido |

Dependencias clave:

- `spring-boot-starter-security`: filtros de seguridad
- `spring-boot-starter-security-oauth2-resource-server`: validación de JWT
- `spring-boot-starter-webmvc`: API REST
- `spring-boot-starter-data-jpa`: persistencia
- `spring-boot-starter-validation`: validación de DTOs
- `software.amazon.awssdk:lambda`: invocación síncrona de funciones
- `software.amazon.awssdk:sns`: publicación de eventos asíncronos
- `org.postgresql:postgresql`: driver JDBC
- `org.projectlombok:lombok`: reducción de boilerplate
- `tools.jackson:jackson-databind` (Jackson 3): serialización JSON

---

## Estructura del proyecto

```
src/main/java/alumno/gaboemi/cloudevaluacion/
├── CloudEvaluacionApplication.java
├── client/
│   ├── AwsLambdaClient.java        # Invocación síncrona a Lambda
│   └── AwsSnsClient.java           # Productor de eventos hacia SNS
├── config/
│   ├── SecurityConfig.java
│   └── SecretGatewayFilter.java
├── security/
│   └── CognitoJwtAuthenticationConverter.java
├── controller/
│   ├── ApiController.java          # /health, /api/me
│   ├── AdminCourseController.java
│   ├── TeacherCourseController.java
│   ├── StudentController.java
│   └── ReportController.java
└── dto/
    ├── AssignTeacherRequest.java
    ├── CreateCourseRequest.java
    ├── CreateTaskRequest.java
    ├── EnrollStudentRequest.java
    ├── MeResponse.java
    └── MessageDto.java
```

> La persistencia (entidades, repositorios y servicios) vive en los microservicios serverless del repositorio `CloudLambda`. El BFF **no** accede directamente a PostgreSQL en este patrón: delega en Lambda.

---

## Requisitos previos

- **JDK 21** ([Temurin](https://adoptium.net/) recomendado)
- **Docker** (para el despliegue)
- **Cuenta de AWS** con:
  - User Pool de Cognito configurado
  - Instancia EC2 (Amazon Linux 2023)
  - API Gateway HTTP API
  - Topic SNS (`CloudStudyEvents`) y colas SQS (ver repo `CloudLambda`)
  - Funciones Lambda desplegadas
- **Gradle Wrapper** (incluido en el repositorio, no necesitas instalar Gradle)

---

## Configuración

Todas las variables de entorno tienen valores por defecto para desarrollo local, pero en producción **siempre** deben sobrescribirse.

### Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `PORT` | Puerto del servidor | `8080` |
| `PUBLIC_HOST_URL` | URL pública del backend | `http://localhost:8080` |
| `API_GATEWAY_SECRET` | Secreto compartido con API Gateway | `wrcUOJQGxFQg4Njl...` |
| `COGNITO_ISSUER_URI` | Issuer URI del User Pool | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_NN6vjj3k5` |
| `SNS_TOPIC_ARN` | ARN del topic SNS de eventos | `arn:aws:sns:us-east-1:123456789012:CloudStudyEvents` |
| `SNS_ENABLED` | Habilita/deshabilita la publicación en SNS | `true` |
| `AWS_REGION` | Región de AWS para los clientes SDK | `us-east-1` |

### `application.properties`

```properties
spring.application.name=CloudEvaluacion

server.port=${PORT:8080}
app.public-host=${PUBLIC_HOST_URL:http://localhost:8080}
app.gateway.secret=${API_GATEWAY_SECRET:secreto_local}

# ---------- Cognito (IDaaS) ----------
spring.security.oauth2.resourceserver.jwt.issuer-uri=${COGNITO_ISSUER_URI:https://cognito-idp.us-east-1.amazonaws.com/us-east-1_NN6vjj3k5}

# ---------- Mensajería asíncrona (SNS) ----------
app.messaging.sns.topic-arn=${SNS_TOPIC_ARN:arn:aws:sns:us-east-1:000000000000:CloudStudyEvents}
app.messaging.sns.enabled=${SNS_ENABLED:true}

logging.level.org.springframework.security=INFO
```

> En local, si no se dispone de credenciales AWS o del topic real, se puede arrancar con `SNS_ENABLED=false` para que `AwsSnsClient` opere en modo *no-op* (registro en logs) y así evitar dependencias externas durante el desarrollo.

---

## Ejecución local

### 1. Clonar el repositorio

```bash
git clone https://github.com/gaboemi-duoc/cloudevaluacion.git
cd cloudevaluacion
```

### 2. Configurar credenciales AWS

El SDK de AWS v2 toma las credenciales del **default provider chain**:

```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:CloudStudyEvents
```

Alternativamente, usar el perfil de AWS CLI: `export AWS_PROFILE=default`.

### 3. Ejecutar la aplicación

```bash
./gradlew bootRun
```

En Windows:

```powershell
.\gradlew.bat bootRun
```

La aplicación arranca en `http://localhost:8080` con el secreto `secreto_local` por defecto.

### 4. Verificar el arranque

```bash
curl -i -H "X-Secret-Gateway: secreto_local" http://localhost:8080/health
```

Salida esperada:

```
HTTP/1.1 200 OK
{"message":"API is UP","error":null}
```

---

## Endpoints de la API

Todos los endpoints (excepto `/health`) requieren:

- Header `Authorization: Bearer <id_token>` validado contra Cognito
- Header `X-Secret-Gateway: <secreto>` validado por `SecretGatewayFilter`

La columna **Tipo** indica el patrón de integración:

- **Sync**: el BFF invoca Lambda y espera respuesta (200).
- **Async**: el BFF publica en SNS y responde **`202 Accepted`**.

### Públicos

| Método | Ruta | Tipo | Descripción |
|---|---|---|---|
| `GET` | `/health` | Sync | Health check |

### Identidad

| Método | Ruta | Rol | Tipo | Descripción |
|---|---|---|---|---|
| `GET` | `/api/me` | Autenticado | Sync | Devuelve subject, email y roles del token actual |

### Admin

| Método | Ruta | Tipo | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/courses` | Sync | Lista todos los cursos |
| `GET` | `/api/admin/courses/{id}` | Sync | Obtiene un curso por ID |
| `POST` | `/api/admin/courses` | **Async** | Crea un curso y asigna profesor |
| `PUT` | `/api/admin/courses/{id}/teacher` | **Async** | Reasigna el profesor del curso |
| `DELETE` | `/api/admin/courses/{id}` | **Async** | Elimina un curso |
| `GET` | `/api/admin/courses/{id}/students` | Sync | Lista los alumnos inscritos |
| `POST` | `/api/admin/courses/{id}/enrollments` | **Async** | Inscribe a un alumno |
| `POST` | `/api/admin/reports/generate` | **Async** | Genera un reporte analítico |

### Teacher

| Método | Ruta | Tipo | Descripción |
|---|---|---|---|
| `GET` | `/api/teacher/courses` | Sync | Lista los cursos asignados al profesor |
| `GET` | `/api/teacher/courses/{id}/students` | Sync | Lista alumnos del curso |
| `POST` | `/api/teacher/courses/{id}/enrollments` | **Async** | Inscribe un alumno |
| `GET` | `/api/teacher/courses/{id}/tasks` | Sync | Lista tareas del curso |
| `POST` | `/api/teacher/courses/{id}/tasks` | **Async** | Crea una tarea |

### Student

| Método | Ruta | Tipo | Descripción |
|---|---|---|---|
| `GET` | `/api/student/courses` | Sync | Lista cursos inscritos |
| `GET` | `/api/student/courses/available` | Sync | Lista cursos disponibles |
| `POST` | `/api/student/courses/{id}/enroll` | **Async** | Auto-inscripción |
| `GET` | `/api/student/courses/{id}/tasks` | Sync | Tareas de un curso (si está inscrito) |
| `GET` | `/api/student/tasks` | Sync | Todas las tareas del alumno |

### Ejemplos

**Crear curso (ADMIN) — respuesta 202:**

```bash
curl -X POST https://<api-gateway>/api/admin/courses \
  -H "Authorization: Bearer $ID_TOKEN" \
  -H "X-Secret-Gateway: $GATEWAY_SECRET" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Desarrollo Cloud Native",
    "description": "DSY1107",
    "teacherId": "profesor@duoc.cl"
  }'
```

Respuesta:

```json
HTTP/1.1 202 Accepted
{ "message": "Course creation initiated" }
```

**Listar mis cursos (TEACHER) — respuesta 200:**

```bash
curl https://<api-gateway>/api/teacher/courses \
  -H "Authorization: Bearer $ID_TOKEN"
```

**Ver identidad actual:**

```bash
curl https://<api-gateway>/api/me \
  -H "Authorization: Bearer $ID_TOKEN"
```

---

## Autenticación y autorización

### Flujo de validación

1. **`SecretGatewayFilter`** (antes de Spring Security)
   Compara `X-Secret-Gateway` con `app.gateway.secret`. Si no coincide, responde **403**.

2. **`BearerTokenAuthenticationFilter`** (Spring Security)
   - Descarga el JWKS de Cognito desde `{issuer-uri}/.well-known/jwks.json`.
   - Verifica la firma RS256 del JWT.
   - Valida el claim `iss` (issuer) y `exp` (expiración).
   - Si algo falla, responde **401**.

3. **`CognitoJwtAuthenticationConverter`**
   Traduce el claim `cognito:groups` a authorities `ROLE_*`:
   - `["ADMIN"]` → `ROLE_ADMIN`
   - `["TEACHER"]` → `ROLE_TEACHER`
   - `["STUDENT"]` → `ROLE_STUDENT`

4. **`@PreAuthorize`** (controladores)
   Autoriza según el rol requerido. Si falta, responde **403**.

### Configuración en Cognito

En el **User Pool** de Cognito, crear tres grupos exactamente con estos nombres (mayúsculas):

- `ADMIN`
- `TEACHER`
- `STUDENT`

Los nombres son sensibles a mayúsculas: `Admin` no es igual a `ADMIN` en `hasRole('ADMIN')`.

### Claims usados por el backend

| Claim | Uso |
|---|---|
| `email` | Identidad del usuario (se usa como `teacherId` y `studentId`) |
| `sub` | Fallback si no hay `email` |
| `cognito:groups` | Roles (`ROLE_*`) |
| `iss` | Validado contra `issuer-uri` |
| `exp` | Validado automáticamente |

Nota importante: usar **`id_token`**, no `access_token`. Los access tokens de Cognito no incluyen el claim `email`.

---

## Base de datos

La persistencia vive en **Supabase (PostgreSQL)** y es gestionada por las **Lambdas consumidoras** del repositorio `CloudLambda`. El BFF **no** abre conexiones JDBC en este patrón: las lecturas se resuelven invocando Lambda (`AwsLambdaClient`) y las escrituras se resuelven publicando eventos en SNS que terminan en Lambdas que sí escriben en la base de datos.

### Esquema de tablas (gestionado por las Lambdas)

| Tabla | Columnas |
|---|---|
| `courses` | `id`, `name`, `description`, `teacher_id`, `created_at` |
| `enrollments` | `id`, `course_id`, `student_id`, `created_at` |
| `tasks` | `id`, `course_id`, `title`, `description`, `due_date`, `created_at` |

Los campos `teacher_id` y `student_id` almacenan el **email** del usuario de Cognito.

> Consultar el README de `CloudLambda` para los detalles de conexión JDBC, uso de sentencias parametrizadas (`$1`, `$2`) y validaciones de reglas de negocio en el consumidor.

---

## Despliegue

El despliegue del BFF está automatizado con **GitHub Actions** y se define en `.github/workflows/deploy.yml`.

### Flujo del pipeline

```
push a main
   |
   +--> JDK 21 + build con Gradle (bootJar)
   |
   +--> Build imagen Docker
   |
   +--> Push a GHCR (ghcr.io/gaboemi-duoc/cloudevaluacion:latest)
   |
   +--> SSH a EC2
         +-- Instala Docker si falta
         +-- Crea swap 2GB si falta
         +-- docker pull de la imagen
         +-- docker stop/rm del contenedor anterior
         +-- docker run con las env vars (incluye SNS_TOPIC_ARN)
```

### Secrets necesarios en GitHub

| Secret | Descripción |
|---|---|
| `EC2_SSH_KEY` | Llave `.pem` para SSH a EC2 |
| `GH_CR_PAT` | Personal Access Token de GitHub (para pull desde GHCR) |
| `API_GATEWAY_SECRET` | Secreto compartido con API Gateway |
| `COGNITO_ISSUER_URI` | Issuer URI del User Pool |
| `SNS_TOPIC_ARN` | ARN del topic SNS de eventos |
| `AWS_REGION` | Región de AWS |
| `AWS_ACCESS_KEY_ID` | Credencial de IAM (recomendado: usar rol IAM en EC2) |
| `AWS_SECRET_ACCESS_KEY` | Credencial de IAM (recomendado: usar rol IAM en EC2) |

### Variables de repositorio

| Variable | Descripción |
|---|---|
| `TARGET_HOST_IP` | IP pública o DNS de la instancia EC2 |

### Configuración de API Gateway

1. Crear un **HTTP API** en AWS API Gateway.
2. Configurar la **integración** apuntando a `http://<ec2-public-dns>:80`.
3. Crear una ruta **`ANY /api/{proxy+}`** con autorización **`None`** (el backend valida el JWT).
4. En **Request parameters > Add header**, mapear:
   - Header: `X-Secret-Gateway`
   - Value: `$stageVariables.gatewaySecret`
5. En **Stage variables**, definir `gatewaySecret` con el mismo valor que `API_GATEWAY_SECRET` en EC2.
6. Configurar **CORS**:
   - `Access-Control-Allow-Origin`: dominios del frontend (sin barra final)
   - `Access-Control-Allow-Methods`: `GET, POST, PUT, DELETE, OPTIONS`
   - `Access-Control-Allow-Headers`: `Authorization, Content-Type, X-Secret-Gateway`

### Puerto en EC2

El contenedor mapea el puerto **80 del host** al **8080 del contenedor**:

```bash
docker run -d \
  --name cloud-evaluacion \
  -p 80:8080 \
  -e PORT=8080 \
  -e SNS_TOPIC_ARN=$SNS_TOPIC_ARN \
  ...
```

El **Security Group** de la instancia EC2 debe permitir:

- **TCP 22** desde tu IP (SSH)
- **TCP 80** desde `0.0.0.0/0` (API Gateway) o desde el prefijo de IP de API Gateway

---

## Seguridad

### Capas de defensa

| Capa | Componente | Protege contra |
|---|---|---|
| 1 | API Gateway | Exposición directa de EC2, rate limiting |
| 2 | `SecretGatewayFilter` | Peticiones fuera de API Gateway |
| 3 | `BearerTokenAuthenticationFilter` | Tokens falsificados o expirados |
| 4 | `CognitoJwtAuthenticationConverter` | Escalada de privilegios por manipulación de roles |
| 5 | `@PreAuthorize` | Acceso no autorizado a endpoints |
| 6 | SNS + Filter Policy | Enrutamiento de eventos solo a consumidores interesados |

### Buenas prácticas implementadas

- **PKCE** en el flujo de login del frontend (Authorization Code + PKCE).
- **Validación de firma** con JWKS de Cognito, no con clave estática.
- **Sin `client_secret`** en el frontend (SPA pública).
- **Sin `open-in-view`** en Hibernate (evita fugas de conexiones).
- **`X-Secret-Gateway`** como control de origen para EC2.
- **TLS obligatorio** con Supabase (`?sslmode=require`) en el consumidor.
- **Separación de responsabilidades**: BFF = productor; Lambda = consumidor y acceso a datos.
- **Configuración centralizada** de recursos de mensajería vía variables de entorno y `application.properties`.
- **IAM Roles** recomendados sobre credenciales estáticas.

---

## Recursos

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [AWS Cognito Developer Guide](https://docs.aws.amazon.com/cognito/latest/developerguide/what-is-amazon-cognito.html)
- [AWS API Gateway HTTP APIs](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api.html)
- [AWS SNS Developer Guide](https://docs.aws.amazon.com/sns/latest/dg/welcome.html)
- [AWS SQS Developer Guide](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/welcome.html)
- [AWS Lambda Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/welcome.html)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Supabase Database Documentation](https://supabase.com/docs/guides/database)

---

## Autor

**Gabriel Emilio** - `gaboemi@duoc.cl`

Asignatura: **DSY1107 Desarrollo Cloud Native**
Evaluación Parcial N° 3 - 2026