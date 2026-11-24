package cl.rarce.calculadorapro

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class PerfilProfesional(
    val nombre: String = "",
    val empresa: String = "",
    val telefono: String = "",
    val correo: String = "",
    val direccion: String = ""
) {
    val encabezado: String get() = empresa.ifBlank { nombre.ifBlank { "Cálculo de Obra" } }
}
fun cargarPerfil(context: Context): PerfilProfesional {
    val p = context.getSharedPreferences("perfil_profesional_v1", Context.MODE_PRIVATE)
    return PerfilProfesional(
        p.getString("nombre", "") ?: "", p.getString("empresa", "") ?: "",
        p.getString("telefono", "") ?: "", p.getString("correo", "") ?: "",
        p.getString("direccion", "") ?: ""
    )
}
fun guardarPerfil(context: Context, p: PerfilProfesional) {
    context.getSharedPreferences("perfil_profesional_v1", Context.MODE_PRIVATE).edit()
        .putString("nombre", p.nombre.trim()).putString("empresa", p.empresa.trim())
        .putString("telefono", p.telefono.trim()).putString("correo", p.correo.trim())
        .putString("direccion", p.direccion.trim()).apply()
}

@Composable
fun EditorPerfilProfesional(
    inicial: PerfilProfesional,
    onCerrar: () -> Unit,
    onGuardar: (PerfilProfesional) -> Unit
) {
    var nombre by remember(inicial) { mutableStateOf(inicial.nombre) }
    var empresa by remember(inicial) { mutableStateOf(inicial.empresa) }
    var telefono by remember(inicial) { mutableStateOf(inicial.telefono) }
    var correo by remember(inicial) { mutableStateOf(inicial.correo) }
    var direccion by remember(inicial) { mutableStateOf(inicial.direccion) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Mi perfil profesional") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estos datos aparecerán en el encabezado del PDF y Excel. No necesitas crear cuenta ni conectarte a internet.")
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del responsable") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(empresa, { empresa = it }, label = { Text("Constructora / nombre comercial (opcional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(telefono, { telefono = it }, label = { Text("Teléfono de contacto") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(correo, { correo = it }, label = { Text("Correo electrónico (opcional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(direccion, { direccion = it }, label = { Text("Dirección / ciudad (opcional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Se usará el ícono de Cálculo de Obra como logo del documento.")
            }
        },
        confirmButton = {
            Button(onClick = {
                onGuardar(PerfilProfesional(nombre, empresa, telefono, correo, direccion))
            }, enabled = nombre.isNotBlank() || empresa.isNotBlank()) { Text("Guardar perfil") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
