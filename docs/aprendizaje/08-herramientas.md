# 08 — Herramientas del ecosistema

## Maven

**En una frase:** Maven maneja las dependencias y el ciclo de build del proyecto, a partir de un
archivo `pom.xml`.

**En este proyecto:** un `pom.xml` por microservicio, sin pom padre, para que cada servicio se
construya por su cuenta.

**Cómo lo cuento en 30 segundos:** "El `pom.xml` declara dependencias y plugins. Maven las baja
al repositorio local `~/.m2` y ejecuta el ciclo de vida: compile, test, package. Al heredar del
`spring-boot-starter-parent`, las versiones de las librerías de Spring vienen fijadas y
compatibles entre sí."

**Repreguntas típicas:**
- *¿Por qué las dependencias no llevan `<version>`?* Porque el parent trae un BOM (*bill of
  materials*) con versiones ya probadas juntas. Fijarlas a mano es la forma más fácil de armar
  una combinación incompatible.
- *¿Qué son los scopes?* `compile` (por defecto), `runtime` (solo al ejecutar, como el driver
  de MySQL), `test` (solo en tests), `provided` (lo aporta el entorno).
- *¿Qué es un starter?* Una dependencia que no trae código propio: agrupa librerías que se usan
  juntas y activa la autoconfiguración de Spring Boot para ellas.
- *Comandos que uso:* `./mvnw compile`, `./mvnw test`, `./mvnw clean package`. El `mvnw` es el
  wrapper: baja la versión correcta de Maven, así no depende de lo que tenga instalado la máquina.

---

## Lombok

**En una frase:** genera getters, setters y constructores en tiempo de compilación, a partir de
anotaciones.

**En este proyecto:** `@Getter`, `@Setter`, `@NoArgsConstructor` y `@AllArgsConstructor` en la
entidad y los DTOs; `@RequiredArgsConstructor` en service y controller; `@Slf4j` en el handler
de errores.

**Cómo lo cuento en 30 segundos:** "Lombok evita el código repetitivo de getters, setters y
constructores. Es un procesador de anotaciones: el código se genera al compilar, no en runtime,
así que el `.class` queda igual que si lo hubiera escrito a mano."

**Repreguntas típicas:**
- *¿Qué hace `@RequiredArgsConstructor`?* Genera un constructor con todos los campos `final`. Es
  lo que permite la inyección por constructor sin escribirlo.
- *¿Contras?* Hay que instalarlo en el IDE, el código generado no se ve al leer la clase, y
  `@Data` en entidades JPA es peligroso: genera `equals`/`hashCode` con todos los campos y
  `toString` que puede disparar carga de relaciones.

**Dónde me trabé:** Maven compilaba pero Eclipse marcaba `getEmail()` como inexistente. Eclipse
usa su propio compilador y necesita que Lombok se instale aparte (`java -jar lombok.jar`), que
agrega un `-javaagent` al `eclipse.ini`. Mismo código, dos compiladores, dos resultados.

---

## JDK, JRE y versiones

**En una frase:** el JRE ejecuta, el JDK además compila; la versión importa porque un `.class`
compilado con una versión nueva no corre en una vieja.

**Cómo lo cuento en 30 segundos:** "El proyecto usa Java 21, que es LTS. Es la versión que todo
el ecosistema de Spring soporta sin sorpresas; las versiones intermedias tienen soporte de seis
meses y no valen la pena para un proyecto que hay que mantener."

**Repreguntas típicas:**
- *¿Qué es `JAVA_HOME`?* La variable de entorno que le dice a Maven y a otras herramientas qué
  JDK usar. No la lee el `java` del `Path`: son dos cosas distintas y pueden apuntar a versiones
  diferentes.
- *¿Qué significa `class file has wrong version 61.0, should be 52.0`?* Que una librería fue
  compilada para Java 17 (61) y se está ejecutando con Java 8 (52).

**Dónde me trabé:** cambiar `JAVA_HOME` no afecta a las terminales ni a las aplicaciones ya
abiertas: las variables de entorno se leen al arrancar el proceso. Hay que cerrar y volver a
abrir. Y en Eclipse, el *execution environment* `JavaSE-21` es una especificación, no un JDK:
si no se le asocia el JDK 21, resuelve contra cualquier otro instalado.

---

## Git

**En una frase:** control de versiones distribuido; cada copia del repo tiene la historia completa.

**En este proyecto:** monorepo con los cinco servicios, GitHub para trabajar y GitLab para la
entrega final.

**Cómo lo cuento en 30 segundos:** "Trabajo con commits chicos y mensajes que digan qué cambió.
Git soporta varios remotos, así que el mismo repo puede publicarse en GitHub y en GitLab sin
copiar nada a mano."

**Repreguntas típicas:**
- *¿Qué va en `.gitignore`?* Lo generado y lo local: `target/`, la base H2 de `data/`, la
  configuración del IDE. Regla práctica: si se regenera solo o depende de la máquina, no va.
- *¿Monorepo o un repo por servicio?* Un repo por servicio da despliegues independientes, pero
  obliga a coordinar cambios entre repos. Para este proyecto, que es de una sola persona y la
  consigna pide un repositorio, el monorepo es lo razonable.

---

## Docker (pendiente en este proyecto)

**En una frase:** empaqueta la aplicación con todo lo que necesita para correr, de modo que
funcione igual en cualquier máquina.

**Estado:** hay un `docker-compose.yml` para MySQL, pero la máquina de trabajo no tiene la
virtualización habilitada, así que por ahora la base es H2 en archivo. El código no depende de
cuál esté activa: solo cambia `application.properties`.

**Cómo lo cuento en 30 segundos:** "La idea es levantar MySQL y los servicios con
docker-compose, para que el entorno sea reproducible y no dependa de lo que cada uno tenga
instalado. Mientras tanto uso H2, que habla SQL estándar y me deja avanzar."

**Repreguntas típicas:**
- *¿Imagen y contenedor?* La imagen es la plantilla inmutable; el contenedor, una instancia en
  ejecución.
- *¿Y los datos?* Un contenedor es efímero: para que la base sobreviva a recrearlo hay que
  montar un volumen.
