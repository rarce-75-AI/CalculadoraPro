package cl.rarce.calculadorapro

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun moneyInicio(v: Long) = "$" +
    NumberFormat.getNumberInstance(Locale("es", "CL")).format(v)

@Composable
private fun AccesoInicio(nombre: String, descripcion: String, icono: ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icono, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(25.dp))
            Column(Modifier.weight(1f)) {
                Text(nombre, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(descripcion, color = TextMuted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
fun InicioObrasScreen(
    context: Context,
    presupuestos: List<PresupuestoGuardado>,
    movimientos: List<MovimientoObra>,
    onFinanzas: () -> Unit,
    onTrabajos: () -> Unit,
    onPresupuestos: () -> Unit,
    onPrecios: () -> Unit
) {
    val obras = cargarControlObras(context)
    val vigentes = obras.filter { it.estado != "Terminada" }
    val terminadas = obras.count { it.estado == "Terminada" }
    val ingresos = movimientos.filter { it.esIngreso }.sumOf { it.monto.toLong() }
    val gastos = movimientos.filter { !it.esIngreso }.sumOf { it.monto.toLong() }
    val porCobrar = obras.sumOf { control ->
        val monto = presupuestos.find { it.id == control.presupuestoId }?.total ?: 0L
        val cobros = movimientos.filter { it.obraId == control.presupuestoId && it.esIngreso }
            .sumOf { it.monto.toLong() }
        (monto - cobros).coerceAtLeast(0)
    }
    val total = (ingresos + gastos).coerceAtLeast(1)
    val proporcionIngresos = (ingresos.toFloat() / total).coerceIn(0f, 1f)
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Cálculo de Obra", fontSize = 23.sp, color = TextMain, fontWeight = FontWeight.ExtraBold)
            Text("Inicio · Control de trabajos y presupuestos",
                color = TextMuted, fontSize = 12.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardDato("Obras activas", vigentes.size.toString(), AccentGreen, Modifier.weight(1f))
                DashboardDato("Terminadas", terminadas.toString(), AccentBlue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardDato("Presupuestos", presupuestos.size.toString(), AccentAmber, Modifier.weight(1f))
                DashboardDato("Por cobrar", moneyInicio(porCobrar), AccentRed, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = BgCard),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Balance general", color = TextMain, fontWeight = FontWeight.Bold)
                    Text("Saldo real: " + moneyInicio(ingresos - gastos),
                        color = if (ingresos >= gastos) AccentGreen else AccentRed,
                        fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    LinearProgressIndicator(progress = { proporcionIngresos },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = AccentGreen, trackColor = AccentRed)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ingresos: " + moneyInicio(ingresos), color = AccentGreen, fontSize = 11.sp)
                        Text("Gastos: " + moneyInicio(gastos), color = AccentRed, fontSize = 11.sp)
                    }
                    Text("Gráfico: proporción entre ingresos y gastos registrados; no representa utilidad.",
                        color = TextMuted, fontSize = 10.sp)
                }
            }
        }
        item {
            Text("Accesos rápidos", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(5.dp))
            AccesoInicio("Finanzas", "Cobros, anticipos y gastos por obra",
                Icons.Default.AccountBalanceWallet, onFinanzas)
            Spacer(Modifier.height(6.dp))
            AccesoInicio("Trabajos", "Configurar radier, pisos y otras partidas",
                Icons.Default.Construction, onTrabajos)
            Spacer(Modifier.height(6.dp))
            AccesoInicio("Presupuestos", "Clientes, tarjetas y exportaciones",
                Icons.Default.Description, onPresupuestos)
            Spacer(Modifier.height(6.dp))
            AccesoInicio("Precios", "Catálogo y valores de materiales",
                Icons.Default.Inventory, onPrecios)
        }
        item {
            Text("Obras en ejecución", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        if (vigentes.isEmpty()) item {
            Text("Cuando inicies una obra desde Finanzas, aparecerá aquí.",
                color = TextMuted, fontSize = 12.sp)
        }
        vigentes.forEach { control ->
            val p = presupuestos.find { it.id == control.presupuestoId }
            if (p != null) item(key = control.presupuestoId) {
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).apply { isLenient = false }
                val inicio = runCatching { fmt.parse(control.fechaInicio)?.time ?: 0L }.getOrDefault(0L)
                val transcurridos = ((Date().time - inicio) / 86400000L).coerceAtLeast(0)
                val porcentajePlazo = if (inicio > 0 && control.diasPlazo > 0)
                    (transcurridos.toFloat() / control.diasPlazo).coerceIn(0f, 1f) else 0f
                Card(colors = CardDefaults.cardColors(containerColor = BgCard),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onFinanzas)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.obra.ifBlank { p.cliente }, color = TextMain, fontWeight = FontWeight.Bold)
                        Text("Cliente: " + p.cliente, color = TextMuted, fontSize = 12.sp)
                        Text("Avance físico declarado: " + control.avanceFisico + "%",
                            color = AccentGreen, fontSize = 11.sp)
                        LinearProgressIndicator(progress = { control.avanceFisico / 100f },
                            modifier = Modifier.fillMaxWidth().height(7.dp), color = AccentGreen)
                        Text("Plazo transcurrido: " + (porcentajePlazo * 100).toInt() + "%",
                            color = TextMuted, fontSize = 11.sp)
                        LinearProgressIndicator(progress = { porcentajePlazo },
                            modifier = Modifier.fillMaxWidth().height(5.dp), color = AccentBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardDato(etiqueta: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(etiqueta, color = TextMuted, fontSize = 11.sp)
            Text(valor, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
