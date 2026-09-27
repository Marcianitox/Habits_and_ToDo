package com.juanti.organizador.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.juanti.organizador.R

// Títulos: Bricolage Grotesque
val Bricolage = FontFamily(
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold)
)

// Texto: Figtree
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold)
)

private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
    displaySmall = Base.displaySmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),

    headlineLarge = Base.headlineLarge.copy(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp
    ),

    titleLarge = Base.titleLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(
        fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 18.sp
    ),
    titleSmall = Base.titleSmall.copy(
        fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
    ),

    bodyLarge = Base.bodyLarge.copy(fontFamily = Figtree),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Figtree),
    bodySmall = Base.bodySmall.copy(fontFamily = Figtree),

    labelLarge = Base.labelLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.Medium),
    labelSmall = Base.labelSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.Medium)
)