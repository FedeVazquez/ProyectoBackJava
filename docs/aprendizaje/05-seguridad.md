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

---

## Qué es un JWT

**En una frase:** es un string firmado que el servidor emite en el login y el cliente devuelve en
cada request para decir quién es.

**Las tres partes:** `header.payload.firma`, separadas por puntos.

| Parte | Qué lleva |
|---|---|
| Header | El algoritmo de firma (acá HS256) |
| Payload | Los datos ("claims"): en este proyecto `sub` (el email), `iat` (emitido) y `exp` (vence) |
| Firma | El resultado de firmar header y payload con la clave secreta del servidor |

**Lo que más se malinterpreta:** el header y el payload están en **Base64, no encriptados**.
Cualquiera que tenga el token puede leerlos, por ejemplo pegándolo en jwt.io. Lo que el JWT
garantiza no es secreto, es **integridad**: sin la clave, nadie puede fabricar un token válido
ni modificar uno existente, porque la firma deja de coincidir.

**Cómo lo cuento en 30 segundos:** "En el login valido las credenciales y emito un JWT firmado
con una clave del servidor. El cliente lo manda en el header `Authorization` en cada request, y
el servidor solo verifica la firma y el vencimiento: no consulta la base ni guarda sesión. El
payload es legible, así que no pongo nada sensible adentro."

**Repreguntas típicas:**
- *¿Por qué no guardar la contraseña o datos personales en el payload?* Porque cualquiera lo
  puede leer. Solo va un identificador y las fechas.
- *¿Entonces para qué sirve la firma?* Para que nadie pueda cambiar el `sub` por el email de otro
  usuario: al modificar el payload, la firma deja de validar.
- *¿Qué pasa si roban el token?* Sirve hasta que venza. Por eso los vencimientos son cortos y
  existen los refresh tokens.
- *¿HS256 o RS256?* HS256 usa **una sola clave** para firmar y verificar: alcanza cuando el mismo
  sistema hace las dos cosas. RS256 usa clave privada para firmar y pública para verificar, que
  es lo que conviene cuando varios servicios tienen que validar tokens que no emitieron.

---

## El `JwtService` de este proyecto

```java
public JwtService(@Value("${jwt.secret}") String secret,
		@Value("${jwt.expiration-ms}") long expirationMs) {
	this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
	this.expirationMs = expirationMs;
}
```

**Decisiones que hay detrás:**

- **La clave se arma una sola vez, en el constructor.** Convertir el texto en un objeto
  criptográfico en cada token sería trabajo repetido al pedo.
- **`Keys.hmacShaKeyFor` falla al arrancar si la clave es corta** (`WeakKeyException`). HS256
  exige 256 bits. El error aparece en el arranque y no en el primer login: fail-fast.
- **El constructor va escrito a mano, sin `@RequiredArgsConstructor`.** Lombok no puede poner
  anotaciones (`@Value`) en los parámetros que genera.
- **Validar es parsear.** `parseSignedClaims` verifica la firma y el vencimiento, y **lanza una
  excepción** si algo está mal; no devuelve `null`. `isValid` es un envoltorio con `try/catch`
  sobre `JwtException` (de la que heredan firma inválida, token vencido y formato roto) y
  `IllegalArgumentException` (token nulo o vacío).

**Dónde me trabé:** la API de `jjwt` cambió en la versión 0.12. Ahora es `.subject(...)`,
`.issuedAt(...)`, `.expiration(...)` y `Jwts.parser().verifyWith(key)`. Los tutoriales viejos
usan `.setSubject(...)` y `parserBuilder().setSigningKey(...)`, que ya no compilan.

---

## La clave secreta: dónde vive

**En una frase:** el código va a Git, los secretos no.

**En este proyecto:**
```properties
jwt.secret=${JWT_SECRET:<clave de desarrollo>}
```

Spring resuelve `${VARIABLE:valorPorDefecto}`: si existe la variable de entorno la usa, y si no,
cae en el valor por defecto. Así la app arranca sin configuración en desarrollo, y en producción
la variable de entorno pisa ese valor.

**Cómo lo cuento en 30 segundos:** "La clave de firma se lee de una variable de entorno, con un
valor por defecto de desarrollo para que el proyecto arranque recién clonado. La clave real nunca
está en el repositorio: si se filtra, cualquiera puede emitir tokens válidos y hacerse pasar por
cualquier usuario."

**Repreguntas típicas:**
- *¿Y si ya se commiteó una clave?* No alcanza con borrarla: queda en el historial de Git. Hay
  que **rotarla**, o sea generar una nueva y dar la vieja por comprometida.
- *¿Qué pasa cuando rotás la clave?* Todos los tokens emitidos dejan de validar y los usuarios
  tienen que volver a loguearse.
