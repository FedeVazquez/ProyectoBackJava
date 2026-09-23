# 05 — Seguridad

> Esta ficha cubre hasta donde llegó el proyecto: hash de contraseñas y la configuración base de
> Spring Security. La parte de JWT se amplía cuando esté implementada.

## Por qué la contraseña se hashea y no se encripta

**En una frase:** encriptar es reversible y hashear no; para una contraseña, nadie —ni el propio
sistema— necesita poder recuperar el texto original.

**Qué problema resuelve:** si se filtra la base, las contraseñas siguen sin ser legibles. Como
mucha gente repite contraseña entre sitios, una filtración en texto plano compromete cuentas de
otros servicios.

**En este proyecto:** `passwordEncoder.encode(...)` antes de guardar. En la base queda algo como
`$2a$10$N9qo8uLOickgx2ZMRZoMye...`, de 60 caracteres.

**Cómo lo cuento en 30 segundos:** "Las contraseñas se guardan hasheadas con BCrypt, nunca
encriptadas ni en texto plano. En el login no se desencripta nada: se hashea lo que mandó el
usuario y se compara con `matches`."

**Repreguntas típicas:**
- *¿Por qué BCrypt y no SHA-256?* SHA-256 está diseñado para ser **rápido**, y eso juega a favor
  del atacante: una GPU prueba miles de millones por segundo. BCrypt es deliberadamente lento y
  tiene un factor de costo ajustable, que se sube a medida que el hardware mejora.
- *¿Qué es el salt?* Un valor aleatorio que se agrega antes de hashear, distinto para cada
  contraseña. Hace que dos usuarios con la misma clave tengan hashes distintos y anula las
  tablas precalculadas (rainbow tables). BCrypt genera el salt solo y lo guarda dentro del
  hash resultante.
- *Si el hash cambia siempre, ¿cómo se compara?* Con `passwordEncoder.matches(plana, hash)`: el
  salt está dentro del hash guardado, así que se reutiliza para hashear la candidata y comparar.
- *¿Por qué el encoder se declara como bean y como interfaz `PasswordEncoder`?* Para poder
  cambiar el algoritmo (a Argon2, por ejemplo) tocando una sola línea.

---

## `spring-security-crypto` vs `spring-boot-starter-security`

**En una frase:** el primero trae solo los algoritmos de hash; el segundo, todo el framework de
seguridad, que apenas aparece bloquea la aplicación entera.

**En este proyecto:** arrancó con `spring-security-crypto` para poder hashear sin bloquear nada,
y el starter completo entró recién al implementar el login.

**Cómo lo cuento en 30 segundos:** "Al principio solo necesitaba BCrypt, así que usé el módulo
de criptografía suelto. El starter completo activa la autoconfiguración de Security, que protege
todos los endpoints y genera un usuario por defecto: sumarlo antes de tiempo complica el
desarrollo sin aportar nada."

**Qué se ve al agregarlo:** en el log aparece `Using generated security password: ...` y
cualquier endpoint que antes funcionaba pasa a devolver **401**.

---

## La cadena de filtros

**En una frase:** Spring Security es una fila de filtros que se ejecuta antes del controller y
puede cortar el request.

**En este proyecto:**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	http
		.csrf(csrf -> csrf.disable())
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth -> auth
			.requestMatchers(HttpMethod.POST, "/users").permitAll()
			.requestMatchers("/error").permitAll()
			.anyRequest().authenticated());

	return http.build();
}
```

**Cómo lo cuento en 30 segundos:** "Security se inserta como una cadena de filtros delante de
los controllers. Yo declaro un `SecurityFilterChain` donde digo qué rutas son públicas y qué
rutas exigen autenticación; el resto del código no se entera de que existe."

**Repreguntas típicas:**
- *¿El orden de las reglas importa?* Sí: se evalúan de arriba hacia abajo y gana la primera que
  coincide. Por eso `anyRequest()` va siempre al final.
- *Autenticación vs autorización.* Autenticación es "quién sos"; autorización, "qué podés hacer".
  En el código: quién sos lo resuelve el filtro de JWT; qué podés hacer, las reglas de
  `authorizeHttpRequests`.

---

## CSRF: por qué se deshabilita acá

**En una frase:** CSRF protege sesiones basadas en cookies, y esta API no las usa.

**El ataque:** estás logueado en tu banco y visitás otra página; esa página dispara un POST al
banco y **el navegador manda la cookie de sesión sola**, porque las cookies viajan por dominio.
Sin protección, el banco creería que la orden la diste vos.

**Por qué no aplica con JWT:** el token va en un header `Authorization` que hay que poner a mano.
Una página de terceros no puede agregarlo, así que el ataque no funciona.

**Cómo lo cuento en 30 segundos:** "La protección CSRF tiene sentido cuando el navegador manda
credenciales automáticamente, o sea con cookies de sesión. Como uso JWT en un header y la API es
stateless, la deshabilito. Si volviera a sesiones con cookies, habría que reactivarla."

**Dónde me trabé (o me iba a trabar):** si CSRF queda activo, **todo POST devuelve 403 aunque la
ruta esté en `permitAll`**, y el mensaje no dice nada de CSRF. Es el error más común al configurar
Security por primera vez.

---

## Stateless y sesiones

**En una frase:** el servidor no guarda nada entre requests; cada request trae todo lo necesario
para identificar al usuario.

**En este proyecto:** `SessionCreationPolicy.STATELESS`.

**Cómo lo cuento en 30 segundos:** "Con JWT no hay sesión en el servidor: el token viaja en cada
request y se valida con una firma. Eso permite escalar horizontalmente, porque cualquier
instancia del servicio puede atender cualquier request sin compartir estado."

**Repreguntas típicas:**
- *¿Ventaja frente a sesiones?* No hace falta almacenamiento compartido ni sesiones pegajosas
  entre instancias.
- *¿Desventaja?* No se puede invalidar un token emitido, porque el servidor no lleva registro.
  De ahí que el logout con JWT necesite una estrategia aparte: expiración corta, lista de
  revocados o refresh tokens.

---

## Detalle que cuesta caro: `/error`

**En una frase:** si `/error` queda protegido, los errores internos vuelven como 401 o 403 y
ocultan el problema real.

**Por qué pasa:** ante un error que no maneja tu handler, Spring Boot hace un forward interno a
`/error` para armar la respuesta. Ese forward también pasa por Security.

**Cómo lo cuento en 30 segundos:** "Dejo `/error` como público. Si no, un 404 o un error
cualquiera vuelve como error de autenticación y te manda a investigar permisos cuando el
problema es otro."
