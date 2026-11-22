# Screen: ProfileScreen

## Descripción
Gestiona la información del perfil del usuario y la configuración personal. También actúa como centro de diagnóstico de errores.

## Funcionamiento
- Muestra los datos actuales (Nombre, Email).
- Permite actualizar la información del perfil y cambiar la contraseña.
- **Crash Reporter:** Muestra logs de errores si la aplicación sufrió un cierre inesperado anteriormente.
- **Personas:** abre la gestión de personas propias y compartidas.
- **Actualizaciones:** consulta la última versión, verifica y descarga el APK y abre el instalador de Android.
- Opción de Cerrar Sesión: Limpia los datos locales y la caché de los widgets.

## Endpoints Llamados
- `PATCH /user/update`: Para actualizar los datos del perfil.

## Ficheros de Código
- `app/android/app/src/main/java/com/lito/planify/ui/screens/ProfileScreen.kt`
- `app/android/app/src/main/java/com/lito/planify/viewmodel/AuthViewModel.kt`
- `app/android/app/src/main/java/com/lito/planify/data/local/SessionManager.kt`
