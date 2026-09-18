# Digital Money House — Backend

Desafío profesional de la especialización Back-End de Digital House.
Documento vivo: se actualiza al cerrar cada sesión de trabajo.

**Última actualización:** 18/09/2026 (sesión 3)

---

## 1. Objetivo

Construir la **API REST de una billetera virtual** llamada Digital Money House (DMH).
Es el MVP para el lanzamiento del producto: el usuario se registra, inicia y cierra sesión,
da de alta tarjetas, carga saldo, transfiere dinero y consulta sus movimientos.

Cada usuario tiene una **CVU única** (Cuenta Virtual Uniforme) y un **alias**, y el sistema
lleva registro de todas las transacciones (ingresos y egresos).

El front-end lo provee Digital House ya hecho; el trabajo es exponer la API que lo alimenta.

---

## 2. Stack (fijado por la consigna, no es elección)

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework | Spring Boot 4.1.1 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | MySQL |
| Autenticación | JWT |
| Documentación de API | Swagger / OpenAPI |
| Arquitectura | Microservicios |
| Contenedores | Docker |
| Testing automatizado | JUnit (unitario) + RestAssured (integración/API) |
| Repositorio | GitHub (trabajo) → GitLab (entrega) |
| Deploy front | Vercel |
| Almacenamiento | Bucket S3 (AWS) |

---

## 3. Arquitectura objetivo

```
eureka-server        service discovery
api-gateway          única puerta de entrada, valida el JWT
users-service        registro, login, logout, datos del usuario
accounts-service     la cuenta: CVU, alias, saldo
cards-service        (Sprint 2) tarjetas asociadas
transactions-service (Sprint 3/4) transferencias y movimientos
MySQL en Docker
```

**Punto clave del diseño:** al registrarse un usuario nacen **dos entidades** — un `User`
y una `Account` con su CVU y alias. Viven en servicios distintos, así que `users-service`
tiene que pedirle a `accounts-service` que cree la cuenta (vía Feign). Ese es el primer
problema real de comunicación entre microservicios del proyecto.

**Estrategia de construcción:** no levantar los cinco servicios de entrada. Primero
`users-service` solo contra MySQL, después Eureka + Gateway, después separar
`accounts-service`. Cada paso verificable por separado.

### Capas dentro de cada servicio

```
Controller   recibe el HTTP, valida y delega. No piensa.
Service      la lógica de negocio
Repository   acceso a datos (Spring Data genera la implementación)
Entity       la tabla, como clase Java
DTO          lo que entra y sale por HTTP (≠ la entidad)
```

El DTO existe para que la forma de los datos internos y la forma de la API sean
independientes. Es lo que permite, por ejemplo, no devolver nunca la contraseña.

---

## 4. Entorno local

| Elemento | Valor |
|---|---|
| IDE | Eclipse + plugin Spring Tools 4 (Boot Dashboard) |
| Workspace de Eclipse | `C:\Users\Desarrollo\eclipse-ws-dmh` |
| Repositorio local | `C:\Users\Desarrollo\Desktop\ProyectoBackJava` |
| Remoto | GitHub (repo personal) |
| Instalado | Docker Desktop, MySQL, Git, Postman |

**El workspace está fuera del repo a propósito.** El workspace es el cuaderno de notas
del IDE (`.metadata`, preferencias); el repo es solo código. Mezclarlos ensucia el
historial de Git con configuración local.

Los proyectos se crean con **"Use default location" destildado**, apuntando la ruta
dentro del repo.

### Preparar otra máquina (checklist)

Todo esto se configura una vez por máquina; no viaja con el repo.

1. **JDK 21** (Temurin): `winget install EclipseAdoptium.Temurin.21.JDK`.
   `JAVA_HOME` apuntando a la carpeta del JDK (sin `in`) y ese `in` primero en el `Path`.
   Verificar en una terminal **nueva**: `java -version` → 21. Las terminales/apps abiertas
   antes del cambio siguen viendo los valores viejos: hay que cerrarlas y reabrirlas.
2. **Lombok en Eclipse.** Maven lo usa solo, pero el compilador de Eclipse no: sin esto
   aparecen errores tipo *"The method getEmail() is undefined"*. Con Eclipse cerrado:
   `java -jar ~/.m2/repository/org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar`
   → elegir el `eclipse.exe` → Install/Update → abrir Eclipse → Project → Clean.
   (El jar aparece en `~/.m2` después del primer build de Maven: `./mvnw compile`.)
3. **Importar el proyecto:** File → Import → Maven → Existing Maven Projects → carpeta
   `users-service`. Después **Alt+F5** (Maven → Update Project).
4. **X rojas en el `pom.xml`** de "Language Servers" (*cvc-elt.1.a* / *Downloading external
   resources is disabled*): no son errores de Maven. Window → Preferences → XML (Wild Web
   Developer) → tildar "Download external resources…". Alternativa: Validation & Resolution
   → schema based validation = `Never`. No tildar "Allow resolution of external entities".
5. **Base H2:** vive en `users-service/data/` y está en `.gitignore`, así que en otra máquina
   arranca vacía. Hibernate crea la tabla sola al levantar la app.
6. Correr: Boot Dashboard → `users-service` → (Re)start. Tiene que terminar en
   `Started UsersServiceApplication` y `Tomcat started on port 8081`.

---

## 5. Estado actual

### Hecho

- [x] Eclipse + Spring Tools 4 instalados
- [x] Repo creado en GitHub y clonado localmente
- [x] Workspace de Eclipse separado del repo
- [x] Proyecto `users-service` creado (Maven, Java 21, `com.dmh.users`)
- [x] Dependencias: Spring Web, Spring Data JPA, MySQL Driver, Lombok, Validation
- [x] `.gitignore` en la raíz del repo
- [x] Primer commit pusheado
- [x] `application.properties` configurado (H2 en archivo por ahora + Hibernate)
- [x] `docker-compose.yml` subido al repo
- [x] Entidad `User` (tabla `users`, `dni` y `email` únicos)
- [x] Interfaz `UserRepository` (`findByEmail`, `existsByEmail`, `existsByDni`)
- [x] DTOs `RegisterUserRequest` (con validaciones) y `UserResponse` (sin contraseña, con CVU y alias) en `com.dmh.users.dto`
- [x] `@Size(max)` del DTO alineados con los `length` de la entidad (nombre y apellido 100, email 254, contraseña 8–72)
- [x] La app arranca y Hibernate crea la tabla `users`
- [x] `spring-security-crypto` + `SecurityConfig` con el bean `PasswordEncoder` (BCrypt)
- [x] `UserAlreadyExistsException` (unchecked, en `exception`)
- [x] `UserService.register`: normaliza email, chequea email/DNI duplicados, hashea la contraseña, guarda y devuelve `UserResponse` (`cvu`/`alias` en `null` por ahora)
- [x] `UserController` → `POST /users` con `@Valid`, responde 201

### En curso — Sprint 1

- [ ] **Probar `POST /users` con Postman** (ver "Próximo paso" abajo)
- [ ] **Manejo de errores** con `@RestControllerAdvice`

### Próximo paso concreto

**1. Probar el registro.** POST `http://localhost:8081/users`, Body → raw → JSON:

```json
{
  "name": "Federico",
  "lastName": "Vazquez",
  "dni": "12345678",
  "email": "Fede@Mail.com",
  "phoneNumber": "1144445555",
  "password": "clave1234"
}
```

| Prueba | Esperado hoy |
|---|---|
| Mandarlo tal cual | 201, email en minúsculas, `cvu`/`alias` en `null`, sin `password` |
| Mandarlo otra vez | 500 (la `UserAlreadyExistsException` todavía no se traduce) |
| `"dni": "123"` | 400 genérico de Spring, sin el mensaje propio |

**2. `GlobalExceptionHandler`** en `com.dmh.users.exception`, con `@RestControllerAdvice`:

- `MethodArgumentNotValidException` (falla de `@Valid`) → 400 con el mensaje de cada campo
- `UserAlreadyExistsException` → 400 (la consigna solo admite 400/500/201 en el registro)
- `DataIntegrityViolationException` (dos registros simultáneos que pasan el `exists` y
  choca el `unique` de la base) → 400
- `Exception` genérica → 500 con un mensaje neutro, sin stack trace

Después de eso, repetir las tres pruebas: 201, 400 con mensaje, 400 con mensaje.

### Después

- [ ] Generación de CVU (22 dígitos) y alias (3 palabras desde un TXT)
- [ ] Login (JWT) y logout
- [ ] Pasar de H2 a MySQL (`dmh_users`)
- [ ] Eureka + Gateway, y separar `accounts-service` (CVU/alias viven ahí, se piden por Feign)

---

## 6. Sprint 1 — requisitos

### Épica: Inicio, registro y acceso

| Endpoint | Entrada | Salida | Errores |
|---|---|---|---|
| Registro | nombre+apellido, DNI, email, teléfono, contraseña | JSON con todos los campos **sin la contraseña**, + CVU y alias | 400, 500, 201 |
| Login | email, contraseña | JSON con el token | 404 usuario inexistente, 400 contraseña incorrecta, 500 |
| Logout | token en el header | — | 200, 500 |

- **CVU:** 22 dígitos numéricos, autogenerado y aleatorio
- **Alias:** 3 palabras separadas por punto, elegidas al azar de un archivo TXT
- El token debe persistir al recargar la página (no desloguear)

### Épica: Testing & calidad

- Plan de pruebas: cómo escribir un caso, cómo reportar un defecto, criterio para
  suite de humo y criterio para suite de regresión
- Testing exploratorio con documento de notas y organización (sesiones, tours,
  escenarios, workflows)
- Planilla de casos de prueba de las funcionalidades del sprint
- Clasificar casos en smoke y regression; ejecutar la suite
- Testing unitario y tests de integración sobre el código
- Subir la planilla a GitLab

### Épica: Infraestructura

- Git, Docker, funcionamiento en microservicios
- Diseño de la infraestructura necesaria
- Boceto de la red y sus componentes (servidores, almacenamiento, red interna, BD)

### Opcionales

- Recuperación de contraseña por mail (link → pantalla de nueva contraseña, repetida)
- Validación de email con código de 6 dígitos en el primer login

---

## 7. Los cuatro sprints

| Sprint | Alcance |
|---|---|
| 1 | Registro, inicio y cierre de sesión |
| 2 | Mi Perfil, registro de tarjetas, ingreso de dinero |
| 3 | Transferir dinero y Dashboard (saldo, alias, CVU, últimos movimientos) |
| 4 | Actividad del usuario: listado de movimientos con filtros |

Desde el Sprint 2 en adelante, **todo endpoint exige el token en el header**.

---

## 8. Entrega final

Repositorio de GitLab con el código, el link y el esbozo de la infraestructura.
Más un **documento de proyecto** con:

- Objetivos del proyecto
- Planificación y descripción de actividades (backlog) con plazos estimados
- Informes de entrega
- Informes de retro personal
- Lecciones aprendidas

---

## 9. Decisiones tomadas y por qué

| Decisión | Razón |
|---|---|
| Java 21 en vez de 25 | Es la LTS que todo el ecosistema Spring/Hibernate/RestAssured soporta sin sorpresas |
| Monorepo, no un repo por servicio | La consigna pide *un* repositorio; sincronizar cinco repos a mano es innecesario |
| Workspace de Eclipse fuera del repo | Evita que `.metadata` y la configuración local ensucien el historial |
| Sin Spring Security al inicio | Agregada de entrada bloquea todos los endpoints con un login automático y confunde el debug. Se suma al implementar el JWT |
| `ddl-auto=update` | Hibernate crea las tablas solo mientras se desarrolla. **Solo para desarrollo** — en producción nunca |
| H2 en archivo antes que MySQL | Permite avanzar con entidad/repositorio sin depender de tener MySQL levantado. La config de MySQL queda comentada en `application.properties` para el cambio |
| Todo el código bajo `com.dmh.users` | Es el paquete de `UsersServiceApplication`; lo que quede fuera no lo encuentra el component scan |
| `spring-security-crypto` y no el starter de Security | Trae solo BCrypt. El starter completo bloquea todos los endpoints; se suma con el JWT |
| Duplicados de email/DNI → 400 | La consigna solo lista 400, 500 y 201 para el registro |
| Email normalizado (`trim` + minúsculas) antes de validar y guardar | El `unique` de la base compara texto exacto: `Juan@Mail.com` y `juan@mail.com` serían dos usuarios |
| Inyección por constructor (`@RequiredArgsConstructor` + campos `final`) | Dependencias explícitas e inmutables; se puede instanciar en tests sin levantar Spring |
| Límites de texto definidos en la entidad y repetidos en el DTO | Si solo están en la base, un dato largo da 500; con `@Size` da 400 con mensaje |
| `show-sql=true` mientras se aprende | Ver el SQL que genera el ORM es la mejor forma de entender qué hace por detrás |
| GitHub para trabajar, GitLab para entregar | Git maneja varios remotos: `git remote add gitlab <url>` y `git push gitlab main` al momento de la entrega |

---

## 10. Bitácora

> Tres líneas al cerrar cada sesión. Esto alimenta los "informes de entrega",
> la "retro personal" y las "lecciones aprendidas" del documento final.

### 15/09/2026 — Sesión 1

- **Hecho:** entorno armado (Eclipse + Spring Tools 4), repo en GitHub, proyecto
  `users-service` creado con sus dependencias, primer commit pusheado.
- **Trabas:** confusión inicial entre el workspace de Eclipse y el repositorio Git.
  Se resolvió separándolos físicamente.
- **Aprendido:** los plugins del Marketplace se instalan en la instalación de Eclipse,
  no en el workspace — sobreviven a cambiar de workspace o borrar proyectos.

### 16–17/09/2026 — Sesión 2

- **Hecho:** `docker-compose.yml`, entidad `User`, `UserRepository` y los primeros DTOs
  (`RegisterUserRequest`, `UserResponse`). Se usa H2 en archivo mientras no esté MySQL.
- **Trabas:** los DTOs quedaron en `com.dmh.user.dto` (sin "s"); se movieron a `com.dmh.users.dto`.
- **Aprendido:** las validaciones del DTO tienen que acompañar las restricciones de la
  entidad; si no, un dato inválido llega a la base y devuelve 500 en lugar de 400.

### 18/09/2026 — Sesión 3

- **Hecho:** límites de tamaño alineados entre DTO y entidad, `SecurityConfig` con BCrypt,
  `UserAlreadyExistsException`, `UserService.register` y `UserController` (`POST /users`).
  Se agregó `CLAUDE.md` con las reglas para trabajar con Claude.
- **Trabas:** Lombok no estaba instalado en Eclipse (Maven compilaba, Eclipse no veía los
  getters); `JAVA_HOME` que no se actualizaba en terminales ya abiertas; falsos errores del
  validador XML en el `pom.xml`; `application.properties` guardado en Windows-1252 rompía Maven.
- **Aprendido:** `@Bean` usa el nombre del método como nombre del bean (va en camelCase);
  sin `@Valid` las validaciones del DTO no corren; Eclipse deja ejecutar con errores de
  compilación, así que hay que mirar la vista Problems antes de dar algo por andando.
