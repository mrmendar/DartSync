plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.ksp) apply false // Room için gerekli, duruyor.
    alias(libs.plugins.google.gms.google.services) apply false // Firebase için bu yeterli.
}