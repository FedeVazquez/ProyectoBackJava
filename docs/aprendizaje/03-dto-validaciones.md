# 03 — DTOs y validaciones

## Por qué existe el DTO

**En una frase:** un DTO es la forma que tienen los datos en la API, separada de la forma que
tienen en la base.

**Qué problema resuelve:** si el controller devolviera la entidad, la API quedaría atada a las
columnas de la tabla y cualquier campo nuevo se publicaría sin querer. El caso más claro es la
contraseña: está en `User`, pero no puede salir nunca por HTTP.

**En este proyecto:** dos DTOs para el registro.
- `RegisterUserRequest`: lo que entra. Tiene `password`, no tiene `id`, `cvu` ni `alias`,
  porque los genera el servidor.
- `UserResponse`: lo que sale. Tiene `id`, `cvu` y `alias`, y **no** tiene `password`.

**Cómo lo cuento en 30 segundos:** "Uso DTOs separados de las entidades. La entidad modela la
tabla y el DTO modela el contrato de la API. Así puedo cambiar el modelo interno sin romper a
los clientes, y sobre todo no expongo campos que no deben salir, como el hash de la contraseña."

**Repreguntas típicas:**
- *¿No es código repetido?* Sí, hay algo de mapeo repetido, y se puede automatizar con MapStruct.
  El costo se paga una vez; el acople de exponer entidades se paga para siempre.
- *¿Un DTO para entrada y otro para salida, o uno solo?* Separados: los campos no coinciden. El
  de entrada tiene contraseña y el de salida tiene el id y los datos generados por el servidor.
- *¿Por qué `UserResponse` es una clase con Lombok y `ErrorResponse` un `record`?* El record es
  inmutable y no necesita Lombok, pero no tiene constructor vacío ni setters, que es lo que
  Jackson y Lombok usan cómodamente en los DTOs de entrada. Para respuestas simples, el record
  alcanza y sobra.

**Dónde me trabé:** después de agregar `cvu` y `alias` a la entidad, el dato se guardaba bien
pero volvía `null` en la respuesta. El mapeo `toResponse` seguía pasando `null` fijo. Lección:
cuando un dato está en la base pero no llega al cliente, el problema está en el mapeo, no en
la entidad.

---

## Bean Validation: `@Valid` y las anotaciones

**En una frase:** las reglas de los datos se declaran como anotaciones sobre los campos del DTO
y se disparan con `@Valid` en el controller.

**En este proyecto:**

```java
@NotBlank(message = "El nombre es obligatorio")
@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
private String name;

@NotBlank(message = "El DNI es obligatorio")
@Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener entre 7 y 8 dígitos")
private String dni;
```

y en el controller:

```java
public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request)
```

**Cómo lo cuento en 30 segundos:** "Uso Bean Validation: las reglas van declaradas en el DTO y
el controller las dispara con `@Valid`. Si algo falla, Spring lanza
`MethodArgumentNotValidException` antes de llegar al service, y un `@RestControllerAdvice` la
convierte en un 400 con el detalle por campo."

**Repreguntas típicas:**
- *¿Qué pasa si me olvido el `@Valid`?* **Las anotaciones del DTO no hacen nada.** Los datos
  pasan sin validar y el error aparece mucho después, en la base o en la lógica. No lo marca
  el compilador ni el IDE: es el error más silencioso de todos.
- *Diferencia entre `@NotNull`, `@NotEmpty` y `@NotBlank`.* `@NotNull` solo rechaza null;
  `@NotEmpty` además rechaza cadena o colección vacía; `@NotBlank`, para texto, también rechaza
  una cadena de solo espacios.
- *¿Y las validaciones de negocio?* Bean Validation valida la **forma** del dato (obligatorio,
  largo, formato). Que el email no esté repetido es una regla de negocio: necesita la base y va
  en el service.

---

## Dónde van los límites de tamaño

**En una frase:** el mismo límite se declara en dos lugares, la entidad y el DTO, y cada uno
cumple un rol distinto.

**En este proyecto:** `name` es `@Column(length = 100)` en la entidad y `@Size(max = 100)` en el
DTO. Email: 254 en los dos lados.

**Cómo lo cuento en 30 segundos:** "El `length` de la entidad define la columna; el `@Size` del
DTO define el contrato de la API. Si el límite existe solo en la base, un dato demasiado largo
llega hasta el INSERT y explota como error 500; con la validación en el DTO, el cliente recibe
un 400 con un mensaje que le dice qué corregir."

**Repreguntas típicas:**
- *¿No es duplicar la regla?* Sí, y es duplicación deliberada: son dos capas distintas
  protegiéndose. Lo importante es que no se desincronicen.
- *¿Por qué 254 para el email?* Es el máximo que admite el estándar. Poner 50 sería inventar un
  límite que rechaza direcciones válidas.
- *¿Por qué la contraseña tiene `max = 72`?* Es el límite de BCrypt: lo que pase de 72 bytes lo
  ignora sin avisar. Mejor rechazarlo explícitamente que dar la ilusión de una clave más larga.

---

## Normalizar la entrada

**En una frase:** limpiar el dato antes de guardarlo, para que comparaciones y restricciones
funcionen como uno espera.

**En este proyecto:** `request.getEmail().trim().toLowerCase()` antes de chequear duplicados y
antes de guardar.

**Cómo lo cuento en 30 segundos:** "Normalizo el email a minúsculas y sin espacios. La
restricción `unique` de la base compara texto exacto, así que sin normalizar `Juan@Mail.com` y
`juan@mail.com` serían dos usuarios distintos y el login después no encontraría al usuario."

**Repreguntas típicas:**
- *¿Dónde se normaliza, en el controller o en el service?* En el service: es una regla de
  negocio, no una cuestión de transporte.
- *¿Y el nombre?* Solo `trim()`. Pasarlo a minúsculas destruiría información real del usuario.
