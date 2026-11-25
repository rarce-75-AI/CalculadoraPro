package cl.rarce.calculadorapro

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class FechasObraTest {
    @Test fun rechazaFechasInexistentesYTextoSobrante() {
        listOf("31/02/2026", "29/02/2026", "01/13/2026", "00/01/2026",
            "01/03/2026 extra", "01/03/2026\n", "1/3/2026", "").forEach {
            assertNull(it, FechasObra.parsear(it))
        }
        assertNotNull(FechasObra.parsear("29/02/2024"))
    }

    @Test fun sumaDiasEntreMesesYAnos() {
        assertEquals("01/01/2027", FechasObra.termino("31/12/2026", 1))
        assertEquals("01/03/2024", FechasObra.termino("28/02/2024", 2))
        assertEquals("01/03/2026", FechasObra.termino("28/02/2026", 1))
    }

    @Test fun diasNoDependenDelCambioDeHoraDeChile() {
        val anterior = TimeZone.getDefault()
        try {
            for (zona in listOf("America/Santiago", "UTC", "Pacific/Auckland")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zona))
                assertEquals("07/09/2026", FechasObra.termino("05/09/2026", 2))
                assertEquals(1L, FechasObra.diasRestantes("05/09/2026", 2, "06/09/2026"))
                assertEquals(2L, FechasObra.diasRestantes("04/04/2026", 2, "04/04/2026"))
            }
        } finally {
            TimeZone.setDefault(anterior)
        }
    }

    @Test fun distingueVencimientoHoyYAtraso() {
        assertEquals(0L, FechasObra.diasRestantes("01/11/2026", 5, "06/11/2026"))
        assertEquals(-1L, FechasObra.diasRestantes("01/11/2026", 5, "07/11/2026"))
        assertNull(FechasObra.diasRestantes("31/02/2026", 5, "01/03/2026"))
        assertNull(FechasObra.diasRestantes("01/03/2026", 5, "inválida"))
    }
}
