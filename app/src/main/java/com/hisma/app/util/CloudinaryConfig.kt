package com.hisma.app.util

object CloudinaryConfig {
    const val CLOUD_NAME = "dcf4bewcl"
    const val UPLOAD_PRESET = "ml_default"
    const val FOLDER = "hismafoto"

    // Método para obtener la configuración
    fun getConfig(): HashMap<String, String> {
        return hashMapOf(
            "cloud_name" to CLOUD_NAME
        )
    }
}