package cl.rarce.calculadorapro

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
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
    yaAgregado: (String) -> Boolean,
    onCatalogo: () -> Unit
) {
    // Borrador persistente: no se pierde al cambiar entre Trabajos, Precios y Presupuesto.
    val context = LocalContext.current
    val borradorPrefs = remember(context) {
        context.getSharedPreferences("calculo_obra_borrador_v1", Context.MODE_PRIVATE)
    }
    var trabajo by remember { mutableStateOf(borradorPrefs.getString("trabajo", "Piso cerámico") ?: "Piso cerámico") }
    var largo by remember { mutableStateOf(borradorPrefs.getString("largo", "") ?: "") }
    var ancho by remember { mutableStateOf(borradorPrefs.getString("ancho", "") ?: "") }
    var espesor by remember { mutableStateOf(borradorPrefs.getString("espesor", "10") ?: "10") }
    var merma by remember { mutableStateOf(borradorPrefs.getString("merma", "0") ?: "0") }
    var buscar by remember { mutableStateOf(borradorPrefs.getString("buscar", "") ?: "") }
    var filtroElegidos by remember { mutableStateOf(borradorPrefs.getBoolean("filtroElegidos", false)) }
    var expanded by remember { mutableStateOf(false) }
    var solucion by remember { mutableStateOf(borradorPrefs.getString("solucion", "Sobre piso existente") ?: "Sobre piso existente") }
    var consumos by remember {
        mutableStateOf(runCatching {
            val json = JSONObject(borradorPrefs.getString("consumos", "{}") ?: "{}")
            json.keys().asSequence().associateWith { json.optString(it, "") }
        }.getOrDefault(emptyMap<String, String>()))
    }
    // Dosificación orientativa editable, persistida en el borrador.
    var cementoKgM3 by remember { mutableStateOf(borradorPrefs.getString("cementoKgM3", "300") ?: "300") }
    var arenaM3M3 by remember { mutableStateOf(borradorPrefs.getString("arenaM3M3", "0,55") ?: "0,55") }
    var gravaM3M3 by remember { mutableStateOf(borradorPrefs.getString("gravaM3M3", "0,80") ?: "0,80") }
    var traslapeMalla by remember { mutableStateOf(borradorPrefs.getString("traslapeMalla", "10") ?: "10") }
    val incluirSugerido = remember {
        mutableStateMapOf<String, Boolean>().apply {
            runCatching {
                val json = JSONObject(borradorPrefs.getString("sugeridos", "{}") ?: "{}")
                json.keys().forEach { key -> this[key] = json.optBoolean(key, true) }
            }
        }
    }

    fun guardarBorrador() {
        val factores = JSONObject().apply { consumos.forEach { (k, v) -> put(k, v) } }
        val sugeridos = JSONObject().apply { incluirSugerido.forEach { (k, v) -> put(k, v) } }
        borradorPrefs.edit()
            .putString("trabajo", trabajo).putString("solucion", solucion)
            .putString("largo", largo).putString("ancho", ancho)
            .putString("espesor", espesor).putString("merma", merma)
            .putString("buscar", buscar).putBoolean("filtroElegidos", filtroElegidos)
            .putString("cementoKgM3", cementoKgM3).putString("arenaM3M3", arenaM3M3)
            .putString("gravaM3M3", gravaM3M3).putString("traslapeMalla", traslapeMalla)
            .putString("consumos", factores.toString()).putString("sugeridos", sugeridos.toString())
            .apply()
    }
    // El estado se conserva incluso al desmontarse esta pantalla por la navegación.
    SideEffect { guardarBorrador() }

    val opciones = when (trabajo) {
        "Radier" -> listOf("Hormigón simple", "Con malla", "Sobre base compactada")
        "Piso cerámico" -> listOf("Sobre piso existente", "Con nivelación", "Con impermeabilización", "Con radier nuevo")
        "Piso flotante" -> listOf("Sobre piso existente", "Con manta aislante", "Con nivelación")
        "Tabique" -> listOf("Estructura de madera", "Estructura Metalcon", "Otra estructura")
        "Muro" -> listOf("Ladrillo o bloque", "Hormigón", "Madera", "Otra solución")
        "Techumbre" -> listOf("Plancha metálica", "Tejas", "Otra cubierta")
        else -> listOf("Definido por usuario")
    }
    // No reiniciar el formulario al volver a Trabajos. Sólo cambiar solución si el usuario
    // seleccionó explícitamente un tipo distinto.
    val solucionActual = if (solucion in opciones) solucion else opciones.first()
    val l = numObra(largo) ?: 0.0
    val a = numObra(ancho) ?: 0.0
    val area = l * a
    val volumen = area * (numObra(espesor) ?: 0.0) / 100.0
    val esVolumen = trabajo == "Radier" || (trabajo == "Piso cerámico" && solucionActual == "Con radier nuevo")
    val valido = l > 0.0 && a > 0.0 && l <= 1000.0 && a <= 1000.0 &&
        (!esVolumen || (numObra(espesor) ?: 0.0) > 0.0)
    val desperdicio = (numObra(merma) ?: 0.0).coerceIn(0.0, 100.0)
    val partidas = consumos.mapNotNull { (nombre, factorTexto) ->
        val f = numObra(factorTexto)
        val base = if ((unidades[nombre] ?: "").contains("m³") && esVolumen) volumen else area
        val cantidad = if (f != null && f > 0 && valido) ceil(base * f * (1 + desperdicio / 100)).toLong() else 0L
        val precio = precios[nombre] ?: 0
        if (cantidad <= 0 || cantidad > Int.MAX_VALUE ||
            cantidad * precio.toLong() > Int.MAX_VALUE) null
        else MaterialItem(
            trabajo + " / " + solucionActual + " / " + nombre,
            cantidad.toString() + " " + (unidades[nombre] ?: "unidad"),
            precio, (cantidad * precio).toInt()
        )
    }
    // Radier: las cantidades se derivan de m³ de hormigón, nunca de sacos por m².
    // Parámetros de ejemplo editables: se deben verificar según diseño y suministro.
    val datosDosificacionValidos = listOf(cementoKgM3, arenaM3M3, gravaM3M3).all {
        (numObra(it) ?: 0.0) > 0.0
    } && numObra(traslapeMalla) != null
    val sugerenciasRadier = if (trabajo == "Radier" && valido && datosDosificacionValidos) {
        val volumenCompra = volumen * (1.0 + desperdicio / 100.0)
        val sacos = ceil(volumenCompra * (numObra(cementoKgM3) ?: 300.0) / 25.0)
        val arena = ceil(volumenCompra * (numObra(arenaM3M3) ?: 0.55) * 100.0) / 100.0
        val grava = ceil(volumenCompra * (numObra(gravaM3M3) ?: 0.80) * 100.0) / 100.0
        val malla = ceil(area * (1.0 + (numObra(traslapeMalla) ?: 10.0) / 100.0) * 100.0) / 100.0
        fun recomendado(nombre: String, cantidad: Double, unidad: String): MaterialItem {
            val p = precios[nombre] ?: 0
            val costo = ceil(cantidad * p.toDouble()).coerceIn(0.0, Int.MAX_VALUE.toDouble()).toInt()
            val cantidadTexto = if (unidad == "sacos") cantidad.toLong().toString() else mostrarObra(cantidad)
            return MaterialItem(
                "Radier / " + solucionActual + " / " + nombre,
                cantidadTexto + " " + unidad,
                p, costo
            )
        }
        buildList {
            add(recomendado("Cemento 25kg (saco)", sacos, "sacos"))
            add(recomendado("Arena para hormigón (m³)", arena, "m³"))
            add(recomendado("Grava para hormigón (m³)", grava, "m³"))
            if (solucionActual == "Con malla") add(recomendado("Malla electrosoldada (m²)", malla, "m²"))
        }
    } else emptyList()
    val sugeridosElegidos = sugerenciasRadier.filter { item ->
        val key = item.nombre.substringAfterLast(" / ")
        incluirSugerido[key] != false
    }
    val partidasFinales = sugeridosElegidos + partidas
    val grupoActual = "$trabajo / $solucionActual"
    val existeEnPresupuesto = yaAgregado(grupoActual)
    val total = partidasFinales.sumOf { it.total.toLong() }
    val nombresSugeridos = sugerenciasRadier.map { it.nombre.substringAfterLast(" / ") }.toSet()
    val visibles = precios.keys.filter { key ->
        key !in nombresSugeridos &&
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
                        DropdownMenuItem(text = { Text(t) }, onClick = {
                            if (trabajo != t) {
                                trabajo = t
                                solucion = when (t) {
                                    "Radier" -> "Hormigón simple"
                                    "Piso cerámico", "Piso flotante" -> "Sobre piso existente"
                                    "Tabique" -> "Estructura de madera"
                                    "Muro" -> "Ladrillo o bloque"
                                    "Techumbre" -> "Plancha metálica"
                                    else -> "Definido por usuario"
                                }
                                consumos = emptyMap()
                                incluirSugerido.clear()
                            }
                            expanded = false
                        })
                    }
                }
            }
        }
        item {
            Text("Solución constructiva", fontWeight = FontWeight.Bold, color = TextMain)
            opciones.forEach { op ->
                Row(Modifier.fillMaxWidth().clickable { solucion = op },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = solucionActual == op, onClick = { solucion = op })
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
        if (trabajo == "Radier") item {
            HorizontalDivider(color = BorderSubtle)
            Text("Materiales sugeridos para el radier", fontSize = 18.sp,
                fontWeight = FontWeight.Bold, color = TextMain)
            Text("La estimación utiliza el volumen de hormigón: superficie × espesor. Ajusta la dosificación antes de presupuestar.",
                fontSize = 12.sp, color = TextMuted)
            OutlinedTextField(value = cementoKgM3, onValueChange = { cementoKgM3 = it },
                label = { Text("Cemento (kg por m³ de hormigón)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = arenaM3M3, onValueChange = { arenaM3M3 = it },
                    label = { Text("Arena (m³/m³)") }, modifier = Modifier.weight(1f),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = gravaM3M3, onValueChange = { gravaM3M3 = it },
                    label = { Text("Grava (m³/m³)") }, modifier = Modifier.weight(1f),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            if (solucionActual == "Con malla") {
                OutlinedTextField(value = traslapeMalla, onValueChange = { traslapeMalla = it },
                    label = { Text("Traslape y recortes de malla (%)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            if (!datosDosificacionValidos)
                Text("Ingresa factores válidos para calcular los materiales.", color = AccentAmber)
            if (valido && datosDosificacionValidos) {
                Text("Hormigón geométrico: " + mostrarObra(volumen) +
                    " m³ · Con merma: " + mostrarObra(volumen * (1.0 + desperdicio / 100.0)) + " m³",
                    color = AccentGreen, fontWeight = FontWeight.SemiBold)
            }
            sugerenciasRadier.forEach { mat ->
                val key = mat.nombre.substringAfterLast(" / ")
                Card(colors = CardDefaults.cardColors(containerColor = BgCard)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = incluirSugerido[key] != false,
                            onCheckedChange = { incluirSugerido[key] = it })
                        Column(Modifier.weight(1f)) {
                            Text(key, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Cantidad sugerida: " + mat.cantidad,
                                color = AccentAmber, fontSize = 12.sp)
                            Text(if (mat.precioUnitario == 0) "Sin precio: configúralo en el catálogo"
                                 else formatoCLP(mat.total),
                                color = if (mat.precioUnitario == 0) AccentAmber else TextMuted,
                                fontSize = 11.sp)
                        }
                    }
                }
            }
            Text("Valores orientativos, no dosificación estructural certificada. Verifica resistencia, áridos, compactación, espesor, refuerzo y dosificación técnica.",
                color = TextMuted, fontSize = 11.sp)
        }
        item {
            HorizontalDivider(color = BorderSubtle)
            Text("Materiales adicionales del catálogo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Opcional: incorpora materiales adicionales, con el consumo que corresponda. Para cemento, áridos y malla del radier usa las sugerencias calculadas de arriba.",
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
            Text("Materiales válidos: " + partidasFinales.size.toString() +
                " · Subtotal: " + formatoCLP(total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                color = AccentGreen, fontWeight = FontWeight.Bold)
            if (consumos.isNotEmpty() && partidas.size != consumos.size)
                Text("Revisa consumos y medidas de todos los artículos seleccionados.", color = AccentAmber)
            Button(onClick = { onAgregar(partidasFinales) }, modifier = Modifier.fillMaxWidth(),
                enabled = valido && partidasFinales.isNotEmpty() &&
                    partidas.size == consumos.size && total <= Int.MAX_VALUE &&
                    (trabajo != "Radier" || datosDosificacionValidos)) {
                Text(if (existeEnPresupuesto) "Actualizar este trabajo en presupuesto"
                     else "Agregar este trabajo al presupuesto")
            }
            Text(if (existeEnPresupuesto)
                    "Este trabajo ya existe: al pulsar se reemplazarán sus materiales, NO se sumará otra copia."
                 else "Esta acción agrega la partida al borrador; para guardarlo, entra a Presupuesto y pulsa GUARDAR.",
                color = AccentAmber, fontSize = 12.sp)
            Text("Cubicación orientativa. Verifica espesores, formato comercial y consumo del fabricante.",
                fontSize = 11.sp, color = TextMuted)
        }
    }
}
