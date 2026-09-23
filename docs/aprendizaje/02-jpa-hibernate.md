# 02 — JPA e Hibernate

## ORM, JPA e Hibernate: quién es quién

**En una frase:** un ORM traduce entre objetos Java y filas de una tabla; JPA es el estándar
que define cómo se hace, e Hibernate es la implementación concreta que usamos.

**Qué problema resuelve:** sin ORM habría que escribir el SQL de cada consulta y copiar a mano
cada columna a cada campo del objeto.

**En este proyecto:** `User` es una clase anotada con `@Entity`; Hibernate genera el
`create table` y los `insert`. Con `show-sql=true` el SQL aparece en el log.

**Cómo lo cuento en 30 segundos:** "JPA es la especificación, Hibernate la implementación y
Spring Data JPA una capa encima que genera los repositorios. Uno trabaja con objetos y el ORM
produce el SQL."

**Repreguntas típicas:**
- *¿Ventajas y desventajas del ORM?* Ahorra código repetitivo y abstrae el motor de base de
  datos; a cambio esconde el SQL real y puede generar consultas ineficientes si no se mira
  lo que produce.
- *¿Qué es el problema N+1?* Traer una lista de N elementos y que el ORM haga una consulta
  extra por cada uno para cargar sus relaciones. Se resuelve con un `JOIN FETCH` o `EntityGraph`.

---

## Entidad y mapeo

**En una frase:** una entidad es una clase que representa una tabla, y cada campo, una columna.

**En este proyecto:**

```java
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 22)
	private String cvu;
	...
}
```

**Qué hace cada anotación:**
- `@Entity`: esta clase se mapea a una tabla. Necesita constructor sin argumentos.
- `@Table(name = "users")`: el nombre de la tabla. Sin esto usaría el nombre de la clase, y
  `user` es palabra reservada en varios motores.
- `@Id`: la clave primaria.
- `@GeneratedValue(IDENTITY)`: el valor lo genera la base (autoincremental), no la aplicación.
- `@Column(nullable, unique, length)`: restricciones que terminan en el DDL de la tabla.

**Cómo lo cuento en 30 segundos:** "Anoto la clase con `@Entity` y los campos con `@Column`.
Hibernate se encarga del mapeo y de convertir camelCase en snake_case: `lastName` pasa a ser
la columna `last_name`."

**Repreguntas típicas:**
- *¿Por qué el CVU es `String` y no `Long`?* Porque 22 dígitos no entran en un `Long` y como
  número se perderían los ceros a la izquierda. Un identificador no es un número aunque parezca:
  no se suma ni se promedia.
- *¿La entidad se devuelve por HTTP?* No: para eso están los DTOs. Ver `03-dto-validaciones.md`.

---

## Spring Data JPA y los repositorios

**En una frase:** se declara una interfaz con métodos de nombre descriptivo y Spring genera
la implementación.

**En este proyecto:**

```java
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByDni(String dni);
}
```

**Cómo lo cuento en 30 segundos:** "Extiendo `JpaRepository` y ya tengo el CRUD. Para las
consultas propias alcanza con nombrar el método siguiendo una convención: Spring Data parsea
el nombre y arma la query. Si el caso es complejo, se escribe con `@Query`."

**Repreguntas típicas:**
- *¿`JpaRepository<User, Long>` qué significan los dos tipos?* La entidad y el tipo de su clave
  primaria.
- *¿Qué pasa si escribo mal el nombre del método?* Falla **al arrancar la aplicación**, no al
  compilar: Spring no encuentra la propiedad y tira `PropertyReferenceException`. Es un error
  que Eclipse no marca.
- *¿Por qué `Optional<User>` y no `User`?* Porque puede no haber resultado. `Optional` obliga a
  quien llama a contemplar ese caso, en vez de que aparezca un `null` inesperado.

---

## `ddl-auto` y el esquema

**En una frase:** Hibernate puede crear o modificar las tablas solo, según lo que digan las
entidades.

**En este proyecto:** `spring.jpa.hibernate.ddl-auto=update`, solo para desarrollo.

**Cómo lo cuento en 30 segundos:** "En desarrollo uso `update` para no escribir DDL a mano
mientras el modelo cambia todos los días. En producción no va nunca: el esquema se versiona
con una herramienta de migraciones, tipo Flyway o Liquibase, para que los cambios sean
revisables y reversibles."

**Repreguntas típicas:**
- *¿Qué valores tiene?* `none`, `validate` (verifica que el esquema coincida), `update`
  (agrega lo que falta), `create` y `create-drop` (recrean todo y borran datos).
- *¿Por qué `update` no alcanza en producción?* Nunca borra ni modifica columnas existentes,
  no versiona los cambios y puede fallar a mitad de camino dejando el esquema inconsistente.

**Dónde me trabé:** al agregar `cvu` y `alias` como `not null` a una tabla que ya tenía filas,
el `alter table` falló (las filas existentes no tienen valor para la columna nueva) y el
siguiente POST devolvía 500. Se resolvió borrando el archivo H2 local, que está en `.gitignore`.
En producción eso mismo se hace con una migración en tres pasos: agregar la columna como
nullable, rellenarla y recién después ponerla `not null`.

---

## Transacciones

**En una frase:** un bloque de operaciones que se confirma entero o no se confirma nada.

**En este proyecto:** `UserService.register` está anotado con `@Transactional`. Si algo falla
después del `save`, no queda el usuario a medio crear.

**Cómo lo cuento en 30 segundos:** "Anoto el método del service con `@Transactional`. Spring
abre la transacción antes de entrar y hace commit al salir; si se escapa una excepción de
runtime, hace rollback."

**Repreguntas típicas:**
- *¿Con qué excepciones hace rollback?* Por defecto solo con las unchecked (`RuntimeException`).
  Para las checked hay que pedirlo con `rollbackFor`.
- *¿Dónde va, en el controller o en el service?* En el service: es donde está la unidad de
  trabajo del negocio.
- *¿Por qué el import importa?* Hay dos `@Transactional`: el de Spring
  (`org.springframework.transaction.annotation`) y el de Jakarta. Se usa el de Spring, que
  entiende `readOnly`, `propagation` y `rollbackFor`.
- *¿Funciona si llamo al método desde la misma clase?* No. Spring implementa `@Transactional`
  con un proxy que envuelve al bean; una llamada interna no pasa por el proxy y la anotación
  se ignora silenciosamente. Es un clásico de entrevista.
