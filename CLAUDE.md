# Instrucciones para trabajar en este repo

## Contexto

Este repo es el desafío profesional de backend de Digital House: la API REST de una
billetera virtual (Digital Money House). El detalle completo del proyecto, la arquitectura,
las decisiones tomadas y el estado actual están en `docs/CONTEXTO.md`. **Leelo antes de
responder cualquier cosa sobre el proyecto.**

## Quién soy y qué busco

Soy un desarrollador que sabe programar pero **hace bastante que no toca Java ni Spring**.
El objetivo de este proyecto no es que el código exista: es que yo recupere el oficio.
Si me resolvés todo, el proyecto se termina y yo no aprendo nada.

Sé razonar sobre arquitectura y lógica. Lo que no tengo fresco es:

- La sintaxis y los idiomas de Java moderno
- Las anotaciones de Spring y qué hace cada una
- Las herramientas del ecosistema (Maven, Lombok, JPA, los starters)
- Los atajos y el flujo de trabajo en Eclipse

## Cómo quiero que trabajes

**No edites archivos por tu cuenta.** No uses las herramientas de escritura salvo que yo
te lo pida explícitamente en ese mensaje. El código lo tipeo yo: es la parte que me hace
recuperar la memoria muscular.

**Dame el código completo, listo para copiar a mano.** Para una clase nueva: la clase
entera con sus imports, campos, anotaciones y la lógica de cada método. Nada de
esqueletos con `// TODO`. Yo lo tipeo leyéndolo de lo que me pasás, y esa es la parte
que me hace recuperar la memoria muscular.

**Explicá el porqué, no solo el qué.** Cada anotación, dependencia o herramienta nueva que
aparezca, explicámela en una o dos líneas: qué hace y por qué va ahí. No des por sabido
nada del ecosistema Spring.

**Revisá lo que escribo y señalá los problemas.** Cuando te muestre código, decime qué está
mal y **por qué**, sin reescribirlo entero. Si hay un error que Eclipse no marca pero va a
explotar en runtime, avisame — eso es lo más valioso que podés hacer.

**Una cosa a la vez.** Preferí un paso verificable ("hacé esto, corré la app, tenés que ver
X") antes que tres features juntas.

**Si dudás entre explicar de más o de menos, explicá de más.**

## Entorno

- **IDE: Eclipse** con Spring Tools 4. No sugieras atajos ni funciones de IntelliJ.
  Si mencionás un atajo, que sea el de Eclipse.
- **Java 21**, Maven, Windows.
- **Terminal: Git Bash.** Los comandos que me pases tienen que funcionar ahí.
- Base de datos: **H2 en modo archivo** temporalmente. MySQL en Docker queda pendiente
  porque la máquina de trabajo no tiene virtualización habilitada. El código no debe
  depender de cuál de las dos esté activa.

## Convenciones del código

- Nombres de campos y clases **en inglés** (`name`, `lastName`, `email`, `phoneNumber`).
- Estructura de paquetes: `com.dmh.<servicio>.{entity,repository,service,controller,dto,config,exception}`
- Un `pom.xml` independiente por microservicio. **No** proponer un proyecto multi-módulo
  con pom padre: los builds tienen que quedar independientes.
- **No agregar `spring-boot-starter-security`** todavía. Para hashear contraseñas alcanza
  con `spring-security-crypto`. El starter completo se suma recién cuando implementemos el JWT.
- DTOs separados de las entidades siempre. La entidad no se expone nunca por HTTP.

## Al cerrar una sesión de trabajo

Recordame actualizar `docs/CONTEXTO.md`: las casillas del estado actual y tres líneas en la
bitácora (qué hice, con qué me trabé, qué aprendí). Eso alimenta los informes de entrega y
las lecciones aprendidas que pide la certificación.
