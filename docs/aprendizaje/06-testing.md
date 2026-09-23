# 06 — Testing

## Unitario, integración y end-to-end

**En una frase:** el test unitario prueba una clase aislada, el de integración prueba varias
piezas trabajando juntas, y el end-to-end prueba el sistema completo como lo usaría un cliente.

| Tipo | Qué levanta | Velocidad | Qué detecta |
|---|---|---|---|
| Unitario | Nada; dependencias mockeadas | milisegundos | Errores de lógica |
| Integración | Spring, base de datos (en memoria o contenedor) | segundos | Errores de mapeo, consultas, configuración |
| End-to-end | La app entera vía HTTP | más lento | Errores de contrato y de flujo completo |

**En este proyecto:** hoy hay unitarios (`AccountDataGeneratorTest`, `UserServiceTest`) más el
test de contexto que genera Spring Boot. Los de integración con RestAssured sobre `POST /users`
están pendientes.

**Cómo lo cuento en 30 segundos:** "Pirámide de tests: muchos unitarios, que son rápidos y
apuntan a la lógica; menos de integración, que verifican que las piezas encajen; y unos pocos
end-to-end sobre los flujos críticos. Invertir la pirámide da suites lentas y frágiles."

**Repreguntas típicas:**
- *¿Qué no se testea?* Los getters y setters, el framework, y las clases sin lógica. Cubrir eso
  infla la métrica de cobertura sin agregar seguridad.
- *¿La cobertura es una buena métrica?* Sirve para encontrar zonas sin probar, pero 100% no
  significa correcto: se puede ejecutar todo el código sin verificar nada.

---

## Mocks con Mockito

**En una frase:** un mock es un objeto falso que reemplaza a una dependencia real para poder
probar una clase sola.

**En este proyecto:**

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AccountDataGenerator accountDataGenerator;

	@InjectMocks private UserService userService;
```

**Qué hace cada anotación:**
- `@ExtendWith(MockitoExtension.class)`: conecta Mockito con JUnit 5; es lo que crea los mocks
  antes de cada test.
- `@Mock`: crea un doble de esa dependencia. Por defecto todos sus métodos devuelven `null`,
  `0` o `false`.
- `@InjectMocks`: crea la clase bajo prueba y le pasa los mocks por el constructor. Funciona
  porque `UserService` usa inyección por constructor.
- `when(...).thenReturn(...)`: define qué devuelve un mock ante una llamada concreta (stubbing).
- `verify(...)`: comprueba que cierta llamada ocurrió, o que **no** ocurrió con `never()`.

**Cómo lo cuento en 30 segundos:** "Para testear el service, mockeo repositorio y encoder con
Mockito. No levanto Spring ni la base: el test corre en milisegundos y verifica solo la lógica
del service. Si el mock del repositorio dice que el email ya existe, espero la excepción y que
`save` no se haya llamado nunca."

**Repreguntas típicas:**
- *¿Mock, stub o spy?* El stub devuelve respuestas fijas; el mock además permite verificar
  interacciones; el spy envuelve un objeto real y solo reemplaza algunos métodos.
- *¿Qué es el stubbing innecesario?* Configurar un `when(...)` que el código nunca usa. Mockito
  falla el test a propósito, porque suele indicar que el test no prueba lo que cree probar.
- *¿Qué es `ArgumentCaptor`?* Captura el objeto que se le pasó a un mock para inspeccionarlo.
  Acá es la única forma de verificar que el usuario guardado tiene el email en minúsculas y la
  contraseña hasheada, porque esos valores no salen en la respuesta.

**Dónde me trabé:** al principio stubbeaba todo, y Mockito hacía fallar los tests por stubs sin
usar. Como los mocks devuelven `false` por defecto, en el test de "email duplicado" no hace falta
stubbear `existsByDni`: nunca se llega ahí.

---

## Qué verifica cada test del proyecto

| Test | Qué asegura |
|---|---|
| `cvuHasTwentyTwoDigits` | El CVU cumple el formato que pide la consigna |
| `aliasHasThreeDifferentWordsSeparatedByDots` | Tres palabras **distintas** separadas por puntos; corre 50 veces por ser aleatorio |
| `twoGeneratedCvusAreDifferent` | El generador no devuelve siempre lo mismo |
| `registerSaveUserWith...` | Email normalizado, contraseña hasheada y CVU/alias asignados al guardar |
| `registerFailsWhenEmailAlreadyExists` | Lanza la excepción y **no** guarda |
| `registerFailsWhenDniAlreadyExists` | Ídem por DNI |

**Sobre testear código aleatorio:** una sola corrida puede pasar de casualidad. Por eso el test
del alias usa `@RepeatedTest(50)`: repite el caso y hace muy improbable que un bug intermitente
pase inadvertido.

**Trampa de Java que apareció acá:** `Set.of(array)` **lanza excepción** si hay elementos
repetidos, en vez de descartarlos. Para contar valores distintos hay que usar
`new HashSet<>(List.of(parts))`; con `Set.of` el test fallaría con un error confuso justo en el
caso que se quiere detectar.

---

## Un pendiente que vale contar

**El problema:** `UsersServiceApplicationTests` levanta Spring con el `application.properties`
normal, así que usa el **mismo archivo H2** que la base de desarrollo. Los tests pueden ensuciar
los datos, y los datos pueden hacer fallar los tests.

**Cómo se resuelve:** un `application-test.properties` con H2 en memoria y un perfil de test,
o Testcontainers para levantar un MySQL real en Docker durante la suite.

**Cómo lo cuento en 30 segundos:** "Los tests tienen que traer su propia base, limpia y
descartable. Si comparten la de desarrollo, dejan de ser repetibles: pasan o fallan según lo que
haya quedado de la última corrida."
