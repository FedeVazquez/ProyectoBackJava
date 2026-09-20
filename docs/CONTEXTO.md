# Digital Money House — Backend

Desafío profesional de la especialización Back-End de Digital House.
Documento vivo: se actualiza al cerrar cada sesión de trabajo.

**Última actualización:** 19/09/2026 (sesión 5)

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
| Segunda PC | Repo en `D:\ProyectoBackJava`, Eclipse en `D:\eclipse\eclipse` |

**El workspace está fuera del repo a propósito.** El workspace es el cuaderno de notas
del IDE (`.metadata`, preferencias); el repo es solo código. Mezclarlos ensucia el
historial de Git con configuración local.

Los proyectos se crean con **"Use default location" destildado**, apuntando la ruta
dentro del repo.

### Preparar otra máquina (checklist)

Todo esto se configura una vez por máquina; no viaja con el repo.

1. **JDK 21** (Temurin), en Git Bash:
   `winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-package-agreements --accept-source-agreements`.
   Los flags `--accept-*` son necesarios: en Git Bash (MinTTY) winget no recibe el `Y` del
   prompt interactivo y parece colgado. Alternativa: correr el mismo comando en PowerShell/cmd.
   `JAVA_HOME` apuntando a la carpeta del JDK (sin `bin`) y ese `bin` primero en el `Path`.
   Verificar en una terminal **nueva**: `java -version` → 21. Las terminales/apps abiertas
   antes del cambio siguen viendo los valores viejos: hay que cerrarlas y reabrirlas.
   - **Registrarlo en Eclipse** (no lo toma solo): Window → Preferences → Java → Installed JREs
     → Add → Standard VM → carpeta del JDK 21. Después, en **Execution Environments** →
     `JavaSE-21` → tildar ese JDK. Si no, `JavaSE-21` se resuelve contra cualquier otro JDK de
     la máquina (pasó con el 25) y el log dice *"using Java 25"* aunque el proyecto pida 21.
     Verificar en el arranque: `Starting UsersServiceApplication using Java 21...`.
   - Los comandos de este documento son sintaxis **Git Bash** (`~`, `$VAR`). En PowerShell la
     ruta al home es `$env:USERPROFILE`; pegar esa sintaxis en Git Bash da *"Unable to access
     jarfile :USERPROFILE..."*.
2. **Lombok en Eclipse.** Maven lo usa solo, pero el compilador de Eclipse no: sin esto
   aparecen errores tipo *"The method getEmail() is undefined"* y *"The blank final field
   ... may not have been initialized"* (esto último es `@RequiredArgsConstructor` sin procesar).
   Con Eclipse cerrado:
   `java -jar ~/.m2/repository/org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar`
   → elegir el `eclipse.exe` (lo detecta solo; si no, "Specify location") → Install/Update →
   abrir Eclipse → Project → Clean. Resultado esperado: Problems en 0 errores.
   (El jar aparece en `~/.m2` después del primer build de Maven: `./mvnw compile`.)
3. **Importar el proyecto:** File → Import → Maven → Existing Maven Projects → carpeta
   `users-service`. Después **Alt+F5** (Maven → Update Project).
   Para abrir Properties: un solo clic sobre el nodo raíz del proyecto → clic derecho →
   Properties (o **Alt+Enter**).
4. **X rojas en el `pom.xml`** de "Language Servers" (*cvc-elt.1.a* / *Downloading external
   resources is disabled*): no son errores de Maven. Window → Preferences → XML (Wild Web
   Developer) → tildar "Download external resources…". Alternativa: Validation & Resolution
   → schema based validation = `Never`. No tildar "Allow resolution of external entities".
5. **Base H2:** vive en `users-service/data/` y está en `.gitignore`, así que en otra máquina
   arranca vacía. Hibernate crea la tabla sola al levantar la app.
6. **Spring Tools 4** (da el Boot Dashboard): Help → Eclipse Marketplace → buscar "Spring Tools 4"
   → Install → reiniciar Eclipse. Sin el plugin, Window → Show View → Other → "boot" no muestra
   nada. Es una vista, se abre en Window → Show View → Other → Spring → Boot Dashboard.
7. Correr: Boot Dashboard → `users-service` → (Re)start. Sin el plugin también anda: clic derecho
   sobre `UsersServiceApplication` → Run As → Java Application. Tiene que terminar en
   `Started UsersServiceApplication` y `Tomcat started on port 8081`.
8. **Autocompletado:** Ctrl+Space siempre funciona. Para que aparezca solo al escribir:
   Window → Preferences → Java → Editor → Content Assist → "Enable auto activation" y, en
   "Auto activation triggers for Java", agregar las letras (`.abcdefghijklmnopqrstuvwxyz`).

### Problemas conocidos

| Síntoma | Causa | Solución |
|---|---|---|
| `getX()` / `setX()` "undefined", "blank final field" | Lombok no instalado en Eclipse | Paso 2 |
| Log dice `using Java 25` pero el proyecto pide 21 | `JavaSE-21` sin JDK 21 asociado en Eclipse | Paso 1 (registrar el JDK) |
| `winget` no reacciona al `Y` en Git Bash | MinTTY no maneja el prompt de winget | Flags `--accept-*` o PowerShell |
| 400 genérico (`timestamp/status/error/path`) sin detalle | Falló una validación del DTO (ej. DNI de 9 dígitos con `\d{7,8}`) o el body llegó vacío/roto | Con `GlobalExceptionHandler` ya devuelve `errors` por campo |
| Warning *Build path entry is missing: src/test/resources* | La carpeta no existe todavía | Crearla al armar los tests de integración |
| Se agrega una columna `not null` y el `alter table` falla en el log; el siguiente POST da 500 | `ddl-auto=update` no puede agregar una columna obligatoria a una tabla que ya tiene filas | Parar la app, borrar `users-service/data/` (está en `.gitignore`) y volver a arrancar: Hibernate recrea la tabla |
| Un campo nuevo se guarda en la base pero vuelve `null` por HTTP | El mapeo a DTO no se actualizó (ej. `toResponse` con `null` hardcodeado) | Revisar el mapeo entidad → DTO, no la entidad |

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
- [x] `UserService.register`: normaliza email, chequea email/DNI duplicados, hashea la contraseña, guarda y devuelve `UserResponse`
- [x] `UserController` → `POST /users` con `@Valid`, responde 201
- [x] `POST /users` probado en Postman: 201, duplicado, DNI inválido (todo verificado)
- [x] `ErrorResponse` (record: `status`, `message`, `errors`) y `GlobalExceptionHandler` con `@RestControllerAdvice`
- [x] Entorno de la segunda PC armado: Lombok en Eclipse, JDK 21 registrado, autocompletado
- [x] Campos `cvu` y `alias` en `User` (`not null` + `unique`), con `existsByCvu`/`existsByAlias` en el repositorio
- [x] `aliases.txt` (60 palabras, sin acentos ni ñ) en `src/main/resources`
- [x] `AccountDataGenerator` (`@Component`): CVU de 22 dígitos con `SecureRandom`; alias de 3 palabras distintas (`shuffle` + `subList`); el TXT se lee una sola vez al arrancar
- [x] `UserService.register` asigna CVU y alias únicos (genera, chequea contra la base y reintenta, con tope de 10)
- [x] `POST /users` devuelve `cvu` de 22 dígitos y `alias` de 3 palabras (verificado en Postman)
- [x] Tests unitarios en verde: `AccountDataGeneratorTest` (3) y `UserServiceTest` con Mockito (3)

### En curso — Sprint 1

- [ ] **Login (JWT) y logout** (ver "Próximo paso" abajo)

### Próximo paso concreto

**Login y logout con JWT.** Es el paso donde recién se suma `spring-boot-starter-security`
(hasta ahora solo estaba `spring-security-crypto` para BCrypt) más una librería de JWT.
Conviene partirlo en pasos chicos y verificables, no de una sola vez.

Requisitos de la consigna:

| Endpoint | Entrada | Salida | Errores |
|---|---|---|---|
| Login | email, contraseña | JSON con el token | 404 usuario inexistente, 400 contraseña incorrecta, 500 |
| Logout | token en el header | — | 200, 500 |

- La contraseña se verifica con `passwordEncoder.matches(plana, hash)`; nunca se compara texto plano
- El token tiene que sobrevivir a un refresh de la página (no desloguear)
- Al sumar el starter de Security, `POST /users` queda bloqueado si no se abre explícitamente
  en `SecurityConfig`: es el primer síntoma esperable

### Contrato de errores del registro (ya implementado)

Todos los errores devuelven `{ "status", "message", "errors" }`:

| Caso | Status | Detalle |
|---|---|---|
| Falla `@Valid` | 400 | `errors` = `campo → mensaje` (todos los campos inválidos juntos) |
| Body vacío o JSON roto | 400 | `HttpMessageNotReadableException` |
| Email o DNI duplicado | 400 | `UserAlreadyExistsException` (o `DataIntegrityViolationException` en la carrera) |
| Cualquier otra excepción | 500 | Mensaje neutro; el stack trace queda solo en el log |

Limitación conocida: el catch-all `Exception` convierte también un 405 (método no permitido) o
un 415 (Content-Type incorrecto) en 500. Se corrige más adelante extendiendo
`ResponseEntityExceptionHandler`.

### Después

- [ ] Tests de integración con RestAssured sobre `POST /users` (hoy los tests son solo unitarios)
- [ ] Pasar de H2 a MySQL (`dmh_users`)
- [ ] Eureka + Gateway, y separar `accounts-service` (CVU/alias viven ahí, se piden por Feign)

### Límites conocidos de CVU y alias

- Las 60 palabras de `aliases.txt` dan unas 200 mil combinaciones de 3. Alcanza para el MVP;
  si hiciera falta más, se agregan palabras al TXT.
- Si dos registros simultáneos generan el mismo alias, los dos pasan el `existsByAlias` y el
  segundo choca contra el `unique`. Cae en el handler de `DataIntegrityViolationException`, que
  responde "El email o el DNI ya están registrados": mensaje engañoso para ese caso. Muy
  improbable, no se resuelve por ahora.

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
| `ErrorResponse` como `record` con `status`, `message` y `errors` | Un solo formato de error para toda la API; el record es inmutable y no necesita Lombok |
| `errors` como mapa `campo → mensaje`, con `toMap` y función de merge | Un campo con dos violaciones repite la clave y `toMap` sin merge lanza `IllegalStateException` |
| Handler propio de `HttpMessageNotReadableException` | Con un catch-all `Exception`, un body vacío o un JSON roto pasaría de 400 a 500 |
| Excepciones inesperadas: mensaje neutro al cliente, stack trace al log (`@Slf4j`) | No filtrar detalles internos por HTTP y conservar la traza para depurar |
| CVU y alias generados en `users-service` por ahora | Su lugar definitivo es `accounts-service`, pero ese servicio todavía no existe. Se mueven al separarlo |
| CVU como `String` y no `Long` | 22 dígitos no entran en un `Long` y así se conservan los ceros a la izquierda |
| `SecureRandom` en vez de `Random` | `Random` es predecible conociendo su semilla; un CVU adivinable no es aceptable en una billetera |
| El alias se arma con `shuffle` + `subList(0, 3)` | Garantiza 3 palabras **distintas**; con tres sorteos sueltos podría salir `sol.sol.sol` |
| `aliases.txt` se lee una sola vez en el constructor | Si falta o está vacío, la app falla al arrancar y no en el primer registro (fail-fast) |
| `ClassPathResource` en vez de una ruta de disco | Funciona igual desde Eclipse que desde el `.jar` empaquetado |
| Generar, chequear con `existsBy...` y reintentar (tope de 10) | La constraint `unique` sola daría un error feo; el tope evita un bucle infinito si el espacio se agota |
| Tests unitarios con Mockito, sin levantar Spring | Corren en milisegundos y prueban la lógica del service aislada de la base |
| `ArgumentCaptor` para inspeccionar lo que se guarda | Es la única forma de verificar el email normalizado y la contraseña hasheada, que no salen en la respuesta |

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

### 18/09/2026 — Sesión 4

- **Hecho:** entorno de la segunda PC armado (Lombok, JDK 21 registrado en Eclipse, autocompletado);
  `POST /users` probado en Postman; `ErrorResponse` y `GlobalExceptionHandler` (400 con detalle por
  campo, 400 para duplicados y body roto, 500 neutro). Se documentaron los "Problemas conocidos".
- **Trabas:** `winget` colgado en Git Bash (no recibe el `Y`); sintaxis de PowerShell pegada en Git
  Bash (`$env:USERPROFILE`); el proyecto corría con Java 25 porque `JavaSE-21` no tenía un JDK 21
  asociado; un 400 "misterioso" que era un DNI de 9 dígitos contra `\d{7,8}`; errores de tipeo
  (`getFieldError()` vs `getFieldErrors()`) y un método helper faltante.
- **Aprendido:** un execution environment de Eclipse es una especificación, no un JDK; sin `@ControllerAdvice`
  el mensaje propio de una validación nunca llega al cliente; un catch-all `Exception` también atrapa
  errores que Spring maneja bien (405/415) y los vuelve 500; guardar con Ctrl+S antes de reiniciar.

### 19/09/2026 — Sesión 5

- **Hecho:** generación de CVU y alias completa — campos en `User`, `aliases.txt`, `AccountDataGenerator`
  con `SecureRandom`, y `UserService.register` asignando valores únicos con reintento. Primeros tests
  unitarios del proyecto: `AccountDataGeneratorTest` y `UserServiceTest` con Mockito, 6 en verde.
- **Trabas:** varios errores de tipeo que Eclipse marcó con mensajes poco obvios (`Build` por `build`,
  `IOEception`, un método a medio escribir que daba *"Return type for the method is missing"*); y un
  `cvu: null` en la respuesta aunque el dato estaba en la base, porque `toResponse` seguía con los
  `null` hardcodeados del paso anterior.
- **Aprendido:** un método sin tipo de retorno solo es válido si es un constructor con el nombre de la
  clase; `Collections` (utilidades) no es `Collection` (interfaz); `@Mock` devuelve `false`/`null` por
  defecto, así que solo hay que stubbear lo que el código realmente usa (si no, Mockito falla por
  *stubbing* innecesario); y cuando un dato se guarda bien pero vuelve `null`, el problema está en el
  mapeo a DTO, no en la entidad.
