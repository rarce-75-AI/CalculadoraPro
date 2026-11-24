# CalculadoraPro — Android Kotlin (v0.3.0)

Proyecto independiente para `rarce-75-AI/CalculadoraPro` (no mezclar con Mis Finanzas o ImageDetect).

## Caracteristicas

Cuatro pantallas: **Finanzas**, **Calculos**, **Presupuesto**, **Precios**. Guarda movimientos y precios localmente. No incluye ingresos ficticios. No confunde presupuesto con ingreso cobrado. Calculos de materiales orientativos.

## Configurar GitHub

1. Crear repositorio vacio **CalculadoraPro** en la cuenta **rarce-75-AI**.
2. Subir el **contenido** de este ZIP a la raiz (no subir el ZIP como archivo individual).
3. En **Settings → Secrets and variables → Actions → New repository secret**, agregar:
   - `CALCPRO_KEYSTORE_B64`: todo el contenido de `keystore-base64.txt` del **paquete de firma privado**.
   - `CALCPRO_STORE_PASSWORD`: valor indicado en `instrucciones-firma.txt`.
   - `CALCPRO_KEY_ALIAS`: valor indicado en `instrucciones-firma.txt`.
   - `CALCPRO_KEY_PASSWORD`: valor indicado en `instrucciones-firma.txt`.
4. En **Actions**, la compilacion genera APK y AAB de publicacion y los deja en **Releases**.

**Guarda el paquete privado por separado; nunca lo subas al repositorio.** Si pierdes la clave, puedes perder la capacidad de actualizar instalaciones firmadas con ella. No habilitar la distribucion comercial hasta probar el APK en el telefono.

## Tecnologias
Kotlin 1.9.25, Jetpack Compose, Android Gradle Plugin 8.10.1, Gradle 8.11.1, SDK 36, Java 17.

## Estado
El codigo base v0.2.0 fue compilado en GitHub. Esta actualizacion v0.3.0 tiene nueva configuracion de SDK y firma y requiere una compilacion propia en el repositorio nuevo para comprobarla.
