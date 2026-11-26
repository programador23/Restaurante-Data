# MesaLista API

Backend REST para administrar varios restaurantes. Requiere Java 17 y Maven; la base de datos de desarrollo es H2 en archivo y queda en `data/restaurantes`.

## Iniciar

Desde esta carpeta:

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. El perfil local crea datos de ejemplo en H2 y un AdministradorMaster de desarrollo: `admin` / `AdminDemo2026!`. El front Vite local (`http://localhost:5173`) y el despliegue `https://restaurante-front-production.up.railway.app` tienen CORS habilitado para `/api/**`. Salvo el inicio de sesión, los endpoints requieren un JWT en `Authorization: Bearer <token>`.

## Docker con PostgreSQL

Requiere Docker Desktop iniciado. Desde esta carpeta, levanta PostgreSQL y la API:

```bash
docker compose up --build -d
docker compose ps
```

La API queda en `http://localhost:8082` y PostgreSQL en `localhost:5432` (solo accesible desde este equipo). En el perfil Docker se crea automáticamente una base `mesalista`, datos de demostración y el usuario inicial AdministradorMaster. El volumen `postgres_data` conserva la información al reiniciar los contenedores.

Credenciales iniciales de desarrollo: `admin` / `AdminDemo2026!`. Cambia `DEMO_ADMIN_PASSWORD` y `JWT_SECRET` en `docker-compose.yml` antes de iniciar un entorno compartido o productivo; estos valores del Compose son solo para desarrollo local.

### App Android para garzones

En un emulador Android, la app apunta por defecto a `http://10.0.2.2:8082/api/`, que permite acceder a la API del equipo anfitrión. En Android 17 (API 37), acepta el permiso de red local cuando la app lo solicite. Inicia sesión con un usuario cuyo rol sea `GARZON`.

Para probar desde un teléfono físico, conecta el teléfono y el equipo a la misma red local y expón temporalmente la API en esa red. En PowerShell:

```powershell
$env:API_BIND_ADDRESS = "0.0.0.0"
docker compose up --build -d
```

En Android Studio, configura la IP local del equipo en **Gradle project properties** como `restaurantApiUrl`, por ejemplo `http://192.168.1.20:8082/api/`. También puede pasarse al compilar con `-PrestaurantApiUrl=http://192.168.1.20:8082/api/`. Permite el puerto 8082 en el firewall únicamente para la red local. Al omitir `API_BIND_ADDRESS`, Docker vuelve a enlazar la API a `127.0.0.1`. Esta configuración usa HTTP y es solo para desarrollo; despliegues reales deben usar HTTPS.

Iniciar sesión y guardar el token en PowerShell:

```powershell
$login = Invoke-RestMethod -Method Post http://localhost:8082/api/auth/login `
  -ContentType "application/json" `
  -Body '{"nombreUsuario":"admin","contrasena":"AdminDemo2026!"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

Prueba los datos iniciales en PowerShell:

```powershell
Invoke-RestMethod http://localhost:8082/api/restaurantes -Headers $headers
Invoke-RestMethod http://localhost:8082/api/restaurantes/1/menus -Headers $headers
Invoke-RestMethod http://localhost:8082/api/menus/1/items -Headers $headers
```

Crear un pedido para la mesa 4 con los dos productos de muestra:

```powershell
curl.exe -X POST http://localhost:8082/api/restaurantes/1/pedidos `
  -H "Authorization: Bearer $($login.accessToken)" `
  -H "Content-Type: application/json" `
  -d '{"numeroMesa":4,"descripcion":"Prueba Docker","items":[{"idItemRestaurante":1,"cantidad":2},{"idItemRestaurante":2,"cantidad":1}]}'
```

Detener los contenedores sin borrar la base:

```bash
docker compose down
```

Para borrar también los datos persistidos, ejecuta `docker compose down -v`; esto elimina de forma permanente la base de desarrollo.

Las credenciales `mesalista/mesalista_dev` del Compose son solo para desarrollo local y no deben usarse en producción.

## Roles y autenticación

El endpoint público `POST /api/auth/login` recibe `{"nombreUsuario":"admin","contrasena":"..."}` y devuelve un JWT válido por una hora. `GET /api/auth/me` devuelve la identidad asociada al token. Las contraseñas se guardan con BCrypt; nunca se devuelven en las respuestas.

| Rol | Permisos |
| --- | --- |
| `ADMINISTRADOR_MASTER` | Administra todos los restaurantes y usuarios; crea cuentas globales y asignadas a restaurantes. |
| `ADMINISTRADOR` | Administra menús, productos, pedidos y usuarios de su restaurante. Solo puede crear/gestionar usuarios con roles `RESTAURANTE` y `GARZON`. |
| `RESTAURANTE` | Administra menús, productos y pedidos de su restaurante. |
| `GARZON` | Consulta y gestiona pedidos de su restaurante; al crear pedidos se asigna a sí mismo. |

`POST /api/restaurantes/{idRestaurante}/usuarios` crea usuarios del restaurante. `GET`, `PUT` y `DELETE` en esa misma ruta gestionan sus cuentas. `ADMINISTRADOR_MASTER` usa `/api/usuarios` para listar/crear usuarios globalmente; para crear ese rol, omite `idRestaurante`. Los tokens se validan contra el usuario actual, por lo que eliminar o cambiar el rol de una cuenta invalida sus tokens anteriores.

## Recursos y operaciones

| Recurso | Rutas |
| --- | --- |
| Autenticación | `POST /api/auth/login`, `GET /api/auth/me` |
| Usuarios | `/api/usuarios` (solo AdministradorMaster) y `/api/restaurantes/{idRestaurante}/usuarios` (AdministradorMaster o Administrador) |
| Restaurantes | `/api/restaurantes` (POST, GET con `?q=`, GET/PUT/DELETE `/{idRestaurante}`) |
| Plantilla de comprobante | `GET /api/restaurantes/{idRestaurante}/plantilla-ticket` para todos los roles con acceso; `PUT` solo para Administrador Master, Administrador y Restaurante |
| Menús | `/api/restaurantes/{idRestaurante}/menus` (un único menú por restaurante; POST duplicado responde 409, GET con `?q=`, GET/PUT/DELETE `/{idMenu}`) |
| Productos | `/api/menus/{idMenu}/items` (POST, GET con `?q=`, GET/PUT/DELETE `/{idItemRestaurante}`) |
| Pedidos | `/api/restaurantes/{idRestaurante}/pedidos` (POST, GET con `?estado=` y/o `?numeroMesa=`, GET/PUT/DELETE `/{idPedido}`) |

Las operaciones de menú y pedidos se validan contra el restaurante de la ruta. Los productos de un pedido deben pertenecer a ese restaurante y estar disponibles. El total se calcula en el servidor y cada línea conserva el nombre y el precio vigentes al crear el pedido. Eliminar un restaurante elimina también sus menús, productos, pedidos y usuarios asociados.

`POST /api/restaurantes/{idRestaurante}/pedidos/aceptar-mas-antiguo` acepta el pedido nuevo más antiguo; devuelve 204 si no hay pedidos nuevos. `POST /api/restaurantes/{idRestaurante}/pedidos/{idPedido}/aceptar` acepta un pedido nuevo concreto y devuelve 409 si ya cambió de estado. Estas operaciones requieren acceso al restaurante; el temporizador y la impresión se ejecutan en el navegador, no en el servidor.

`PUT /api/restaurantes/{idRestaurante}/pedidos/{idPedido}/items` modifica productos de un pedido en preparación. Recibe `itemsOriginales` y `items`, ambas listas de `{idItemRestaurante, cantidad}`; devuelve 409 si el estado o los productos originales cambiaron. Los productos conservados mantienen su nombre y precio históricos; los nuevos deben estar disponibles. Si hubo cambios, la respuesta indica `reimpresionCocinaPendiente` y `revisionCocina`. `POST /api/restaurantes/{idRestaurante}/pedidos/{idPedido}/reimpresion-cocina` recibe `revision` para registrar el envío de esa versión a impresión, sin confirmar el resultado físico; una versión obsoleta devuelve 409. `POST /api/restaurantes/{idRestaurante}/pedidos/{idPedido}/avanzar` recibe `estadoActual` y avanza desde preparación o listo, sin reemplazar los productos. `DELETE /api/restaurantes/{idRestaurante}/pedidos/{idPedido}` solo elimina pedidos en preparación.

La plantilla de comprobante se guarda por restaurante. `GET` devuelve `encabezado`, `contenido` y `pie` como `null` si aún no hay plantilla; `PUT` recibe esos tres campos de texto. El contenido debe incluir `{{mesa}}`, `{{productos}}` y `{{total}}`; también se admiten `{{restaurante}}` y `{{fecha}}`. La cuenta impresa es no fiscal y no registra el pago ni sustituye una boleta electrónica.

## Ejemplos

Crear restaurante:

```json
{
  "nombre": "La Buena Mesa",
  "categoria": "Restaurante"
}
```

Crear un usuario asignado a un restaurante (solo AdministradorMaster):

```json
{
  "idRestaurante": 1,
  "nombre": "Ana",
  "apellido": "Pérez",
  "nombreUsuario": "ana.admin",
  "contrasena": "cambia-esta-clave",
  "rol": "ADMINISTRADOR"
}
```

Para crear un `ADMINISTRADOR_MASTER`, envía `idRestaurante: null`. En las rutas de usuarios del restaurante, el ID se toma de la URL y el rol permitido depende del usuario autenticado.

Crear menú:

```json
{
  "nombre": "Carta principal"
}
```

Crear producto:

```json
{
  "nombre": "Hamburguesa de la casa",
  "descripcion": "Vacuno, queso y cebolla caramelizada",
  "valor": 10990,
  "categoria": "Platos principales",
  "imagen": "https://ejemplo.cl/hamburguesa.jpg",
  "disponible": true
}
```

Crear pedido con varios productos:

```json
{
  "numeroMesa": 4,
  "idGarzon": null,
  "descripcion": "",
  "estado": "NUEVO",
  "items": [
    { "idItemRestaurante": 1, "cantidad": 2 },
    { "idItemRestaurante": 2, "cantidad": 1 }
  ]
}
```

Los estados válidos son `NUEVO`, `EN_PREPARACION`, `LISTO_PARA_SERVIR`, `ENTREGADO` y `PAGADO`. `PAGADO` es el estado final; la interfaz solicita confirmación del total antes de finalizar los pedidos entregados de una mesa. Los IDs se generan en el servidor. Las respuestas de productos usan `idItemRestaurante`, `nombre`, `descripcion`, `valor`, `categoria`, `imagen` y `disponible`; el front de muestra usa `id`, `name`, `description`, `price`, `category`, `image` y `available`. Su adaptador HTTP debe traducir esos nombres y enviar el `idMenu`.

Para cobrar una mesa en conjunto, usa `POST /api/restaurantes/{idRestaurante}/pedidos/mesas/{numeroMesa}/pago` con `{"idPedidos":[...],"totalConfirmado":18000,"metodoPago":"TARJETA"}`. La API valida que la lista y el total correspondan exactamente a los pedidos entregados pendientes de pago de esa mesa, los marca como pagados en una sola transacción y responde con los pedidos y un total único. Si la lista o el total quedaron desactualizados, responde `409` sin modificar pedidos.

El pago requiere `metodoPago`: `EFECTIVO` o `TARJETA` (débito o crédito). Los valores anteriores `DEBITO` y `CREDITO` siguen siendo válidos para preservar la compatibilidad y los pagos históricos. Solo los pagos en efectivo se suman al efectivo de caja.

## Modelo

- `Restaurante`: `idRestaurante`, `nombre`, `categoria`.
- `Menu`: `idMenu`, relación única con restaurante y `nombre`; cada restaurante puede tener un solo menú y la respuesta incluye `idRestaurante` y `nombreRestaurante`.
- `ItemRestaurante`: `idItemRestaurante`, relación con menú, `nombre`, `descripcion`, `valor`, además de categoría, imagen y disponibilidad para el catálogo del front.
- `Usuario`: `idUsuario`, restaurante opcional (para AdministradorMaster), nombre, apellido, nombre de usuario, hash de contraseña y rol (`ADMINISTRADOR`, `RESTAURANTE`, `GARZON`, `ADMINISTRADOR_MASTER`).
- `Pedido`: restaurante, mesa, garzón, descripción, estado y varias líneas con cantidad y precio histórico.

La API valida el rol y el restaurante asociado en cada petición autenticada. Configura secretos y contraseñas propios antes de exponerla fuera del entorno local.

## Pruebas

```bash
mvn test
```
# Restaurante-Data
