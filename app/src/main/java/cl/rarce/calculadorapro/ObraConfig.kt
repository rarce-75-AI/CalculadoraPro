package cl.rarce.calculadorapro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import java.util.Locale

val categoriasObra = listOf(
    "Cerámicas y pisos", "Hormigón y áridos", "Tabiquería", "Maderas",
    "Sanitarios y grifería", "Cocinas y lavaplatos", "Techumbres",
    "Electricidad", "Pinturas", "Instalaciones", "Otros"
)
val tiposTrabajo = listOf(
    "Radier", "Piso cerámico", "Piso flotante", "Tabique", "Muro",
    "Techumbre", "Baño", "Cocina", "Otro trabajo"
)

private fun numObra(s: String): Double? =
    s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
private fun mostrarObra(v: Double): String = String.format(Locale("es", "CL"), "%.2f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoObraScreen(
    precios: MutableMap<String, Int>,
    categorias: MutableMap<String, String>,
    unidades: MutableMap<String, String>
) {
    var query by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf("Todas") }
    var dialog by remember { mutableStateOf(false) }
    var original by remember { mutableStateOf<String?>(null) }
    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Otros") }
    var unidad by remember { mutableStateOf("unidad") }
    var precio by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    var expand by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    fun editar(key: String?) {
        original = key
        nombre = key ?: ""
        categoria = if (key == null) "Otros" else categorias[key] ?: "Otros"
        unidad = if (key == null) "unidad" else unidades[key] ?: "unidad"
        precio = if (key == null) "" else (precios[key] ?: 0).toString()
        aviso = ""
        dialog = true
    }
    val encontrados = precios.keys.filter { key ->
        (filtro == "Todas" || categorias[key] == filtro) &&
            (query.isBlank() || key.contains(query, ignoreCase = true) ||
                (categorias[key] ?: "").contains(query, ignoreCase = true))
    }.sorted()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Materiales y precios", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Text("Crea tu lista: grifería, lavamanos, lavaplatos, sanitarios, pisos y lo que necesites.",
            fontSize = 12.sp, color = TextMuted)
        Button(onClick = { editar(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Agregar producto o servicio")
        }
        OutlinedTextField(value = query, onValueChange = { query = it },
            label = { Text("Buscar material o categoría") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (listOf("Todas") + categoriasObra).forEach { c ->
                FilterChip(selected = filtro == c, onClick = { filtro = c }, label = { Text(c) })
            }
        }
        Text(encontrados.size.toString() + " productos · toca uno para editar", color = TextMuted, fontSize = 11.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(encontrados, key = { it }) { key ->
                Card(onClick = { editar(key) }, colors = CardDefaults.cardColors(containerColor = BgCard),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(key, color = TextMain, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text((categorias[key] ?: "Otros") + " · " + (unidades[key] ?: "unidad"),
                                color = TextMuted, fontSize = 11.sp)
                        }
                        Text(formatoCLP(precios[key] ?: 0), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    if (dialog) {
        AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Ficha de precio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ExposedDropdownMenuBox(expanded = expand, onExpandedChange = { expand = it }) {
                        OutlinedTextField(value = categoria, onValueChange = {}, readOnly = true,
                            label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expand) })
                        ExposedDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                            categoriasObra.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = { categoria = c; expand = false })
                            }
                        }
                    }
                    OutlinedTextField(value = unidad, onValueChange = { unidad = it },
                        label = { Text("Unidad: m², m³, saco, unidad, ml…") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = precio, onValueChange = { precio = it },
                        label = { Text("Precio unitario CLP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (aviso.isNotEmpty()) Text(aviso, color = AccentRed)
                    if (original != null) TextButton(onClick = { confirmarEliminar = true }) {
                        Text("Eliminar del catálogo", color = AccentRed)
                    }
                }
            }, confirmButton = {
                Button(onClick = {
                    val n = nombre.trim()
                    val p = precio.toIntOrNull()
                    aviso = when {
                        n.isBlank() || unidad.isBlank() -> "Nombre y unidad obligatorios."
                        p == null || p < 0 -> "Precio inválido. Usa 0 si no hay precio."
                        n != original && precios.containsKey(n) -> "El artículo ya existe."
                        else -> {
                            original?.takeIf { it != n }?.let {
                                precios.remove(it); categorias.remove(it); unidades.remove(it)
                            }
                            precios[n] = p!!
                            categorias[n] = categoria
                            unidades[n] = unidad.trim()
                            dialog = false
                            ""
                        }
                    }
                }) { Text("Guardar") }
            }, dismissButton = {
                TextButton(onClick = { dialog = false }) { Text("Cancelar") }
            })
    }
    if (confirmarEliminar) {
        AlertDialog(onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar producto") },
            text = { Text("Lo quita del catálogo, sin borrar partidas ya agregadas al presupuesto.") },
            confirmButton = { Button(onClick = {
                original?.let { precios.remove(it); categorias.remove(it); unidades.remove(it) }
                confirmarEliminar = false; dialog = false
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguradorObraScreen(
    precios: Map<String, Int>,
    categorias: Map<String, String>,
    unidades: Map<String, String>,
    onAgregar: (List<MaterialItem>) -> Unit,
    onCatalogo: () -> Unit
) {
    var trabajo by remember { mutableStateOf("Piso cerámico") }
    var largo by remember { mutableStateOf("") }
    var ancho by remember { mutableStateOf("") }
    var espesor by remember { mutableStateOf("10") }
    var merma by remember { mutableStateOf("0") }
    var buscar by remember { mutableStateOf("") }
    var filtroElegidos by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var solucion by remember { mutableStateOf("") }
    var consumos by remember { mutableStateOf(mapOf<String, String>()) }

    val opciones = when (trabajo) {
        "Radier" -> listOf("Hormigón simple", "Con malla", "Sobre base compactada")
        "Piso cerámico" -> listOf("Sobre piso existente", "Con nivelación", "Con impermeabilización", "Con radier nuevo")
        "Piso flotante" -> listOf("Sobre piso existente", "Con manta aislante", "Con nivelación")
        "Tabique" -> listOf("Estructura de madera", "Estructura Metalcon", "Otra estructura")
        "Muro" -> listOf("Ladrillo o bloque", "Hormigón", "Madera", "Otra solución")
        "Techumbre" -> listOf("Plancha metálica", "Tejas", "Otra cubierta")
        else -> listOf("Definido por usuario")
    }
    LaunchedEffect(trabajo) { solucion = opciones.first(); consumos = emptyMap() }
    val l = numObra(largo) ?: 0.0
    val a = numObra(ancho) ?: 0.0
    val area = l * a
    val volumen = area * (numObra(espesor) ?: 0.0) / 100.0
    val esVolumen = trabajo == "Radier" || (trabajo == "Piso cerámico" && solucion == "Con radier nuevo")
    val valido = l > 0.0 && a > 0.0 && l <= 1000.0 && a <= 1000.0
    val desperdicio = (numObra(merma) ?: 0.0).coerceIn(0.0, 100.0)
    val partidas = consumos.mapNotNull { (nombre, factorTexto) ->
        val f = numObra(factorTexto)
        val base = if ((unidades[nombre] ?: "").contains("m³") && esVolumen) volumen else area
        val cantidad = if (f != null && f > 0 && valido) ceil(base * f * (1 + desperdicio / 100)).toLong() else 0L
        val precio = precios[nombre] ?: 0
        if (cantidad <= 0 || cantidad > Int.MAX_VALUE ||
            cantidad * precio.toLong() > Int.MAX_VALUE) null
        else MaterialItem(
            trabajo + " / " + solucion + " / " + nombre,
            cantidad.toString() + " " + (unidades[nombre] ?: "unidad"),
            precio, (cantidad * precio).toInt()
        )
    }
    val total = partidas.sumOf { it.total.toLong() }
    val visibles = precios.keys.filter { key ->
        (buscar.isBlank() || key.contains(buscar, true) ||
            (categorias[key] ?: "").contains(buscar, true)) &&
            (!filtroElegidos || consumos.containsKey(key))
    }.sorted()
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Configurar trabajo", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Elige sistema, medidas y materiales. La app no decide por ti qué capas colocar.",
                fontSize = 12.sp, color = TextMuted)
        }
        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(value = trabajo, onValueChange = {}, readOnly = true,
                    label = { Text("Tipo de trabajo") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) })
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    tiposTrabajo.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { trabajo = t; expanded = false })
                    }
                }
            }
        }
        item {
            Text("Solución constructiva", fontWeight = FontWeight.Bold, color = TextMain)
            opciones.forEach { op ->
                Row(Modifier.fillMaxWidth().clickable { solucion = op },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = solucion == op, onClick = { solucion = op })
                    Text(op, color = TextMain, fontSize = 13.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = largo, onValueChange = { largo = it },
                    label = { Text("Largo (m)") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = ancho, onValueChange = { ancho = it },
                    label = { Text(if (trabajo == "Muro" || trabajo == "Tabique") "Altura (m)" else "Ancho (m)") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            if (esVolumen) {
                OutlinedTextField(value = espesor, onValueChange = { espesor = it },
                    label = { Text("Espesor hormigón (cm)") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            OutlinedTextField(value = merma, onValueChange = { merma = it },
                label = { Text("Merma estimada (%) — editable") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (valido) Text(
                "Superficie: " + mostrarObra(area) + " m²" +
                    (if (esVolumen) " · Hormigón: " + mostrarObra(volumen) + " m³" else ""),
                color = AccentGreen, fontWeight = FontWeight.Bold)
            else Text("Ingresa medidas reales para calcular.", color = AccentAmber)
        }
        item {
            HorizontalDivider(color = BorderSubtle)
            Text("Selecciona materiales", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Indica el consumo o cantidad por m². Si el producto se vende en m³ y hay hormigón, usa factor por m³. Los rendimientos son tuyos, no valores ocultos.",
                fontSize = 12.sp, color = TextMuted)
            OutlinedButton(onClick = onCatalogo, modifier = Modifier.fillMaxWidth()) {
                Text("+ Agregar materiales a mi catálogo")
            }
            OutlinedTextField(value = buscar, onValueChange = { buscar = it },
                label = { Text("Buscar productos o categorías") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            FilterChip(selected = filtroElegidos, onClick = { filtroElegidos = !filtroElegidos },
                label = { Text("Ver solo los seleccionados") })
        }
        items(visibles, key = { it }) { key ->
            val elegido = consumos.containsKey(key)
            Card(colors = CardDefaults.cardColors(containerColor = BgCard)) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = elegido, onCheckedChange = { checked ->
                            consumos = if (checked) consumos + (key to "") else consumos - key
                        })
                        Column(Modifier.weight(1f)) {
                            Text(key, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text((categorias[key] ?: "Otros") + " · " +
                                (unidades[key] ?: "unidad") + " · " + formatoCLP(precios[key] ?: 0),
                                color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    if (elegido) {
                        OutlinedTextField(value = consumos[key] ?: "",
                            onValueChange = { consumos = consumos + (key to it) },
                            label = { Text(if ((unidades[key] ?: "").contains("m³") && esVolumen)
                                "Factor por m³" else "Unidades de compra por m²") },
                            supportingText = { Text("Consumo mayor que cero. Se redondea a unidades de compra completas.") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                }
            }
        }
        item {
            if (precios.isEmpty()) Text("No hay productos en tu catálogo. Agrégalos en Precios.", color = AccentAmber)
            Text("Materiales válidos: " + partidas.size.toString() +
                " · Subtotal: " + formatoCLP(total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                color = AccentGreen, fontWeight = FontWeight.Bold)
            if (consumos.isNotEmpty() && partidas.size != consumos.size)
                Text("Revisa consumos y medidas de todos los artículos seleccionados.", color = AccentAmber)
            Button(onClick = { onAgregar(partidas) }, modifier = Modifier.fillMaxWidth(),
                enabled = valido && partidas.isNotEmpty() &&
                    partidas.size == consumos.size && total <= Int.MAX_VALUE) {
                Text("Agregar partida al presupuesto")
            }
            Text("Cubicación orientativa. Verifica espesores, formato comercial y consumo del fabricante.",
                fontSize = 11.sp, color = TextMuted)
        }
    }
}
