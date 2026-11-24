package cl.rarce.calculadorapro

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class PresupuestoGuardado(
    val id: Long,
    val cliente: String,
    val obra: String,
    val fecha: String,
    val manoDeObra: Int,
    val partidas: List<MaterialItem>
) {
    val total: Long get() = partidas.sumOf { it.total.toLong() } + manoDeObra
}
fun fechaPresupuesto(): String = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date())
fun cargarPresupuestos(context: Context): List<PresupuestoGuardado> =
    runCatching {
        val prefs = context.getSharedPreferences("calculadora_pro_v2", Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString("presupuestos_guardados_v1", "[]") ?: "[]")
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val detalles = obj.optJSONArray("partidas") ?: JSONArray()
            PresupuestoGuardado(
                obj.getLong("id"), obj.optString("cliente"), obj.optString("obra"),
                obj.optString("fecha"), obj.optInt("manoDeObra"),
                (0 until detalles.length()).map { j ->
                    val p = detalles.getJSONObject(j)
                    MaterialItem(p.getString("nombre"), p.getString("cantidad"),
                        p.getInt("precio"), p.getInt("total"))
                }
            )
        }.sortedByDescending { it.id }
    }.getOrDefault(emptyList())

fun guardarPresupuestos(context: Context, presupuestos: List<PresupuestoGuardado>) {
    val array = JSONArray()
    presupuestos.forEach { p ->
        val partidas = JSONArray()
        p.partidas.forEach { m ->
            partidas.put(JSONObject().apply {
                put("nombre", m.nombre); put("cantidad", m.cantidad)
                put("precio", m.precioUnitario); put("total", m.total)
            })
        }
        array.put(JSONObject().apply {
            put("id", p.id); put("cliente", p.cliente); put("obra", p.obra)
            put("fecha", p.fecha); put("manoDeObra", p.manoDeObra)
            put("partidas", partidas)
        })
    }
    context.getSharedPreferences("calculadora_pro_v2", Context.MODE_PRIVATE)
        .edit().putString("presupuestos_guardados_v1", array.toString()).apply()
}

private fun escapeXml(s: String): String = s.replace("&", "&amp;")
    .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    .replace("'", "&apos;").filter { it == '\n' || it == '\t' || it.code >= 32 }

private fun xmlCell(value: String, index: Int): String =
    "<c r=\"$index\" t=\"inlineStr\"><is><t>" + escapeXml(value) + "</t></is></c>"
private fun xmlNumber(value: Long, index: Int): String =
    "<c r=\"$index\"><v>$value</v></c>"

fun exportarPresupuestoExcel(context: Context, uri: Uri, p: PresupuestoGuardado) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        ZipOutputStream(stream).use { zip ->
            fun entry(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write(xml.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            val ct = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="$ct.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="$ct.worksheet+xml"/>
</Types>""".replace("$ct.main+xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml")
                .replace("$ct.worksheet+xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"))
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
            entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Presupuesto" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""")
            val rows = mutableListOf<List<String>>()
            rows += listOf("CÁLCULO DE OBRA — PRESUPUESTO")
            rows += listOf("N°", p.id.toString())
            rows += listOf("Cliente", p.cliente)
            rows += listOf("Obra", p.obra)
            rows += listOf("Fecha", p.fecha)
            rows += listOf("")
            rows += listOf("Partida / Material", "Cantidad", "Precio unitario (CLP)", "Total (CLP)")
            p.partidas.forEach { rows += listOf(it.nombre, it.cantidad, it.precioUnitario.toString(), it.total.toString()) }
            rows += listOf("Mano de obra", "", "", p.manoDeObra.toString())
            rows += listOf("TOTAL (CLP)", "", "", p.total.toString())
            val sheet = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols><col min="1" max="1" width="52" customWidth="1"/><col min="2" max="2" width="24" customWidth="1"/><col min="3" max="4" width="24" customWidth="1"/></cols><sheetData>""")
                rows.forEachIndexed { rowIndex, fields ->
                    val rn = rowIndex + 1
                    append("<row r=\"$rn\">")
                    fields.forEachIndexed { colIndex, value ->
                        val ref = ('A'.code + colIndex).toChar().toString() + rn
                        val numeric = rowIndex >= 7 && colIndex >= 2 && value.toLongOrNull() != null
                        append(if (numeric) xmlNumber(value.toLong(), colIndex) .replace("r=\"$colIndex\"", "r=\"$ref\"")
                            else xmlCell(value, colIndex).replace("r=\"$colIndex\"", "r=\"$ref\""))
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    } ?: error("No se pudo abrir el archivo Excel.")
}

fun exportarPresupuestoPdf(context: Context, uri: Uri, p: PresupuestoGuardado) {
    val pdf = PdfDocument()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 11f }
    val ancho = 595
    val alto = 842
    val margen = 42f
    var pagina = 0
    var y = 0f
    var canvas: android.graphics.Canvas? = null
    var page: PdfDocument.Page? = null
    fun nuevaPagina() {
        page?.let { pdf.finishPage(it) }
        pagina++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(ancho, alto, pagina).create())
        canvas = page!!.canvas
        y = margen
    }
    fun linea(valor: String, titulo: Boolean = false) {
        if (canvas == null || y > alto - margen) nuevaPagina()
        paint.typeface = if (titulo) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        paint.textSize = if (titulo) 15f else 10f
        val maxWidth = ancho - 2 * margen
        var texto = valor
        if (texto.isEmpty()) { y += 12; return }
        while (texto.isNotEmpty()) {
            val n = paint.breakText(texto, true, maxWidth, null).coerceAtLeast(1)
            var cortar = n
            if (n < texto.length) {
                val ultimoEspacio = texto.substring(0, n).lastIndexOf(' ')
                if (ultimoEspacio > n / 2) cortar = ultimoEspacio + 1
            }
            if (y > alto - margen) nuevaPagina()
            canvas!!.drawText(texto.take(cortar).trim(), margen, y, paint)
            y += if (titulo) 24 else 16
            texto = texto.drop(cortar).trimStart()
        }
    }
    try {
        nuevaPagina()
        linea("CÁLCULO DE OBRA", true)
        linea("PRESUPUESTO N° " + p.id)
        linea("Cliente: " + p.cliente)
        linea("Obra: " + p.obra)
        linea("Fecha: " + p.fecha)
        linea("")
        linea("PARTIDAS", true)
        p.partidas.forEachIndexed { i, m ->
            linea("${i + 1}. ${m.nombre}")
            linea("   ${m.cantidad}   x   ${formatoCLP(m.precioUnitario)}   =   ${formatoCLP(m.total)}")
        }
        linea("")
        linea("Mano de obra: " + formatoCLP(p.manoDeObra))
        linea("TOTAL ESTIMADO: " + formatoCLP(p.total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), true)
        page?.let { pdf.finishPage(it) }
        context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            ?: error("No se pudo abrir el archivo PDF.")
    } finally { pdf.close() }
}
