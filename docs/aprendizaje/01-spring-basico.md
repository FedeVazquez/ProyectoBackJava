# 01 — Spring básico

## Qué es Spring y qué es Spring Boot

**En una frase:** Spring es un framework que administra los objetos de la aplicación y las
conexiones entre ellos; Spring Boot es Spring con todo preconfigurado y un servidor adentro.

**Qué problema resuelve:** antes había que escribir cientos de líneas de XML para decir qué
objeto usaba a cuál, y desplegar un `.war` en un servidor aparte (Tomcat, JBoss). Con Boot,
`main()` levanta la aplicación y el servidor ya viene incluido.

**En este proyecto:** `UsersServiceApplication` con `@SpringBootApplication` arranca todo; en
el log se ve `Tomcat started on port 8081`.

**Cómo lo cuento en 30 segundos:** "Spring Boot es Spring con convenciones por defecto: trae
un servidor embebido, autoconfigura lo que encuentra en el classpath y arranca con un `main`.
Uno escribe la lógica y la configuración aparece solo cuando hace falta salirse de la convención."

**Repreguntas típicas:**
- *¿Qué hace `@SpringBootApplication`?* Son tres en una: `@Configuration` (la clase puede
  declarar beans), `@EnableAutoConfiguration` (activa la autoconfiguración) y `@ComponentScan`
  (busca componentes desde el paquete de esa clase hacia abajo).
- *¿Qué es la autoconfiguración?* Spring Boot mira qué librerías hay en el classpath y arma la
  configuración típica para ellas. Si aparece el driver de una base, arma el `DataSource`; si
  aparece Spring Security, protege todos los endpoints.

---

## Bean y contenedor

**En una frase:** un bean es un objeto que crea y administra Spring, no vos con `new`.

**Qué problema resuelve:** evita que cada clase tenga que saber cómo construir lo que necesita,
y garantiza que haya una sola instancia compartida de las cosas caras (conexiones, encoders).

**En este proyecto:** `SecurityConfig.passwordEncoder()` está anotado con `@Bean`, así que el
`BCryptPasswordEncoder` que devuelve lo administra Spring. `UserService` no lo crea: lo recibe.

**Cómo lo cuento en 30 segundos:** "El contenedor de Spring es un registro de objetos. Al
arrancar, crea los beans, resuelve qué necesita cada uno y los conecta. Por defecto son
singleton: una instancia por aplicación."

**Repreguntas típicas:**
- *¿Cómo se declara un bean?* Con un estereotipo sobre la clase (`@Component`, `@Service`,
  `@Repository`, `@Controller`) o con un método `@Bean` dentro de una clase `@Configuration`.
  Lo segundo sirve para objetos de librerías, cuyo código no puedo anotar.
- *¿`@Component` y `@Service` son distintos?* Técnicamente hacen lo mismo. El nombre comunica
  la intención: `@Service` dice "acá vive la lógica de negocio".

**Dónde me trabé:** en un `@Bean`, **el nombre del método es el nombre del bean**. Lo escribí
`PasswordEncoder()` con mayúscula y quedó registrado con ese nombre. Compila y funciona porque
lo pido por tipo, pero rompe la convención y fallaría un `@Qualifier("passwordEncoder")`.

---

## Component scan

**En una frase:** Spring busca clases anotadas para convertirlas en beans, empezando por el
paquete de la clase principal.

**En este proyecto:** todo cuelga de `com.dmh.users`, el paquete de `UsersServiceApplication`.

**Cómo lo cuento en 30 segundos:** "Al arrancar, Spring escanea el paquete de la clase principal
y todos sus subpaquetes buscando anotaciones, y registra lo que encuentra. Por eso la estructura
de paquetes no es solo prolijidad: define qué ve el framework."

**Dónde me trabé:** los DTOs quedaron en `com.dmh.user.dto`, sin la "s", fuera del paquete
principal. Con DTOs no pasa nada, porque no son beans. Pero si hubiera sido un `@Service`,
Spring no lo habría encontrado y el error recién aparecería al arrancar, no al compilar.

---

## Inyección de dependencias

**En una frase:** en vez de que una clase cree lo que necesita, se lo dan hecho desde afuera.

**Qué problema resuelve:** si `UserService` hiciera `new BCryptPasswordEncoder()` adentro,
quedaría casado con esa implementación y no habría forma de reemplazarla en un test.

**En este proyecto:** `UserService` recibe `UserRepository`, `PasswordEncoder` y
`AccountDataGenerator` por el constructor, que genera Lombok con `@RequiredArgsConstructor`
a partir de los campos `final`. En `UserServiceTest` se le pasan mocks y no hace falta levantar
Spring ni la base.

**Cómo lo cuento en 30 segundos:** "Spring administra los objetos que la aplicación necesita y
se los entrega a quien los pide, normalmente por el constructor. Eso permite cambiar la
implementación sin tocar la clase que la usa, y testear con dobles de prueba."

**Repreguntas típicas:**
- *¿Por constructor o con `@Autowired` en el campo?* Por constructor: permite campos `final`,
  deja las dependencias a la vista y se puede instanciar la clase en un test sin Spring. La
  inyección por campo esconde dependencias y hace fácil que una clase acumule diez sin que se note.
- *¿Y si hay dos beans del mismo tipo?* Falla al arrancar por ambigüedad; se desempata con
  `@Qualifier` o marcando uno como `@Primary`.
- *¿Inversión de control e inyección de dependencias es lo mismo?* La inversión de control es
  el principio (el framework controla el flujo, no tu código); la inyección de dependencias es
  la forma concreta en que Spring lo aplica.

---

## Las capas

**En una frase:** cada capa tiene un solo trabajo, y las de arriba no saben cómo trabajan las
de abajo.

```
Controller   recibe el HTTP, valida y delega. No piensa.
Service      la lógica de negocio
Repository   acceso a datos
Entity       la tabla, como clase Java
DTO          lo que entra y sale por HTTP
```

**En este proyecto:** `UserController` solo recibe el JSON, dispara las validaciones con
`@Valid` y llama a `userService.register(...)`. Toda la lógica (duplicados, hash, CVU, alias)
está en el service.

**Cómo lo cuento en 30 segundos:** "Separo controller, service y repository. El controller
traduce HTTP a llamadas de negocio, el service tiene las reglas y el repository habla con la
base. Así la lógica no depende de que la entrada sea HTTP: si mañana entra por una cola de
mensajes, el service no cambia."

**Repreguntas típicas:**
- *¿Por qué no poner la lógica en el controller?* Porque queda atada al protocolo, no se puede
  reutilizar y es más difícil de testear: hay que simular requests en vez de llamar un método.
