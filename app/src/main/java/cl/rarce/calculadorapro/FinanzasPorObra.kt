package cl.rarce.calculadorapro

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ControlObra(
    val presupuestoId: Long,
    val fechaInicio: String,
    val diasPlazo: Int,
    val anticipoPorcentaje: Int,
    val estado: String = "En ejecución"
)

private fun formatoMontoObra(v: Long): String =
    "$" + java.text.NumberFormat.getNumberInstance(Locale("es", "CL")).format(v) + " CLP"

private fun hoyObra(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date())

private fun fechaObraValida(valor: String): Date? = runCatching {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).apply { isLenient = false }
    sdf.parse(valor)
}.getOrNull()

private fun fechaTerminoObra(inicio: String, dias: Int): String {
    val fecha = fechaObraValida(inicio) ?: return "Fecha inválida"
    val calendario = Calendar.getInstance().apply { time = fecha; add(Calendar.DAY_OF_MONTH, dias) }
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(calendario.time)
}

private fun diasRestantesObra(inicio: String, dias: Int): Long? {
    val fin = fechaObraValida(fechaTerminoObra(inicio, dias)) ?: return null
    val hoy = fechaObraValida(hoyObra()) ?: return null
    return (fin.time - hoy.time) / 86400000L
}

private fun cargarControlObras(context: Context): List<ControlObra> = runCatching {
    val raw = context.getSharedPreferences("calculadora_pro_v2", Context.MODE_PRIVATE)
        .getString("control_obras_v1", "[]") ?: "[]"
    val a = JSONArray(raw)
    (0 until a.length()).map { i ->
        val j = a.getJSONObject(i)
        ControlObra(j.getLong("presupuestoId"), j.getString("fechaInicio"),
            j.getInt("diasPlazo"), j.getInt("anticipoPorcentaje"),
            j.optString("estado", "En ejecución"))
    }
}.getOrDefault(emptyList())

private fun persistirControlObras(context: Context, obras: List<ControlObra>) {
    val a = JSONArray()
    obras.forEach { obra ->
        a.put(JSONObject().apply {
            put("presupuestoId", obra.presupuestoId)
            put("fechaInicio", obra.fechaInicio)
            put("diasPlazo", obra.diasPlazo)
            put("anticipoPorcentaje", obra.anticipoPorcentaje)
            put("estado", obra.estado)
        })
    }
    context.getSharedPreferences("calculadora_pro_v2", Context.MODE_PRIVATE).edit()
        .putString("control_obras_v1", a.toString()).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanzasPorObraScreen(
    presupuestos: List<PresupuestoGuardado>,
    movimientos: MutableList<MovimientoObra>
) {
    val context = LocalContext.current
    val controles = remember {
        mutableStateListOf<ControlObra>().apply { addAll(cargarControlObras(context)) }
    }
    val activas = controles.mapNotNull { control ->
        presupuestos.find { it.id == control.presupuestoId }?.let { it to control }
    }
    var obraSeleccionadaId by remember { mutableLongStateOf(0L) }
    val activa = activas.find { it.first.id == obraSeleccionadaId }
    var abrirSelector by remember { mutableStateOf(false) }
    var mostrarConfig by remember { mutableStateOf(false) }
    var mostrarMovimiento by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf<Long?>(null) }
    var aviso by remember { mutableStateOf("") }
    var presupuestoElegidoId by remember { mutableLongStateOf(0L) }
    var fechaInput by remember { mutableStateOf(hoyObra()) }
    var plazoInput by remember { mutableStateOf("14") }
    var anticipoInput by remember { mutableStateOf("25") }
    var estadoInput by remember { mutableStateOf("En ejecución") }
    var descripcionInput by remember { mutableStateOf("") }
    var montoInput by remember { mutableStateOf("") }
    var ingresoInput by remember { mutableStateOf(true) }
    var tipoIngreso by remember { mutableStateOf("Abono cliente") }

    val movimientosFiltrados = if (obraSeleccionadaId == 0L) movimientos.toList()
        else movimientos.filter { it.obraId == obraSeleccionadaId }
    val ingresos = movimientosFiltrados.filter { it.esIngreso }.sumOf { it.monto.toLong() }
    val gastos = movimientosFiltrados.filter { !it.esIngreso }.sumOf { it.monto.toLong() }
    val saldo = ingresos - gastos
    val presupuestado = if (activa != null) activa.first.total
        else activas.sumOf { it.first.total }
    val cobrado = if (activa != null) ingresos
        else movimientos.filter { it.esIngreso && it.obraId != 0L && controles.any { o -> o.presupuestoId == it.obraId } }
            .sumOf { it.monto.toLong() }
    val pendiente = (presupuestado - cobrado).coerceAtLeast(0L)

    fun guardarControl() {
        val p = presupuestos.find { it.id == presupuestoElegidoId }
        val plazo = plazoInput.toIntOrNull()
        val anticipo = anticipoInput.toIntOrNull()
        when {
            p == null -> aviso = "Elige un presupuesto guardado."
            fechaObraValida(fechaInput) == null -> aviso = "Fecha inválida. Usa DD/MM/AAAA."
            plazo == null || plazo <= 0 || plazo > 3650 -> aviso = "Plazo inválido (1 a 3650 días)."
            anticipo == null || anticipo !in 0..100 -> aviso = "Anticipo debe estar entre 0% y 100%."
            else -> {
                controles.removeAll { it.presupuestoId == p.id }
                controles.add(ControlObra(p.id, fechaInput, plazo!!, anticipo!!, estadoInput))
                persistirControlObras(context, controles)
                obraSeleccionadaId = p.id
                mostrarConfig = false
                aviso = "Configuración de obra guardada. El anticipo todavía no se considera cobrado."
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Finanzas de obras", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextMain)
            Text("Cada gasto y abono pertenece a una obra. Las cotizaciones no son pagos.",
                fontSize = 12.sp, color = TextMuted)
            OutlinedButton(
                onClick = { abrirSelector = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (activa == null) "Todas las obras · Vista general"
                    else activa.first.obra.ifBlank { activa.first.cliente } + " · " + activa.first.cliente)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            if (aviso.isNotBlank()) Text(aviso, color = AccentAmber, fontSize = 12.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = BgCard),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(if (activa == null) "Resumen consolidado" else "Resumen de esta obra",
                        fontWeight = FontWeight.Bold, color = TextMain)
                    Text("SALDO REAL: " + formatoMontoObra(saldo),
                        fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                        color = if (saldo >= 0) AccentGreen else AccentRed)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Cobros recibidos", color = TextMuted, fontSize = 11.sp)
                            Text(formatoMontoObra(ingresos), color = AccentGreen)
                        }
                        Column {
                            Text("Gastos registrados", color = TextMuted, fontSize = 11.sp)
                            Text(formatoMontoObra(gastos), color = AccentRed)
                        }
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Text("Presupuestos de obras: " + formatoMontoObra(presupuestado),
                        color = TextMain, fontSize = 12.sp)
                    Text("Pendiente por cobrar: " + formatoMontoObra(pendiente),
                        color = AccentAmber, fontWeight = FontWeight.Bold)
                    if (activa == null && movimientos.any { it.obraId == 0L })
                        Text("Incluye movimientos antiguos sin obra asignada en el saldo global. " +
                             "No se incluyen como cobros de contratos.",
                            color = AccentAmber, fontSize = 11.sp)
                }
            }
        }
        if (activa != null) item {
            val (presupuesto, control) = activa
            val faltanDias = diasRestantesObra(control.fechaInicio, control.diasPlazo)
            val anticipoExigido = presupuesto.total * control.anticipoPorcentaje / 100L
            val pagos = movimientos.filter { it.obraId == presupuesto.id && it.esIngreso }
                .sumOf { it.monto.toLong() }
            Card(colors = CardDefaults.cardColors(containerColor = BgCard)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Contrato y avance de pagos", fontWeight = FontWeight.Bold, color = TextMain)
                    Text("Cliente: " + presupuesto.cliente, color = TextMain)
                    Text("Inicio: " + control.fechaInicio + " · Plazo: " + control.diasPlazo + " días",
                        color = TextMuted, fontSize = 12.sp)
                    Text("Término previsto: " + fechaTerminoObra(control.fechaInicio, control.diasPlazo),
                        color = TextMain)
                    Text(when {
                        faltanDias == null -> "Revisar fecha de inicio"
                        faltanDias < 0 && control.estado != "Terminada" -> "Atraso: " + (-faltanDias) + " días"
                        control.estado == "Terminada" -> "Obra terminada"
                        else -> "Días restantes: $faltanDias"
                    }, color = if (faltanDias != null && faltanDias < 0 && control.estado != "Terminada") AccentRed else AccentGreen)
                    Text("Estado: " + control.estado, color = TextMuted, fontSize = 12.sp)
                    HorizontalDivider(color = BorderSubtle)
                    Text("Anticipo pactado: " + control.anticipoPorcentaje + "% = " + formatoMontoObra(anticipoExigido),
                        color = TextMain)
                    Text("Cobros registrados: " + formatoMontoObra(pagos), color = AccentGreen)
                    Text(if (pagos < anticipoExigido) "Falta para cubrir anticipo: " +
                        formatoMontoObra(anticipoExigido - pagos) else "Anticipo cubierto por pagos registrados",
                        color = if (pagos < anticipoExigido) AccentAmber else AccentGreen)
                    Text(if (pagos >= presupuesto.total) "Pago total recibido" else
                        "Saldo pendiente del cliente: " + formatoMontoObra(presupuesto.total - pagos),
                        fontWeight = FontWeight.Bold, color = AccentAmber)
                    OutlinedButton(onClick = {
                        presupuestoElegidoId = presupuesto.id
                        fechaInput = control.fechaInicio
                        plazoInput = control.diasPlazo.toString()
                        anticipoInput = control.anticipoPorcentaje.toString()
                        estadoInput = control.estado
                        mostrarConfig = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("Editar plazo, anticipo y estado") }
                }
            }
        }
        item {
            Button(onClick = {
                if (activa == null) aviso = "Selecciona primero una obra para asignar el gasto o abono."
                else { descripcionInput = ""; montoInput = ""; ingresoInput = true; tipoIngreso = "Abono cliente"; mostrarMovimiento = true }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Registrar gasto o abono de esta obra")
            }
            OutlinedButton(onClick = {
                presupuestoElegidoId = presupuestos.firstOrNull { p ->
                    controles.none { it.presupuestoId == p.id }
                }?.id ?: 0L
                fechaInput = hoyObra(); plazoInput = "14"; anticipoInput = "25"
                estadoInput = "En ejecución"; mostrarConfig = true
            }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Iniciar obra desde presupuesto guardado")
            }
        }
        item {
            Text("Movimientos" + if (activa == null) " de todas las obras" else " de esta obra",
                fontWeight = FontWeight.Bold, color = TextMain)
        }
        if (movimientosFiltrados.isEmpty()) item {
            Text("Sin movimientos reales registrados.", color = TextMuted)
        }
        items(movimientosFiltrados, key = { it.id }) { m ->
            val nombreObra = presupuestos.find { it.id == m.obraId }
                ?.let { it.obra.ifBlank { it.cliente } } ?: "Sin obra asignada"
            Card(colors = CardDefaults.cardColors(containerColor = BgCard)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.titulo, color = TextMain, fontWeight = FontWeight.SemiBold)
                        Text(nombreObra + " · " + m.categoria, color = TextMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text((if (m.esIngreso) "+" else "-") + formatoMontoObra(m.monto.toLong()),
                            color = if (m.esIngreso) AccentGreen else AccentRed)
                        TextButton(onClick = { mostrarConfirmacionEliminar = m.id }) {
                            Text("Eliminar", color = AccentRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (abrirSelector) AlertDialog(
        onDismissRequest = { abrirSelector = false },
        title = { Text("Seleccionar obra") },
        text = {
            Column(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { obraSeleccionadaId = 0L; abrirSelector = false }) {
                    Text("Todas las obras · Resumen general")
                }
                activas.forEach { (p, _) ->
                    TextButton(onClick = { obraSeleccionadaId = p.id; abrirSelector = false }) {
                        Text((p.obra.ifBlank { "Obra" } + " · " + p.cliente).take(65))
                    }
                }
                if (activas.isEmpty()) Text("Aún no hay obras iniciadas. Guarda un presupuesto y usa Iniciar obra.")
            }
        },
        confirmButton = { TextButton(onClick = { abrirSelector = false }) { Text("Cerrar") } }
    )

    if (mostrarConfig) {
        val disponibles = presupuestos.filter { p ->
            controles.none { it.presupuestoId == p.id } || p.id == presupuestoElegidoId
        }
        AlertDialog(onDismissRequest = { mostrarConfig = false },
            title = { Text("Datos de la obra") },
            text = {
                Column(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (disponibles.isEmpty()) Text("Guarda primero un presupuesto en la pestaña Presupuesto.")
                    else {
                        Text("Presupuesto vinculado:", fontWeight = FontWeight.Bold)
                        disponibles.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = presupuestoElegidoId == p.id,
                                    onClick = { presupuestoElegidoId = p.id })
                                Text(p.obra.ifBlank { p.cliente } + " · " + p.cliente,
                                    modifier = Modifier.clickable { presupuestoElegidoId = p.id }, fontSize = 12.sp)
                            }
                        }
                    }
                    OutlinedTextField(fechaInput, { fechaInput = it },
                        label = { Text("Fecha de inicio DD/MM/AAAA") }, singleLine = true)
                    OutlinedTextField(plazoInput, { plazoInput = it },
                        label = { Text("Duración en días calendario") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(anticipoInput, { anticipoInput = it },
                        label = { Text("Anticipo pactado (%)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("25", "30", "50").forEach { porcentaje ->
                            FilterChip(selected = anticipoInput == porcentaje,
                                onClick = { anticipoInput = porcentaje },
                                label = { Text("$porcentaje%") })
                        }
                    }
                    Text("Estado de la obra:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("En ejecución", "Terminada").forEach { st ->
                            FilterChip(selected = estadoInput == st,
                                onClick = { estadoInput = st },
                                label = { Text(if (st == "Terminada") "Terminada" else "Activa") })
                        }
                    }
                    Text("El anticipo pactado no se registrará como ingreso hasta que lo cobres.",
                        color = AccentAmber, fontSize = 11.sp)
                }
            },
            confirmButton = { Button(onClick = { guardarControl() }) { Text("Guardar obra") } },
            dismissButton = { TextButton(onClick = { mostrarConfig = false }) { Text("Cancelar") } }
        )
    }

    if (mostrarMovimiento && activa != null) {
        AlertDialog(onDismissRequest = { mostrarMovimiento = false },
            title = { Text("Movimiento · " + activa.first.cliente) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = ingresoInput,
                            onClick = { ingresoInput = true },
                            label = { Text("Cobro / abono") })
                        FilterChip(selected = !ingresoInput,
                            onClick = { ingresoInput = false },
                            label = { Text("Gasto") })
                    }
                    if (ingresoInput) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Anticipo", "Abono cliente", "Pago final").forEach { tipo ->
                            FilterChip(selected = tipoIngreso == tipo, onClick = { tipoIngreso = tipo },
                                label = { Text(tipo, fontSize = 11.sp) })
                        }
                    }
                    OutlinedTextField(descripcionInput, { descripcionInput = it },
                        label = { Text("Detalle del movimiento") }, singleLine = true)
                    OutlinedTextField(montoInput, { montoInput = it },
                        label = { Text("Monto efectivamente recibido / pagado (CLP)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Text("El movimiento quedará asociado solo a esta obra.", color = TextMuted, fontSize = 11.sp)
                }
            },
            confirmButton = { Button(onClick = {
                val valor = montoInput.replace(".", "").replace("$", "").trim().toIntOrNull()
                if (valor == null || valor <= 0) {
                    aviso = "Ingresa un monto válido mayor que cero."
                } else {
                    movimientos.add(0, MovimientoObra(
                        id = System.currentTimeMillis(),
                        titulo = descripcionInput.ifBlank { if (ingresoInput) tipoIngreso else "Gasto de obra" },
                        categoria = if (ingresoInput) tipoIngreso else "Gasto",
                        monto = valor, esIngreso = ingresoInput, obraId = activa.first.id
                    ))
                    mostrarMovimiento = false
                    aviso = "Movimiento registrado en " + activa.first.obra.ifBlank { activa.first.cliente }
                }
            }) { Text("Guardar movimiento") } },
            dismissButton = { TextButton(onClick = { mostrarMovimiento = false }) { Text("Cancelar") } }
        )
    }
    mostrarConfirmacionEliminar?.let { id ->
        AlertDialog(onDismissRequest = { mostrarConfirmacionEliminar = null },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Esta acción cambiará el saldo real y el total abonado.") },
            confirmButton = { Button(onClick = {
                movimientos.removeAll { it.id == id }
                mostrarConfirmacionEliminar = null
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionEliminar = null }) {
                Text("Cancelar")
            } }
        )
    }
}
