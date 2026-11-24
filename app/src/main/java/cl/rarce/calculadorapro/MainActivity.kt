package cl.rarce.calculadorapro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

// Paleta de colores Dark Pro (Estilo Finanzas & Obra)
val BgPrimary = Color(0xFF0B1116)
val BgCard = Color(0xFF151E26)
val BgInput = Color(0xFF1C2731)
val AccentGreen = Color(0xFF2ECC71)
val AccentRed = Color(0xFFE74C3C)
val AccentAmber = Color(0xFFF39C12)
val AccentBlue = Color(0xFF3498DB)
val TextMain = Color(0xFFF4F6F8)
val TextMuted = Color(0xFF8B9BB0)
val BorderSubtle = Color(0xFF24323F)

data class MaterialItem(
    val nombre: String,
    val cantidad: String,
    val precioUnitario: Int,
    val total: Int
)

data class MovimientoObra(
    val id: Long,
    val titulo: String,
    val categoria: String,
    val monto: Int,
    val esIngreso: Boolean
)

fun formatoCLP(valor: Int): String {
    val format = NumberFormat.getNumberInstance(Locale("es", "CL"))
    return "$" + format.format(valor) + " CLP"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentGreen,
                    background = BgPrimary,
                    surface = BgCard,
                    onPrimary = Color(0xFF08120C),
                    onBackground = TextMain,
                    onSurface = TextMain
                )
            ) {
                CalculadoraProApp()
            }
        }
    }
}

@Composable
fun CalculadoraProApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Estado de Finanzas de Obra
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calculadora_pro_v2", Context.MODE_PRIVATE) }
    val movimientos = remember { mutableStateListOf<MovimientoObra>().also { lista ->
        runCatching {
            val data = JSONArray(prefs.getString("movimientos", "[]"))
            for (i in 0 until data.length()) {
                val x = data.getJSONObject(i)
                lista.add(MovimientoObra(x.getLong("id"), x.getString("titulo"),
                    x.getString("categoria"), x.getInt("monto"), x.getBoolean("esIngreso")))
            }
        }
    } }
    val itemsPresupuesto = remember { mutableStateListOf<MaterialItem>() }
    var nombreCliente by remember { mutableStateOf(prefs.getString("cliente", "") ?: "") }
    var manoDeObra by remember { mutableStateOf(prefs.getString("manoDeObra", "0") ?: "0") }

    // Lista de Precios Base Editables (Chile)
    val preciosBase = remember {
        mutableStateMapOf(
            "Cerámica 45x45 (m²)" to 11500,
            "Adhesivo Bekrón 25kg (saco)" to 6200,
            "Fragüe 1kg (bolsa)" to 3500,
            "Plancha Volcanita 10mm" to 8900,
            "Perfil Metalcon 2x3 (tira)" to 4200,
            "Pasta Muro 15kg (tineta)" to 14500,
            "Plancha Zinc Acanalada" to 9800,
            "Ladrillo Fiscal / Princesa" to 750,
            "Cemento 25kg (saco)" to 5800,
            "Costanera 6m (tira)" to 4800
        ).apply {
            runCatching {
                val saved = JSONObject(prefs.getString("precios", "{}"))
                keys.toList().forEach { key ->
                    if (saved.has(key)) this[key] = saved.getInt(key)
                }
            }
        }
    }

    LaunchedEffect(movimientos.toList()) {
        val data = JSONArray()
        movimientos.forEach { m -> data.put(JSONObject().apply {
            put("id", m.id); put("titulo", m.titulo); put("categoria", m.categoria)
            put("monto", m.monto); put("esIngreso", m.esIngreso)
        }) }
        prefs.edit().putString("movimientos", data.toString()).apply()
    }
    LaunchedEffect(preciosBase.toMap()) {
        val data = JSONObject()
        preciosBase.forEach { (name, price) -> data.put(name, price) }
        prefs.edit().putString("precios", data.toString()).apply()
    }
    LaunchedEffect(nombreCliente, manoDeObra) {
        prefs.edit().putString("cliente", nombreCliente).putString("manoDeObra", manoDeObra).apply()
    }

    Scaffold(
        containerColor = BgPrimary,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0E161D),
                contentColor = TextMuted
            ) {
                val navItems = listOf(
                    Triple("Finanzas", Icons.Default.AccountBalanceWallet, 0),
                    Triple("Cálculos", Icons.Default.Calculate, 1),
                    Triple("Presupuesto", Icons.Default.Description, 2),
                    Triple("Precios", Icons.Default.Inventory, 3)
                )
                navItems.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentGreen,
                            selectedTextColor = AccentGreen,
                            indicatorColor = AccentGreen.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> FinanzasObraScreen(movimientos)
                1 -> CalculadorasScreen(
                    preciosBase = preciosBase,
                    onAgregarAlPresupuesto = { nuevosItems ->
                        itemsPresupuesto.clear()
                        itemsPresupuesto.addAll(nuevosItems)
                        selectedTab = 2
                    }
                )
                2 -> PresupuestoScreen(
                    cliente = nombreCliente,
                    onClienteChange = { nombreCliente = it },
                    items = itemsPresupuesto,
                    manoDeObra = manoDeObra,
                    onManoDeObraChange = { manoDeObra = it },
                    onGuardarEnFinanzas = { totalFinal ->
                        movimientos.add(
                            0,
                            MovimientoObra(
                                id = System.currentTimeMillis(),
                                titulo = "Cobro recibido: $nombreCliente",
                                categoria = "Ingreso real",
                                monto = totalFinal,
                                esIngreso = true
                            )
                        )
                        selectedTab = 0
                    }
                )
                3 -> PreciosBaseScreen(preciosBase)
            }
        }
    }
}

// ==========================================
// 1. PANTALLA FINANZAS & CONTROL DE OBRAS
// ==========================================
@Composable
fun FinanzasObraScreen(movimientos: MutableList<MovimientoObra>) {
    var showDialog by remember { mutableStateOf(false) }
    var tituloInput by remember { mutableStateOf("") }
    var montoInput by remember { mutableStateOf("") }
    var esIngresoInput by remember { mutableStateOf(false) }

    val totalIngresos = movimientos.filter { it.esIngreso }.sumOf { it.monto }
    val totalGastos = movimientos.filter { !it.esIngreso }.sumOf { it.monto }
    val saldo = totalIngresos - totalGastos

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Calculadora Pro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Control de pagos realmente recibidos y gastos. Datos guardados en este teléfono.", fontSize = 12.sp, color = TextMuted)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saldo Disponible de Obra", fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatoCLP(saldo),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (saldo >= 0) AccentGreen else AccentRed
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ingresos / Abonos", fontSize = 12.sp, color = TextMuted)
                            Text(formatoCLP(totalIngresos), fontWeight = FontWeight.Bold, color = AccentGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gastos Materiales", fontSize = 12.sp, color = TextMuted)
                            Text(formatoCLP(totalGastos), fontWeight = FontWeight.Bold, color = AccentRed)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF08120C))
                Spacer(Modifier.width(8.dp))
                Text("Registrar Gasto o Abono", fontWeight = FontWeight.Bold, color = Color(0xFF08120C))
            }
        }

        item {
            Text("Últimos Movimientos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }

        if (movimientos.isEmpty()) item { Text("Sin movimientos aún. Agrega un gasto o un cobro realmente recibido.", fontSize = 13.sp, color = TextMuted) }
        items(movimientos, key = { it.id }) { mov ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mov.titulo, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text(mov.categoria, fontSize = 12.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (mov.esIngreso) "+" else "-") + formatoCLP(mov.monto),
                            fontWeight = FontWeight.Bold,
                            color = if (mov.esIngreso) AccentGreen else AccentRed
                        )
                        TextButton(onClick = { movimientos.removeAll { it.id == mov.id } },
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)) { Text("Eliminar", fontSize = 11.sp, color = AccentRed) }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = BgCard,
            title = { Text("Nuevo Movimiento", color = TextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !esIngresoInput,
                            onClick = { esIngresoInput = false },
                            label = { Text("Gasto Material") }
                        )
                        FilterChip(
                            selected = esIngresoInput,
                            onClick = { esIngresoInput = true },
                            label = { Text("Ingreso / Abono") }
                        )
                    }
                    OutlinedTextField(
                        value = tituloInput,
                        onValueChange = { tituloInput = it },
                        label = { Text("Descripción (Ej: Bekrón, Pago cliente)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = montoInput,
                        onValueChange = { montoInput = it },
                        label = { Text("Monto en CLP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = montoInput.replace(".", "").replace("$", "").trim().toIntOrNull() ?: 0
                    if (m > 0 && tituloInput.isNotBlank()) {
                        movimientos.add(
                            0,
                            MovimientoObra(
                                id = System.currentTimeMillis(),
                                titulo = tituloInput,
                                categoria = if (esIngresoInput) "Abono Obra" else "Materiales",
                                monto = m,
                                esIngreso = esIngresoInput
                            )
                        )
                        tituloInput = ""
                        montoInput = ""
                        showDialog = false
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar", color = TextMuted) }
            }
        )
    }
}

// ==========================================
// 2. CALCULADORAS TÉCNICAS (PISOS, TABIQUES, TECHUMBRE, MUROS)
// ==========================================
@Composable
fun CalculadorasScreen(
    preciosBase: Map<String, Int>,
    onAgregarAlPresupuesto: (List<MaterialItem>) -> Unit
) {
    var especialidad by remember { mutableIntStateOf(0) }
    val especialidades = listOf("Pisos & Bekrón", "Tabiques Metalcon", "Techumbre", "Muro Ladrillo")

    // Inputs compartidos
    var largo by remember { mutableStateOf("6.0") }
    var ancho by remember { mutableStateOf("4.5") }
    var incluirMerma by remember { mutableStateOf(true) }

    val l = largo.replace(',', '.').toDoubleOrNull() ?: 0.0
    val a = ancho.replace(',', '.').toDoubleOrNull() ?: 0.0
    val medidasValidas = l > 0 && a > 0 && l.isFinite() && a.isFinite() && l <= 100 && a <= 100
    val areaBase = if (medidasValidas) l * a else 0.0
    val factorMerma = if (incluirMerma) 1.10 else 1.0
    val areaTotal = areaBase * factorMerma

    // Cálculo dinámico según especialidad
    val resultados: List<MaterialItem> = if (!medidasValidas) emptyList() else {
        when (especialidad) {
            0 -> { // Pisos, Cerámica y Bekrón
                val m2Ceramica = ceil(areaTotal).toInt()
                val sacosBekron = ceil(areaTotal / 3.5).toInt() // Rendimiento aprox 3.5 m2 por saco 25kg
                val bolsasFrague = ceil(areaTotal / 8.0).toInt()
                listOf(
                    MaterialItem("Cerámica / Porcelanato", "$m2Ceramica m²", preciosBase["Cerámica 45x45 (m²)"] ?: 11500, m2Ceramica * (preciosBase["Cerámica 45x45 (m²)"] ?: 11500)),
                    MaterialItem("Adhesivo Bekrón 25kg", "$sacosBekron sacos", preciosBase["Adhesivo Bekrón 25kg (saco)"] ?: 6200, sacosBekron * (preciosBase["Adhesivo Bekrón 25kg (saco)"] ?: 6200)),
                    MaterialItem("Fragüe y Separadores", "$bolsasFrague bolsas", preciosBase["Fragüe 1kg (bolsa)"] ?: 3500, bolsasFrague * (preciosBase["Fragüe 1kg (bolsa)"] ?: 3500))
                )
            }
            1 -> { // Tabiques Volcanita & Metalcon (ambas caras)
                val planchasVolcanita = ceil((areaTotal * 2) / 2.88).toInt() // Plancha 1.20x2.40 = 2.88m2
                // Montantes cada 40 cm; altura de muro proviene del segundo campo.
                // Perfiles comerciales de 6 m. Soleras superior e inferior incluidas.
                val montantes = ceil(l / 0.40).toInt() + 1
                val metrosPerfiles = montantes * a + l * 2
                val tirasMetalcon = ceil(metrosPerfiles / 6.0).toInt()
                val tinetasPasta = ceil(areaTotal / 15.0).toInt()
                listOf(
                    MaterialItem("Plancha Volcanita 10mm", "$planchasVolcanita unid.", preciosBase["Plancha Volcanita 10mm"] ?: 8900, planchasVolcanita * (preciosBase["Plancha Volcanita 10mm"] ?: 8900)),
                    MaterialItem("Perfiles Metalcon 2x3", "$tirasMetalcon tiras", preciosBase["Perfil Metalcon 2x3 (tira)"] ?: 4200, tirasMetalcon * (preciosBase["Perfil Metalcon 2x3 (tira)"] ?: 4200)),
                    MaterialItem("Pasta Muro 15kg + Tornillos", "$tinetasPasta tinetas", preciosBase["Pasta Muro 15kg (tineta)"] ?: 14500, tinetasPasta * (preciosBase["Pasta Muro 15kg (tineta)"] ?: 14500))
                )
            }
            2 -> { // Techumbre
                val planchasZinc = ceil(areaTotal / 2.4).toInt()
                val costaneras = ceil((ceil(a / 0.60) + 1) * l / 6.0).toInt()
                listOf(
                    MaterialItem("Planchas Zinc Acanalada", "$planchasZinc unid.", preciosBase["Plancha Zinc Acanalada"] ?: 9800, planchasZinc * (preciosBase["Plancha Zinc Acanalada"] ?: 9800)),
                    MaterialItem("Costaneras Acero / Madera 6m", "$costaneras tiras", preciosBase["Costanera 6m (tira)"] ?: 4800, costaneras * (preciosBase["Costanera 6m (tira)"] ?: 4800))
                )
            }
            else -> { // Muro de Ladrillos
                val ladrillos = ceil(areaTotal * 38).toInt()
                val sacosCemento = ceil(areaTotal * 0.45).toInt()
                listOf(
                    MaterialItem("Ladrillo Fiscal / Princesa", "$ladrillos unid.", preciosBase["Ladrillo Fiscal / Princesa"] ?: 750, ladrillos * (preciosBase["Ladrillo Fiscal / Princesa"] ?: 750)),
                    MaterialItem("Cemento 25kg (Mortero)", "$sacosCemento sacos", preciosBase["Cemento 25kg (saco)"] ?: 5800, sacosCemento * (preciosBase["Cemento 25kg (saco)"] ?: 5800))
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cubicación & Materiales", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Selecciona la especialidad de remodelación", fontSize = 13.sp, color = TextMuted)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                especialidades.forEachIndexed { index, title ->
                    val selected = especialidad == index
                    Box(
                        modifier = Modifier
                            .widthIn(min = 110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccentGreen else BgCard)
                            .clickable { especialidad = index }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color(0xFF08120C) else TextMain,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = largo,
                            onValueChange = { largo = it },
                            label = { Text("Largo (m)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ancho,
                            onValueChange = { ancho = it },
                            label = { Text(if (especialidad == 1 || especialidad == 3) "Alto (m)" else "Ancho (m)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Incluir +10% merma por cortes", fontSize = 13.sp, color = TextMuted)
                        Switch(
                            checked = incluirMerma,
                            onCheckedChange = { incluirMerma = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen)
                        )
                    }

                    if (!medidasValidas) Text("Ingresa medidas mayores que cero y válidas.", color = AccentRed, fontSize = 12.sp)
                    Text(
                        "Superficie con merma: ${String.format(Locale.US, "%.2f", areaTotal)} m²",
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Text("Materiales Calculados", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Cubicación orientativa. Verifica formatos comerciales, rendimientos del fabricante, aberturas y condiciones de obra.", fontSize = 11.sp, color = TextMuted)
        }

        items(resultados) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.nombre, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 2)
                        Text("Cantidad: ${item.cantidad}", fontSize = 12.sp, color = AccentAmber)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(formatoCLP(item.total), fontWeight = FontWeight.Bold, color = TextMain, maxLines = 2)
                }
            }
        }

        item {
            Button(
                onClick = { onAgregarAlPresupuesto(resultados) },
                enabled = medidasValidas && resultados.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Crear Presupuesto con estos Datos →", fontWeight = FontWeight.Bold, color = Color(0xFF08120C))
            }
        }
    }
}

// ==========================================
// 3. PANTALLA PRESUPUESTO & ENVÍO WHATSAPP
// ==========================================
@Composable
fun PresupuestoScreen(
    cliente: String,
    onClienteChange: (String) -> Unit,
    items: List<MaterialItem>,
    manoDeObra: String,
    onManoDeObraChange: (String) -> Unit,
    onGuardarEnFinanzas: (Int) -> Unit
) {
    val context = LocalContext.current
    val totalMateriales = items.sumOf { it.total }
    val totalManoObra = manoDeObra.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val granTotal = (totalMateriales.toLong() + totalManoObra.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    var confirmarCobro by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Detalle de Presupuesto", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Cotización lista para compartir con tu cliente", fontSize = 13.sp, color = TextMuted)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cliente,
                        onValueChange = onClienteChange,
                        label = { Text("Cliente / Nombre de Obra") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manoDeObra,
                        onValueChange = onManoDeObraChange,
                        label = { Text("Mano de Obra (CLP)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(item.nombre, fontWeight = FontWeight.SemiBold, color = TextMain, fontSize = 14.sp)
                    Text(item.cantidad, fontSize = 12.sp, color = TextMuted)
                }
                Text(formatoCLP(item.total), fontWeight = FontWeight.Bold, color = TextMain)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2922)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, AccentGreen, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Estimado Presupuesto", fontSize = 13.sp, color = TextMuted)
                    Text(formatoCLP(granTotal), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AccentGreen)
                }
            }
        }

        item {
            Button(
                onClick = {
                    val resumen = buildString {
                        appendLine("*PRESUPUESTO DE REMODELACIÓN*")
                        appendLine("Obra: $cliente")
                        appendLine("-----------------------------")
                        items.forEach {
                            appendLine("• ${it.nombre} (${it.cantidad}): ${formatoCLP(it.total)}")
                        }
                        appendLine("• Mano de Obra: ${formatoCLP(totalManoObra)}")
                        appendLine("-----------------------------")
                        appendLine("*TOTAL ESTIMADO: ${formatoCLP(granTotal)}*")
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, resumen)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Enviar presupuesto por..."))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF08120C))
                Spacer(Modifier.width(8.dp))
                Text("Compartir cotización", fontWeight = FontWeight.Bold, color = Color(0xFF08120C))
            }
        }

        item {
            OutlinedButton(
                onClick = { confirmarCobro = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Registrar pago recibido", color = AccentGreen)
            }
        }
    }
    if (confirmarCobro) {
        AlertDialog(onDismissRequest = { confirmarCobro = false },
            title = { Text("¿Recibiste el pago?") },
            text = { Text("Registrar una cotización como ingreso altera el saldo. Confirma solamente si el cliente ya pagó el monto completo.") },
            confirmButton = { Button(onClick = {
                onGuardarEnFinanzas(granTotal); confirmarCobro = false
            }) { Text("Sí, pago recibido") } },
            dismissButton = { TextButton(onClick = { confirmarCobro = false }) { Text("Cancelar") } })
    }
}

// ==========================================
// 4. BASE DE DATOS DE PRECIOS EDITABLES
// ==========================================
@Composable
fun PreciosBaseScreen(preciosBase: MutableMap<String, Int>) {
    var materialEditando by remember { mutableStateOf<String?>(null) }
    var nuevoPrecioInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Mis Materiales & Precios", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Toca un material para actualizar su valor de referencia", fontSize = 13.sp, color = TextMuted)
        }

        items(preciosBase.keys.toList()) { nombre ->
            val precio = preciosBase[nombre] ?: 0
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        materialEditando = nombre
                        nuevoPrecioInput = precio.toString()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(nombre, color = TextMain, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(formatoCLP(precio), color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (materialEditando != null) {
        AlertDialog(
            onDismissRequest = { materialEditando = null },
            containerColor = BgCard,
            title = { Text("Editar Precio", color = TextMain) },
            text = {
                Column {
                    Text(materialEditando ?: "", color = TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuevoPrecioInput,
                        onValueChange = { nuevoPrecioInput = it },
                        label = { Text("Precio unitario (CLP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = nuevoPrecioInput.toIntOrNull()
                    if (p != null && p > 0 && materialEditando != null) {
                        preciosBase[materialEditando!!] = p
                    }
                    materialEditando = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { materialEditando = null }) { Text("Cancelar", color = TextMuted) }
            }
        )
    }
}
