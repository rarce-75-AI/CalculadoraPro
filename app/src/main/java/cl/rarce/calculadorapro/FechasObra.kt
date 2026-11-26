package cl.rarce.calculadorapro

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Fechas civiles sin efectos del horario de verano. No cambia el reloj del usuario. */
internal object FechasObra {
    private fun formato() = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parsear(valor: String): Date? {
        if (!Regex("\\d{1,2}/\\d{1,2}/\\d{4}").matches(valor)) return null
        return runCatching { formato().parse(valor) }.getOrNull()
    }

    fun termino(inicio: String, dias: Int): String? {
        val fecha = parsear(inicio) ?: return null
        val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = fecha
            add(Calendar.DAY_OF_MONTH, dias)
        }
        return formato().format(calendario.time)
    }

    fun diasRestantes(inicio: String, dias: Int, hoy: String): Long? {
        val fin = termino(inicio, dias)?.let(::parsear) ?: return null
        val actual = parsear(hoy) ?: return null
        return (fin.time - actual.time) / 86_400_000L
    }
}
