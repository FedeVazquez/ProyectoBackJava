# Digital Money House — Backend

Desafío profesional de la especialización Back-End de Digital House.
Documento vivo: se actualiza al cerrar cada sesión de trabajo.

**Última actualización:** 15/09/2026

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
| Framework | Spring Boot |
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

### En curso — Sprint 1, sesión 1

- [ ] Crear base `dmh_users` en MySQL
- [ ] Configurar `application.properties` (datasource + Hibernate)
- [ ] Entidad `User`
- [ ] Interfaz `UserRepository`
- [ ] Verificar que la app arranca y Hibernate crea la tabla

### Próximo

- [ ] DTO de registro con validaciones (`@Valid`)
- [ ] `UserService` con la lógica
- [ ] `UserController` → `POST /users`
- [ ] Generación de CVU (22 dígitos) y alias (3 palabras desde un TXT)
- [ ] Probar con Postman

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
