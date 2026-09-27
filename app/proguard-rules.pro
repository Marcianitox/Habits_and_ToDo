# Reglas para la versión release (optimizada con R8)

# Widgets: cada widget tiene que conservar su propia clase y su nombre,
# porque Glance los distingue por el nombre de la clase.
# Sin esto, R8 puede fusionarlos y un widget muestra el contenido de otro.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# Widgets: las acciones (tildar tareas y hábitos) se crean por reflexión
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

# Recordatorios: el trabajador se crea por reflexión desde WorkManager
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}