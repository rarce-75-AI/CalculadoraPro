# Calculadora de Obra · KIMORA SOFTWARE

## Corrección propuesta: calendario de obra

El cálculo anterior dividía milisegundos de medianoches locales por 24 horas. Un cambio de hora podía reducir por truncamiento el número de días restantes. Además, el analizador podía aceptar texto sobrante tras una fecha.

La propuesta utiliza fechas civiles en UTC para validar, sumar y comparar días. La fecha de hoy continúa obteniéndose en la zona local del dispositivo. No cambia presupuestos, anticipos, movimientos, materiales ni el almacenamiento existente.

Validación automatizada: fechas inexistentes, formato completo DD/MM/AAAA, bisiestos, fin de mes y año, vencimiento, atraso y ejecución bajo distintas zonas horarias. Compilación de APK debug y AAB sin firma para QA.

Pendiente antes de publicar: resultado de CI, pruebas de instalación y actualización en dispositivo, versión de publicación y paquete firmado con la clave existente. Los artefactos de QA no se deben subir a Play Console.
