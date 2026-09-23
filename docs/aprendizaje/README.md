# Aprendizaje — para entender y para saber explicar

Este documento es distinto de `docs/CONTEXTO.md`.

| Documento | Para qué |
|---|---|
| `docs/CONTEXTO.md` | El estado del proyecto: qué está hecho, qué sigue, qué se decidió. Le habla a la próxima sesión de trabajo |
| `docs/aprendizaje/` | Los conceptos explicados en criollo, con la respuesta de entrevista ya redactada. Le habla a mi yo de la entrevista |

**Cómo usarlo:** leer una ficha, tapar la parte de "cómo lo cuento" y tratar de decirlo en voz alta.
Si no sale, la explicación todavía no es mía. Reescribir con mis palabras las que no suenen a cómo
las diría yo.

## Índice

| Archivo | Temas |
|---|---|
| [01-spring-basico.md](01-spring-basico.md) | Beans, inyección de dependencias, anotaciones, capas, autoconfiguración |
| [02-jpa-hibernate.md](02-jpa-hibernate.md) | ORM, entidades, repositorios, `ddl-auto`, transacciones |
| [03-dto-validaciones.md](03-dto-validaciones.md) | Por qué DTOs, `@Valid`, dónde van los límites de datos |
| [04-errores-http.md](04-errores-http.md) | Códigos de estado, `@RestControllerAdvice`, contrato de error |
| [05-seguridad.md](05-seguridad.md) | Hash de contraseñas, BCrypt, la cadena de filtros, CSRF, JWT |
| [06-testing.md](06-testing.md) | Unitario vs integración, mocks, qué se testea y qué no |
| [08-herramientas.md](08-herramientas.md) | Maven, Lombok, JDK, Eclipse, Git |

Pendientes de escribir, cuando lleguemos a esa parte del proyecto:

- `07-microservicios.md` — por qué separar servicios, Eureka, Gateway, Feign
- Ampliar `05-seguridad.md` con JWT cuando esté implementado

---

## Cómo cuento este proyecto en 2 minutos

> Es la API REST de una billetera virtual, el desafío final de una especialización de
> back-end. Un usuario se registra, inicia sesión y opera con su cuenta: carga saldo,
> transfiere y consulta movimientos. Cada usuario tiene una CVU de 22 dígitos y un alias
> de tres palabras, igual que una billetera real.
>
> Está hecho en Java 21 con Spring Boot, JPA sobre MySQL y autenticación con JWT. La
> arquitectura es de microservicios: hay un servicio de usuarios, uno de cuentas, uno de
> tarjetas y uno de transacciones, con Eureka para service discovery y un API Gateway como
> única puerta de entrada.
>
> Lo armé de forma incremental: primero el servicio de usuarios solo contra la base, y recién
> después la parte distribuida, para que cada paso fuera verificable. Dentro de cada servicio
> uso las capas clásicas — controller, service, repository, entity — con DTOs separados, así
> la forma de la API no queda atada a la forma de las tablas.
>
> Lo más interesante fue el registro: al crearse un usuario tiene que nacer también su cuenta,
> y como viven en servicios distintos hay que coordinarlos por HTTP. Ese es el primer problema
> real de comunicación entre microservicios del proyecto.

**Si preguntan "¿qué harías distinto?":** empezar antes con los tests de integración, y separar
la base de desarrollo de la que usan los tests; al principio los tests corrían contra el mismo
archivo H2 que usaba a mano.
