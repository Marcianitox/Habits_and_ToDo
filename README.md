# Habityx

Aplicación Android para organizar la vida académica y personal en un solo lugar: **plan del día**, **deadlines** (exámenes y entregas), **hábitos** y **estadísticas de progreso**. Funciona 100 % sin conexión: todos los datos se guardan en el dispositivo.
La diferencia con otras aplicaciones del estilo es que permite cargar deadlines (fechas de exámenes, tareas, etc) y armar un plan de estudio para cumplir esa deadline. Por ejemplo, permite cargar  "exámen de física el viernes" y luego "estudiar para [exámen de física]" los 3 días previos, teniendo un panorama tanto de las fechas de entrega como las acciones a tomar cada día.
A su vez, incorpora también un tracker de hábitos, para poder tener todo organizado desde la misma aplicación.

## Funcionalidades

- **Plan**: tareas por día, opcionalmente vinculadas a un deadline.
- **Deadlines**: exámenes, entregas u otros, con materia, notas y estado de completado. Cada uno puede tener recordatorios (X días antes, a la hora elegida) mediante notificaciones.
- **Hábitos**: cuatro tipos de frecuencia:
  - diaria,
  - cada N días,
  - días de la semana específicos,
  - veces por semana.

  Incluye calendario por hábito y archivado.
- **Progreso**: cumplimiento de hábitos y tareas (hechos vs. esperados) con gráficos.
- **Widgets** de pantalla de inicio (Glance): plan del día, hábitos, deadlines y semana/calendario de hábitos. Permiten carga rápida desde una ventana flotante.
- **Temas**: cuatro paletas (Tinta y Girasol, Día, Algodón y Lavanda Nocturna).

## Tecnologías

| Área | Herramienta |
|---|---|
| Lenguaje | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| Base de datos | Room (con KSP) |
| Widgets | Jetpack Glance |
| Recordatorios | WorkManager + notificaciones |
| Build | Gradle (Kotlin DSL), AGP 9.2 |

Requisitos de la app: Android 8.0 (API 26) o superior; `compileSdk`/`targetSdk` 36.

## Estructura del proyecto

```
app/src/main/java/com/juanti/organizador/
├── MainActivity.kt            # Actividad principal
├── ActividadCargaRapida.kt    # Ventana flotante de carga rápida (widgets)
├── data/                      # Room: entidades, DAOs, lógica de hábitos y estadísticas
├── recordatorios/             # Notificaciones de deadlines (WorkManager)
├── widget/                    # Widgets de Glance
└── ui/theme/                  # Pantallas, componentes, gráficos y paletas
```

## Compilar y ejecutar

Requisitos: Android Studio reciente (compatible con AGP 9.2) y JDK 11 o superior.

1. Clonar el repositorio y abrirlo en Android Studio.
2. Esperar a que Gradle sincronice.
3. Ejecutar en un emulador o dispositivo (API 26+).

Desde la terminal:

```bash
./gradlew assembleDebug     # APK de depuración
./gradlew testDebugUnitTest # Tests unitarios
```

> La versión *release* usa la misma clave que la de depuración, de modo que se puede instalar encima sin perder datos. Para publicar, configurá tu propia firma.

## Permisos

- `POST_NOTIFICATIONS`: para mostrar los recordatorios de deadlines.
