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
            val responsable = cargarPerfil(context)
            rows += listOf("CÁLCULO DE OBRA — PRESUPUESTO")
            rows += listOf("Responsable", responsable.nombre)
            rows += listOf("Constructora", responsable.empresa)
            rows += listOf("Teléfono", responsable.telefono)
            rows += listOf("Correo", responsable.correo)
            rows += listOf("Dirección", responsable.direccion)
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
                        val numeric = colIndex >= 2 && value.toLongOrNull() != null
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
    val perfil = cargarPerfil(context)
    val pdf = PdfDocument()
    val width = 595f
    val height = 842f
    val margin = 40f
    val dark = android.graphics.Color.rgb(12, 34, 44)
    val green = android.graphics.Color.rgb(18, 119, 86)
    val muted = android.graphics.Color.rgb(94, 109, 118)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var current: PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null
    var pageNumber = 0
    var y = 0f

    fun rect(l: Float, t: Float, r: Float, b: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        canvas!!.drawRect(l, t, r, b, paint)
    }
    fun text(value: String, x: Float, baseline: Float, size: Float = 10f,
             color: Int = dark, bold: Boolean = false) {
        paint.color = color
        paint.textSize = size
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        paint.style = Paint.Style.FILL
        canvas!!.drawText(value, x, baseline, paint)
    }
    fun newPage() {
        current?.let { pdf.finishPage(it) }
        pageNumber++
        current = pdf.startPage(PdfDocument.PageInfo.Builder(width.toInt(), height.toInt(), pageNumber).create())
        canvas = current!!.canvas
        rect(0f, 0f, width, 102f, dark)
        runCatching {
            val logo = context.getDrawable(R.drawable.ic_launcher)
            logo?.setBounds(40, 19, 100, 79)
            logo?.draw(canvas!!)
        }
        text(perfil.encabezado.take(53), 112f, 43f, 18f, android.graphics.Color.WHITE, true)
        val subtitulo = if (perfil.empresa.isNotBlank()) perfil.nombre else "CÁLCULO DE OBRA · PRESUPUESTO"
        text(subtitulo.take(75), 112f, 62f, 10f, android.graphics.Color.WHITE)
        if (perfil.telefono.isNotBlank())
            text(("Tel.: " + perfil.telefono).take(75), 112f, 79f, 9f, android.graphics.Color.WHITE)
        rect(margin, height - 34f, width - margin, height - 33f, android.graphics.Color.LTGRAY)
        text("Cálculo de Obra · Documento generado desde la aplicación", margin, height - 18f, 8f, muted)
        text("Pág. $pageNumber", width - 83f, height - 18f, 8f, muted)
        y = 128f
    }
    fun space(h: Float) {
        if (y + h > height - 55f) newPage()
    }
    fun line(value: String, size: Float = 10f, color: Int = dark, bold: Boolean = false,
             indent: Float = margin, maxW: Float = width - 2 * margin) {
        paint.textSize = size
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        var remaining = value.trim()
        if (remaining.isEmpty()) { y += 12f; return }
        while (remaining.isNotEmpty()) {
            space(size + 9f)
            val count = paint.breakText(remaining, true, maxW, null).coerceAtLeast(1)
            var split = count
            if (count < remaining.length) {
                val pos = remaining.substring(0, count).lastIndexOf(' ')
                if (pos > count / 2) split = pos + 1
            }
            text(remaining.take(split).trim(), indent, y, size, color, bold)
            y += size + 7f
            remaining = remaining.drop(split).trimStart()
        }
    }
    fun section(title: String) {
        space(34f)
        y += 12f
        rect(margin, y - 12f, width - margin, y + 14f, android.graphics.Color.rgb(232, 243, 238))
        text(title, margin + 10f, y + 6f, 11f, green, true)
        y += 31f
    }
    try {
        newPage()
        line("PRESUPUESTO N° " + p.id, 16f, dark, true)
        line("Fecha: " + p.fecha, 10f, muted)
        line("Cliente: " + p.cliente, 12f, dark, true)
        if (p.obra.isNotBlank()) line("Obra: " + p.obra, 10f)
        if (perfil.correo.isNotBlank()) line("Correo responsable: " + perfil.correo, 9f, muted)
        if (perfil.direccion.isNotBlank()) line("Dirección responsable: " + perfil.direccion, 9f, muted)

        val grupos = p.partidas.groupBy { item ->
            val partes = item.nombre.split(" / ")
            if (partes.size >= 3) partes.take(2).joinToString(" · ") else "Otras partidas"
        }
        grupos.forEach { (titulo, detalles) ->
            section(titulo)
            detalles.forEach { item ->
                space(43f)
                line(item.nombre.substringAfterLast(" / "), 10f, dark, true)
                line(item.cantidad + "  ·  Unitario: " + formatoCLP(item.precioUnitario) +
                    "  ·  Importe: " + formatoCLP(item.total), 9f, muted)
                y += 3f
            }
            space(24f)
            val subtotal = detalles.sumOf { it.total.toLong() }
            line("Subtotal " + titulo + ": $" + "%,d".format(Locale("es", "CL"), subtotal) + " CLP",
                10f, green, true)
        }
        section("RESUMEN ECONÓMICO")
        val subtotalMat = p.partidas.sumOf { it.total.toLong() }
        line("Materiales y partidas: $" + "%,d".format(Locale("es", "CL"), subtotalMat) + " CLP", 11f)
        line("Mano de obra: " + formatoCLP(p.manoDeObra), 11f)
        space(52f)
        rect(margin, y - 2f, width - margin, y + 42f, dark)
        text("TOTAL ESTIMADO", margin + 12f, y + 15f, 10f, android.graphics.Color.WHITE, true)
        text("$" + "%,d".format(Locale("es", "CL"), p.total) + " CLP",
            margin + 12f, y + 34f, 16f, android.graphics.Color.WHITE, true)
        y += 57f
        line("Valores de carácter presupuestario, sujetos a verificación de cantidades y condiciones de obra.",
            8f, muted)
        current?.let { pdf.finishPage(it) }
        context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            ?: error("No se pudo abrir el archivo PDF")
    } finally {
        pdf.close()
    }
}
