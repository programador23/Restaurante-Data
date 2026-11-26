package cl.mesalista.restaurante.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.model.Rol;
import cl.mesalista.restaurante.model.Usuario;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:restaurante-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestauranteApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private RestauranteRepository restaurantes;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String masterToken;

    @BeforeEach
    void iniciarSesionComoAdministradorMaster() throws Exception {
        String username = "master-" + UUID.randomUUID();
        usuarios.save(new Usuario(null, "Admin", "Master", username,
                passwordEncoder.encode("MasterTest123!"), Rol.ADMINISTRADOR_MASTER));
        masterToken = iniciarSesion(username, "MasterTest123!");
    }

    @Test
    void reimpresionSoloSeHabilitaTrasModificarYNoBorraCambiosPosteriores() throws Exception {
        long restaurante = crearRestaurante("Reimpresión cocina");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long pedido = crearPedido(restaurante, 1, item, 1);
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/aceptar", restaurante, pedido)
                        .with(masterAuth()))
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(false)));
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":1}],
                                 "items":[{"idItemRestaurante":%d,"cantidad":1}]}
                                """.formatted(item, item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(false)));
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":1}],
                                 "items":[{"idItemRestaurante":%d,"cantidad":2}]}
                                """.formatted(item, item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionCocina", is(1)))
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(true)));
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido).with(masterAuth()))
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(true)));
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":2}],
                                 "items":[{"idItemRestaurante":%d,"cantidad":3}]}
                                """.formatted(item, item)))
                .andExpect(jsonPath("$.revisionCocina", is(2)));
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/reimpresion-cocina", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":1}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido).with(masterAuth()))
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(true)));
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/reimpresion-cocina", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(false)));
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido).with(masterAuth()))
                .andExpect(jsonPath("$.reimpresionCocinaPendiente", is(false)));
    }

    @Test
    void editarProductosEnPreparacionConservaPreciosYDetectaCambios() throws Exception {
        long restaurante = crearRestaurante("Editar pedido");
        long menu = crearMenu(restaurante, "Carta");
        long original = crearItem(menu, "Plato", "2500");
        long agregado = crearItem(menu, "Jugo", "1200");
        long pedido = crearPedido(restaurante, 1, original, 1);
        String cambios = """
                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":1}],
                 "items":[{"idItemRestaurante":%d,"cantidad":2},{"idItemRestaurante":%d,"cantidad":1}]}
                """.formatted(original, original, agregado);

        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(cambios))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/aceptar", restaurante, pedido)
                        .with(masterAuth())).andExpect(status().isOk());
        mockMvc.perform(put("/api/menus/{menu}/items/{item}", menu, original)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Plato modificado","descripcion":"Descripción","valor":8000,"disponible":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].nombre", is("Plato")))
                .andExpect(jsonPath("$.items[0].valorUnitario").value(2500))
                .andExpect(jsonPath("$.total").value(6200));
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(cambios))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/menus/{menu}/items/{item}", menu, original)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Plato modificado","descripcion":"Descripción","valor":8000,"disponible":false}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":2},{"idItemRestaurante":%d,"cantidad":1}],
                                 "items":[{"idItemRestaurante":%d,"cantidad":3},{"idItemRestaurante":%d,"cantidad":1}]}
                                """.formatted(original, agregado, original, agregado)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":2},{"idItemRestaurante":%d,"cantidad":1}],
                                 "items":[{"idItemRestaurante":%d,"cantidad":1},{"idItemRestaurante":%d,"cantidad":1}]}
                                """.formatted(original, agregado, agregado, agregado)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemsOriginales":[{"idItemRestaurante":%d,"cantidad":2},{"idItemRestaurante":%d,"cantidad":1}],
                                 "items":[{"idItemRestaurante":%d,"cantidad":1}]}
                                """.formatted(original, agregado, agregado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.total").value(1200));
        mockMvc.perform(get("/api/menus/{menu}/items/{item}", menu, original).with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idItemRestaurante").value(original));
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/avanzar", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoActual\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("LISTO_PARA_SERVIR")))
                .andExpect(jsonPath("$.total").value(1200));
        mockMvc.perform(delete("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido)
                        .with(masterAuth())).andExpect(status().isConflict());
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}/items", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(cambios))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/avanzar", restaurante, pedido)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoActual\":\"EN_PREPARACION\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarSoloPedidosEnPreparacionYDelRestauranteAutorizado() throws Exception {
        long restaurante = crearRestaurante("Eliminar pedido");
        long otroRestaurante = crearRestaurante("Otro restaurante");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long pedido = crearPedido(restaurante, 1, item, 1);
        String username = "eliminar-" + UUID.randomUUID();
        usuarios.save(new Usuario(restaurantes.findById(otroRestaurante).orElseThrow(), "Ana", "Garzón",
                username, passwordEncoder.encode("GarzonTest123!"), Rol.GARZON));
        String token = iniciarSesion(username, "GarzonTest123!");
        mockMvc.perform(delete("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido)
                        .with(masterAuth())).andExpect(status().isConflict());
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/aceptar", restaurante, pedido)
                        .with(masterAuth())).andExpect(status().isOk());
        mockMvc.perform(delete("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido)
                        .with(masterAuth())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedido)
                        .with(masterAuth())).andExpect(status().isNotFound());
    }

    @Test
    void aceptarMasAntiguoRespetaElRestauranteYElOrdenDeLlegada() throws Exception {
        long restaurante = crearRestaurante("Aceptación automática");
        long otroRestaurante = crearRestaurante("Otro restaurante automático");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long primerPedido = crearPedido(restaurante, 1, item, 1);
        long segundoPedido = crearPedido(restaurante, 2, item, 1);
        String username = "auto-" + UUID.randomUUID();
        usuarios.save(new Usuario(restaurantes.findById(restaurante).orElseThrow(), "Ana", "Garzón",
                username, passwordEncoder.encode("GarzonTest123!"), Rol.GARZON));
        String token = iniciarSesion(username, "GarzonTest123!");

        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/aceptar-mas-antiguo", otroRestaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/aceptar-mas-antiguo", restaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(primerPedido))
                .andExpect(jsonPath("$.estado", is("EN_PREPARACION")));
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/aceptar", restaurante, primerPedido)
                        .with(masterAuth()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/aceptar-mas-antiguo", restaurante)
                        .with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(segundoPedido));
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/aceptar-mas-antiguo", restaurante)
                        .with(masterAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aceptacionManualNoAceptaDosVecesElMismoPedido() throws Exception {
        long restaurante = crearRestaurante("Aceptación manual");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long pedido = crearPedido(restaurante, 1, item, 1);
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/aceptar", restaurante, pedido)
                        .with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("EN_PREPARACION")));
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/{pedido}/aceptar", restaurante, pedido)
                        .with(masterAuth()))
                .andExpect(status().isConflict());
    }

    @Test
    void aceptacionesSimultaneasNoRepitenPedidos() throws Exception {
        long restaurante = crearRestaurante("Aceptación simultánea");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long primero = crearPedido(restaurante, 1, item, 1);
        long segundo = crearPedido(restaurante, 2, item, 1);
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var tarea = (java.util.concurrent.Callable<MvcResult>) () -> {
                inicio.await();
                return mockMvc.perform(post("/api/restaurantes/{id}/pedidos/aceptar-mas-antiguo", restaurante)
                                .with(masterAuth()))
                        .andExpect(status().isOk()).andReturn();
            };
            var primera = executor.submit(tarea);
            var segunda = executor.submit(tarea);
            inicio.countDown();
            assertEquals(Set.of(primero, segundo),
                    Set.of(id(primera.get(10, TimeUnit.SECONDS), "idPedido"),
                            id(segunda.get(10, TimeUnit.SECONDS), "idPedido")));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void pedidoPuedeTenerVariosProductosYCalculaTotal() throws Exception {
        long restaurante = crearRestaurante("MesaLista pedido");
        long menu = crearMenu(restaurante, "Carta");
        long primerItem = crearItem(menu, "Hamburguesa", "10990");
        long segundoItem = crearItem(menu, "Limonada", "3490");

        MvcResult pedidoCreado = mockMvc.perform(post("/api/restaurantes/{id}/pedidos", restaurante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content("""
                                {
                                  "numeroMesa": 4,
                                  "items": [
                                    {"idItemRestaurante": %d, "cantidad": 2},
                                    {"idItemRestaurante": %d, "cantidad": 1}
                                  ]
                                }
                                """.formatted(primerItem, segundoItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()", is(2)))
                .andExpect(jsonPath("$.total").value(25470.0))
                .andReturn();
        long idPedido = id(pedidoCreado, "idPedido");

        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{idPedido}", restaurante, idPedido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content("""
                                {
                                  "numeroMesa": 4,
                                  "estado": "PAGADO",
                                  "items": [
                                    {"idItemRestaurante": %d, "cantidad": 2},
                                    {"idItemRestaurante": %d, "cantidad": 1}
                                  ]
                                }
                                """.formatted(primerItem, segundoItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("PAGADO")));
    }

    @Test
    void pagoDeMesaFinalizaJuntosLosPedidosEntregadosConUnSoloTotal() throws Exception {
        long restaurante = crearRestaurante("MesaLista pago conjunto");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long primerPedido = crearPedido(restaurante, 5, item, 2);
        long segundoPedido = crearPedido(restaurante, 5, item, 1);
        long pedidoActivo = crearPedido(restaurante, 5, item, 1);
        marcarPedidoEntregado(restaurante, primerPedido, item, 5, 2);
        marcarPedidoEntregado(restaurante, segundoPedido, item, 5, 1);

        var listaIncompleta = objectMapper.createObjectNode();
        listaIncompleta.putArray("idPedidos").add(primerPedido);
        listaIncompleta.put("totalConfirmado", 7500);
        listaIncompleta.put("metodoPago", "EFECTIVO");
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/mesas/{mesa}/pago", restaurante, 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(listaIncompleta.toString()))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, primerPedido)
                        .with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ENTREGADO")));

        var pagoConjunto = objectMapper.createObjectNode();
        pagoConjunto.putArray("idPedidos").add(primerPedido).add(segundoPedido);
        pagoConjunto.put("totalConfirmado", 7000);
        pagoConjunto.put("metodoPago", "CREDITO");
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/mesas/{mesa}/pago", restaurante, 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(pagoConjunto.toString()))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, segundoPedido)
                        .with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ENTREGADO")));

        pagoConjunto.put("totalConfirmado", 7500);
        pagoConjunto.put("metodoPago", "DEBITO");
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/mesas/{mesa}/pago", restaurante, 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(pagoConjunto.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroMesa", is(5)))
                .andExpect(jsonPath("$.pedidos.length()", is(2)))
                .andExpect(jsonPath("$.pedidos[0].estado", is("PAGADO")))
                .andExpect(jsonPath("$.pedidos[1].estado", is("PAGADO")))
                .andExpect(jsonPath("$.pedidos[0].metodoPago", is("DEBITO")))
                .andExpect(jsonPath("$.pedidos[1].metodoPago", is("DEBITO")))
                .andExpect(jsonPath("$.total").value(7500.0));

        mockMvc.perform(get("/api/restaurantes/{id}/pedidos/{pedido}", restaurante, pedidoActivo)
                        .with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("NUEVO")));
    }

    @Test
    void pagoConTarjetaSeRegistraSinSumarloAlEfectivoDeCaja() throws Exception {
        long restaurante = crearRestaurante("MesaLista tarjeta");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long pedido = crearPedido(restaurante, 5, item, 1);
        marcarPedidoEntregado(restaurante, pedido, item, 5, 1);
        mockMvc.perform(post("/api/restaurantes/{id}/caja/apertura", restaurante)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":1000}"))
                .andExpect(status().isOk());

        var pago = objectMapper.createObjectNode();
        pago.putArray("idPedidos").add(pedido);
        pago.put("totalConfirmado", 2500);
        pago.put("metodoPago", "TARJETA");
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/mesas/{mesa}/pago", restaurante, 5)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(pago.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidos[0].metodoPago", is("TARJETA")));
        mockMvc.perform(get("/api/restaurantes/{id}/caja", restaurante).with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.efectivoCobrado").value(0))
                .andExpect(jsonPath("$.totalCaja").value(1000));
    }

    @Test
    void pagoEnEfectivoSumaALaCajaYCierreConservaElTotal() throws Exception {
        long restaurante = crearRestaurante("MesaLista efectivo");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long pedido = crearPedido(restaurante, 5, item, 2);
        marcarPedidoEntregado(restaurante, pedido, item, 5, 2);
        mockMvc.perform(post("/api/restaurantes/{id}/caja/apertura", restaurante)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":1000}"))
                .andExpect(status().isOk());

        var pago = objectMapper.createObjectNode();
        pago.putArray("idPedidos").add(pedido);
        pago.put("totalConfirmado", 5000);
        pago.put("metodoPago", "EFECTIVO");
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos/mesas/{mesa}/pago", restaurante, 5)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(pago.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/restaurantes/{id}/caja", restaurante).with(masterAuth()))
                .andExpect(jsonPath("$.efectivoCobrado").value(5000))
                .andExpect(jsonPath("$.totalCaja").value(6000));
        mockMvc.perform(post("/api/restaurantes/{id}/caja/cierre", restaurante).with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.efectivoCobrado").value(5000));
    }

    @Test
    void aperturasSimultaneasDeCajaSoloCreanUna() throws Exception {
        long restaurante = crearRestaurante("MesaLista caja simultánea");
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var tarea = (java.util.concurrent.Callable<Integer>) () -> {
                inicio.await();
                return mockMvc.perform(post("/api/restaurantes/{id}/caja/apertura", restaurante)
                                .with(masterAuth()).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":1000}"))
                        .andReturn().getResponse().getStatus();
            };
            var primera = executor.submit(tarea);
            var segunda = executor.submit(tarea);
            inicio.countDown();
            assertEquals(Set.of(200, 409),
                    Set.of(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS)));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void listadoDePedidosFiltraPorEstadoYMesaConOrdenEstable() throws Exception {
        long restaurante = crearRestaurante("MesaLista filtros");
        long menu = crearMenu(restaurante, "Carta");
        long item = crearItem(menu, "Plato", "2500");
        long mesa1 = crearPedido(restaurante, 1, item, 1);
        long mesa2 = crearPedido(restaurante, 2, item, 1);
        long otraMesa1 = crearPedido(restaurante, 1, item, 1);
        marcarPedidoEntregado(restaurante, mesa2, item, 2, 1);

        mockMvc.perform(get("/api/restaurantes/{id}/pedidos", restaurante).with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)))
                .andExpect(jsonPath("$[0].idPedido").value(mesa1))
                .andExpect(jsonPath("$[2].idPedido").value(otraMesa1))
                .andExpect(jsonPath("$[0].items[0].nombre", is("Plato")));
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos", restaurante)
                        .param("numeroMesa", "1").with(masterAuth()))
                .andExpect(jsonPath("$.length()", is(2)));
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos", restaurante)
                        .param("estado", "ENTREGADO").with(masterAuth()))
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].idPedido").value(mesa2));
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos", restaurante)
                        .param("estado", "ENTREGADO").param("numeroMesa", "1").with(masterAuth()))
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void pedidoRechazaProductosDeOtroRestaurante() throws Exception {
        long primero = crearRestaurante("MesaLista A");
        long segundo = crearRestaurante("MesaLista B");
        long menu = crearMenu(segundo, "Carta");
        long item = crearItem(menu, "Producto ajeno", "2500");

        mockMvc.perform(post("/api/restaurantes/{id}/pedidos", primero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content("""
                                {
                                  "numeroMesa": 1,
                                  "items": [{"idItemRestaurante": %d, "cantidad": 1}]
                                }
                                """.formatted(item)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recursosProtegidosRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/restaurantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plantillaDeTicketSeGuardaPorRestauranteYRespetaElAcceso() throws Exception {
        long restaurante = crearRestaurante("Plantilla A");
        long otroRestaurante = crearRestaurante("Plantilla B");
        String username = "ticket-" + UUID.randomUUID();
        usuarios.save(new Usuario(restaurantes.findById(restaurante).orElseThrow(), "Ana", "Equipo",
                username, passwordEncoder.encode("GarzonTest123!"), Rol.GARZON));
        String token = iniciarSesion(username, "GarzonTest123!");
        var plantilla = objectMapper.createObjectNode()
                .put("encabezado", "Mi restaurante")
                .put("contenido", "Mesa {{mesa}}\n{{productos}}\nTotal {{total}}")
                .put("pie", "Gracias");

        mockMvc.perform(get("/api/restaurantes/{id}/plantilla-ticket", restaurante))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", restaurante)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", restaurante)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encabezado", is("Mi restaurante")));
        mockMvc.perform(get("/api/restaurantes/{id}/plantilla-ticket", restaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", is("Mesa {{mesa}}\n{{productos}}\nTotal {{total}}")));
        for (Rol rol : new Rol[] { Rol.ADMINISTRADOR, Rol.RESTAURANTE }) {
            String nombreUsuario = "plantilla-" + UUID.randomUUID();
            usuarios.save(new Usuario(restaurantes.findById(restaurante).orElseThrow(), "Ana", "Equipo",
                    nombreUsuario, passwordEncoder.encode("PlantillaTest123!"), rol));
            String otroToken = iniciarSesion(nombreUsuario, "PlantillaTest123!");
            mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", restaurante)
                            .header("Authorization", "Bearer " + otroToken)
                            .contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", otroRestaurante)
                            .header("Authorization", "Bearer " + otroToken)
                            .contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/restaurantes/{id}/plantilla-ticket", otroRestaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", otroRestaurante)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/restaurantes/{id}/plantilla-ticket", otroRestaurante).with(masterAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").doesNotExist());

        plantilla.put("contenido", "Mesa {{mesa}} Total {{total}}");
        mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", restaurante)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                .andExpect(status().isBadRequest());
        plantilla.put("contenido", "{{mesa}} {{productos}} {{total}} {{desconocido}}");
        mockMvc.perform(put("/api/restaurantes/{id}/plantilla-ticket", restaurante)
                        .with(masterAuth()).contentType(MediaType.APPLICATION_JSON).content(plantilla.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/restaurantes/{id}/plantilla-ticket", restaurante).with(masterAuth()))
                .andExpect(jsonPath("$.contenido", is("Mesa {{mesa}}\n{{productos}}\nTotal {{total}}")));
    }

    @Test
    void restauranteNoPuedeTenerMasDeUnMenu() throws Exception {
        long restaurante = crearRestaurante("MesaLista menú único");
        crearMenu(restaurante, "Menú principal");

        mockMvc.perform(post("/api/restaurantes/{id}/menus", restaurante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(objectMapper.createObjectNode()
                                .put("nombre", "Menú duplicado").toString()))
                .andExpect(status().isConflict());
    }

    @Test
    void garzonSoloAccedeAPedidosDeSuRestaurante() throws Exception {
        long restaurante = crearRestaurante("MesaLista garzon");
        long otroRestaurante = crearRestaurante("MesaLista ajeno");
        long menu = crearMenu(restaurante, "Carta");
        long otroMenu = crearMenu(otroRestaurante, "Carta");
        long item = crearItem(menu, "Plato del día", "5000");
        String username = "garzon-" + UUID.randomUUID();
        usuarios.save(new Usuario(restaurantes.findById(restaurante).orElseThrow(), "Diego", "Muñoz",
                username, passwordEncoder.encode("GarzonTest123!"), Rol.GARZON));
        String token = iniciarSesion(username, "GarzonTest123!");

        mockMvc.perform(get("/api/restaurantes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos", restaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/restaurantes/{id}/pedidos", otroRestaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/restaurantes/{id}/menus", restaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/restaurantes/{id}/menus", otroRestaurante)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/menus/{id}/items", menu)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
        mockMvc.perform(get("/api/menus/{id}/items", otroMenu)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/menus/{id}/items", menu)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"No permitido","descripcion":"Prueba","valor":1000}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/restaurantes/{id}/pedidos", restaurante)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroMesa": 2,
                                  "items": [{"idItemRestaurante": %d, "cantidad": 1}]
                                }
                                """.formatted(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreGarzon", is("Diego Muñoz")));
    }

    @Test
    void administradorGestionaUsuariosPropiosYLosTokensSeInvalidanAlCambiarRol() throws Exception {
        long restaurante = crearRestaurante("MesaLista administrador");
        MvcResult creado = mockMvc.perform(post("/api/usuarios")
                        .with(masterAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("idRestaurante", restaurante)
                                .put("nombre", "Ana")
                                .put("apellido", "Admin")
                                .put("nombreUsuario", "admin-" + UUID.randomUUID())
                                .put("contrasena", "AdminTest123!")
                                .put("rol", Rol.ADMINISTRADOR.name()).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist())
                .andReturn();
        JsonNode admin = objectMapper.readTree(creado.getResponse().getContentAsString());
        String adminToken = iniciarSesion(admin.get("nombreUsuario").asText(), "AdminTest123!");

        mockMvc.perform(post("/api/restaurantes/{id}/usuarios", restaurante)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("nombre", "Diego")
                                .put("apellido", "Garzon")
                                .put("nombreUsuario", "garzon-" + UUID.randomUUID())
                                .put("contrasena", "GarzonTest123!")
                                .put("rol", Rol.GARZON.name()).toString()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/restaurantes/{id}/usuarios", restaurante)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/restaurantes/{id}/usuarios", restaurante)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("nombre", "Otro")
                                .put("apellido", "Admin")
                                .put("nombreUsuario", "admin-elevacion-" + UUID.randomUUID())
                                .put("contrasena", "AdminTest123!")
                                .put("rol", Rol.ADMINISTRADOR.name()).toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/usuarios/{id}", admin.get("idUsuario").asLong())
                        .with(masterAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("nombre", "Ana")
                                .put("apellido", "Admin")
                                .put("rol", Rol.RESTAURANTE.name()).toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/restaurantes/{id}/menus", restaurante)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }

    private long crearRestaurante(String nombre) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(objectMapper.createObjectNode()
                                .put("nombre", nombre)
                                .put("categoria", "Restaurante").toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "idRestaurante");
    }

    private long crearMenu(long idRestaurante, String nombre) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurantes/{id}/menus", idRestaurante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(objectMapper.createObjectNode().put("nombre", nombre).toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "idMenu");
    }

    private long crearItem(long idMenu, String nombre, String valor) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/menus/{id}/items", idMenu)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content(objectMapper.createObjectNode()
                                .put("nombre", nombre)
                                .put("descripcion", "Descripción")
                                .put("valor", new BigDecimal(valor))
                                .put("disponible", true).toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "idItemRestaurante");
    }

    private long crearPedido(long idRestaurante, int mesa, long idItem, int cantidad) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/restaurantes/{id}/pedidos", idRestaurante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content("""
                                {
                                  "numeroMesa": %d,
                                  "items": [{"idItemRestaurante": %d, "cantidad": %d}]
                                }
                                """.formatted(mesa, idItem, cantidad)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "idPedido");
    }

    private void marcarPedidoEntregado(long idRestaurante, long idPedido, long idItem,
                                       int mesa, int cantidad) throws Exception {
        mockMvc.perform(put("/api/restaurantes/{id}/pedidos/{pedido}", idRestaurante, idPedido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(masterAuth())
                        .content("""
                                {
                                  "numeroMesa": %d,
                                  "estado": "ENTREGADO",
                                  "items": [{"idItemRestaurante": %d, "cantidad": %d}]
                                }
                                """.formatted(mesa, idItem, cantidad)))
                .andExpect(status().isOk());
    }

    private long id(MvcResult result, String property) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get(property).asLong();
    }

    private String iniciarSesion(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("nombreUsuario", username)
                                .put("contrasena", password).toString()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private RequestPostProcessor masterAuth() {
        return request -> {
            request.addHeader("Authorization", "Bearer " + masterToken);
            return request;
        };
    }
}
