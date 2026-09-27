# Reglas para la versión release (optimizada con R8)

# Widgets: las acciones (tildar tareas y hábitos) se crean por reflexión
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

# Recordatorios: el trabajador se crea por reflexión desde WorkManager
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}