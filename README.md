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


## Compatibilidad con celular, Android TV y TV Box
- La interfaz web usa diseño adaptable: se reorganiza según el ancho disponible de la pantalla.
- El manifiesto declara que la pantalla táctil es opcional y agrega la categoría de inicio Android TV (Leanback), para que el launcher de algunos TV Box pueda detectar M23 TV.
- La detección del launcher no garantiza compatibilidad con todos los modelos ni navegación completa con control remoto; hay que probar el APK en el equipo real.
- Para instalar, descargá el APK generado en la sección **Actions** del repositorio: https://github.com/maxjavrol-ui/M23tv-android/actions → abrir la última ejecución exitosa → **Artifacts** → descargar el ZIP y extraer el APK. En Android TV/TV Box, transferilo por USB o desde una app de archivos y habilitá la instalación desde esa fuente si el sistema lo solicita.
- Los artefactos de Actions son temporales. Para distribuir una versión estable, hay que publicar un Release con el APK.

## Canales
M23 TV puede cargar listas M3U/M3U Plus que el usuario tenga derecho a utilizar. No es posible garantizar “todos los canales”: las señales cambian, algunas requieren suscripción y los enlaces públicos pueden dejar de funcionar. Solo deben agregarse fuentes públicas/autorizadas; la app no desbloquea servicios de pago ni reemplaza una suscripción.
