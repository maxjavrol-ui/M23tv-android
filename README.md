# M23 TV — proyecto Android inicial

Proyecto Android nativo con una interfaz WebView local, catálogo informativo de ejemplo y enlaces a la guía oficial de Flow.

## Estado
- Interfaz adaptable a celulares Android.
- Búsqueda y filtros por categorías.
- Enlaces a las páginas oficiales de Flow.
- No contiene retransmisiones, credenciales, ni datos de programación en vivo.
- El catálogo inicial es demostrativo y requiere validar la disponibilidad de cada canal por región y plan.

## Generar APK
Abrir esta carpeta en Android Studio con JDK 17 y Android SDK Platform 35 instalado. Sincronizar Gradle y ejecutar `Build > Build APK(s)`.

El entorno donde se preparó este paquete no tiene Android SDK/Gradle instalados, así que este ZIP contiene el proyecto fuente y todavía no es un APK compilado.


## Reproductor en vivo
La interfaz incluye un reproductor integrado para señales públicas compatibles. La disponibilidad de cada transmisión depende del emisor. Los canales que requieren suscripción se abren mediante sus accesos oficiales y no se desbloquean ni se copian flujos privados.
