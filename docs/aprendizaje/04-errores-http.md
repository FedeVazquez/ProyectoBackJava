# 04 — Errores y códigos HTTP

## Los códigos que uso y por qué

**En una frase:** el código de estado es la primera respuesta que lee un cliente; tiene que
distinguir "te equivocaste vos" de "me equivoqué yo".

| Código | Familia | Cuándo |
|---|---|---|
| 200 OK | éxito | Operación normal |
| 201 Created | éxito | Se creó un recurso. Es el del registro |
| 400 Bad Request | error del cliente | Datos inválidos o inconsistentes |
| 401 Unauthorized | error del cliente | No estás autenticado (mal nombre: debería ser "unauthenticated") |
| 403 Forbidden | error del cliente | Estás autenticado pero no tenés permiso |
| 404 Not Found | error del cliente | El recurso no existe |
| 500 Internal Server Error | error del servidor | Se rompió algo del lado nuestro |

**Cómo lo cuento en 30 segundos:** "La familia 4xx dice que el problema está en el request y
reintentarlo igual no sirve; la 5xx dice que el problema es del servidor. Esa distinción es lo
que permite a un cliente decidir si mostrar un mensaje al usuario o reintentar más tarde."

**Repreguntas típicas:**
- *¿401 o 403?* 401 es "no sé quién sos"; 403 es "sé quién sos y no te alcanza".
- *¿Un email duplicado es 400 o 409?* 409 Conflict es más preciso, pero la consigna del proyecto
  solo admite 400, 500 y 201 en el registro, así que uso 400. En una entrevista lo diría así:
  conozco la diferencia y elegí ajustarme al contrato pedido.
- *¿Por qué 201 y no 200 al registrar?* Porque se creó un recurso nuevo. Lo ideal es acompañarlo
  con el header `Location` apuntando a la URL del recurso creado.

---

## Manejo centralizado con `@RestControllerAdvice`

**En una frase:** una clase que atrapa las excepciones de todos los controllers y las convierte
en respuestas HTTP con un formato único.

**En este proyecto:** `GlobalExceptionHandler` maneja cinco casos.

| Excepción | Status | Caso |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Falló `@Valid`; devuelve mapa `campo → mensaje` |
| `HttpMessageNotReadableException` | 400 | Body vacío o JSON roto |
| `UserAlreadyExistsException` | 400 | Email o DNI repetido (excepción propia) |
| `DataIntegrityViolationException` | 400 | Chocó una restricción `unique` de la base |
| `Exception` | 500 | Cualquier otra cosa: mensaje neutro y stack trace al log |

**Cómo lo cuento en 30 segundos:** "Centralizo los errores en un `@RestControllerAdvice`. Los
services lanzan excepciones de dominio y el handler decide el código HTTP y el cuerpo. Así los
controllers no se llenan de try/catch y toda la API responde los errores con el mismo formato."

**Repreguntas típicas:**
- *¿Por qué una excepción propia y no `RuntimeException`?* Porque el handler decide según el
  **tipo**. Con una genérica no se puede distinguir "email duplicado" (400) de "se cayó la base"
  (500).
- *¿Por qué las excepciones son unchecked?* Para que suban solas hasta el handler sin obligar a
  declarar `throws` en cada método del camino.
- *¿Qué no hay que devolver nunca?* El stack trace o el mensaje crudo de la base. Filtra detalles
  internos (motor, nombres de tablas) que le sirven a un atacante. Al cliente, mensaje neutro;
  la traza, al log.

**Por qué el handler de body ilegible:** sin él, un JSON roto lo atraparía el catch-all de
`Exception` y devolvería 500, cuando en realidad el error es del cliente.

**Limitación conocida:** el catch-all también atrapa excepciones que Spring ya maneja bien,
como 405 (método no permitido) o 415 (Content-Type incorrecto), y las convierte en 500. Se
corrige extendiendo `ResponseEntityExceptionHandler`, que trae handlers por defecto para esos
casos. Está anotado como pendiente en `CONTEXTO.md`.

---

## El contrato de error

**En una frase:** todos los errores de la API tienen la misma forma, para que el cliente pueda
tratarlos igual.

**En este proyecto:**

```java
public record ErrorResponse(int status, String message, Map<String, String> errors) { }
```

```json
{
  "status": 400,
  "message": "Hay datos invalidos",
  "errors": {
    "dni": "El DNI debe tener entre 7 y 8 dígitos",
    "email": "El email no tiene un formato válido"
  }
}
```

**Cómo lo cuento en 30 segundos:** "Definí un formato único de error con status, mensaje y un
mapa de errores por campo. El front puede mostrar el mensaje general o pintar cada campo del
formulario con su error, sin parsear texto."

**Repreguntas típicas:**
- *¿Por qué devolver todos los campos inválidos juntos y no el primero?* Para que el usuario
  corrija todo de una vez en lugar de descubrir los errores de a uno.
- *¿Qué es un `record`?* Una clase inmutable y concisa de Java 16+: define los campos y genera
  constructor, getters, `equals`, `hashCode` y `toString`. Para un DTO de salida va perfecto.

**Dónde me trabé:** armar el mapa con `Collectors.toMap` falla con `IllegalStateException` si un
mismo campo tiene **dos** violaciones, porque la clave se repite. Hay que pasarle una función de
merge, por ejemplo quedarse con el primer mensaje.
