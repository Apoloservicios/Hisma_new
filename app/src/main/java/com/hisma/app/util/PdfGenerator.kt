package com.hisma.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.hisma.app.R
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.ui.oilchange.OilChangeData
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clase utilitaria para generar PDF de cambios de aceite con formato optimizado
 */
class PdfGenerator {

    companion object {
        // Definir colores
        private val COLOR_BLUE = DeviceRgb(0, 92, 170)
        private val COLOR_YELLOW = DeviceRgb(230, 200, 0)
        private val COLOR_RED = DeviceRgb(200, 30, 30)
        private val COLOR_GREEN = DeviceRgb(0, 150, 0)
        private val COLOR_BLACK = DeviceRgb(0, 0, 0)

        // Formateadores de fecha
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        /**
         * Genera un PDF para un cambio de aceite
         * @param context Contexto de la aplicación
         * @param lubricenter Datos del lubricentro
         * @param oilChangeData Datos del cambio de aceite
         * @return Archivo PDF generado
         */
        fun generateOilChangePdf(context: Context, lubricenter: Lubricenter, oilChangeData: OilChangeData): File {
            // Crear nombre de archivo basado en la patente y fecha
            val fileNameFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "cambio_aceite_${oilChangeData.vehiclePlate}_${fileNameFormatter.format(Date())}.pdf"

            // Preparar archivo
            val pdfFile = File(context.filesDir, fileName)
            if (pdfFile.exists()) {
                pdfFile.delete()
            }

            try {
                // Inicializar documento PDF
                val writer = PdfWriter(pdfFile)
                val pdf = PdfDocument(writer)
                val document = Document(pdf, PageSize.A4)
                document.setMargins(20f, 20f, 20f, 20f) // Márgenes más estrechos

                // Agregar logo y encabezado
                addHeader(context, document, lubricenter)

                // Agregar información del ticket, operario y fecha en una misma línea
                addServiceInfo(document, oilChangeData)

                // Agregar información del vehículo y cliente
                addVehicleInfo(document, oilChangeData)

                // Agregar información de aceite y filtros en una misma tabla
                addOilAndFiltersInfo(document, oilChangeData)

                // Agregar información de extras
                addExtrasInfo(document, oilChangeData)

                // Agregar observaciones si existen
                if (oilChangeData.observations.isNotBlank()) {
                    addObservationsSection(document, oilChangeData.observations)
                }

                // Agregar pie de página
                addFooter(document)

                // Cerrar documento
                document.close()

                return pdfFile
            } catch (e: Exception) {
                Log.e("PdfGenerator", "Error generando PDF", e)
                throw e
            }
        }

        /**
         * Agrega el encabezado con logo y datos del lubricentro
         */
        private fun addHeader(context: Context, document: Document, lubricenter: Lubricenter) {
            try {
                // Crear tabla para el encabezado
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 80f)))
                headerTable.useAllAvailableWidth()

                // Logo del lubricentro
                val logoCell = Cell()
                    .setBorder(Border.NO_BORDER)

                // Intentar cargar logo real del lubricentro, si existe
                if (lubricenter.logoUrl.isNotEmpty()) {
                    // En una implementación real, cargaríamos la imagen desde la URL
                    val logoImage = getLubricenterLogoFromUrl(context, lubricenter.logoUrl)
                    if (logoImage != null) {
                        logoCell.add(logoImage)
                    } else {
                        // Si falla, usar el logo de placeholder
                        val fallbackImage = getLubricenterLogo(context)
                        if (fallbackImage != null) {
                            logoCell.add(fallbackImage)
                        }
                    }
                } else {
                    // Usar logo de Hisma como fallback
                    val hismaLogoImage = getHismaLogo(context)
                    if (hismaLogoImage != null) {
                        logoCell.add(hismaLogoImage)
                    }
                }

                headerTable.addCell(logoCell)

                // Información del lubricentro
                val infoCell = Cell()
                    .setBorder(Border.NO_BORDER)

                // Crear fuentes
                val titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
                val subtitleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

                val titleParagraph = Paragraph(lubricenter.fantasyName.uppercase())
                    .setFont(titleFont)
                    .setFontSize(16f)
                    .setFontColor(COLOR_BLUE)

                infoCell.add(titleParagraph)

                // Agregar información del lubricentro de manera más compacta
                val infoTable = Table(2)
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER)

                // Primera columna
                infoTable.addCell(createInfoCell("Responsable:", subtitleFont, 10f))
                infoTable.addCell(createInfoCell(lubricenter.responsible, subtitleFont, 10f))

                infoTable.addCell(createInfoCell("Teléfono:", subtitleFont, 10f))
                infoTable.addCell(createInfoCell(lubricenter.phone, subtitleFont, 10f))

                // Segunda columna
                infoTable.addCell(createInfoCell("Dirección:", subtitleFont, 10f))
                infoTable.addCell(createInfoCell(lubricenter.address, subtitleFont, 10f))

                infoTable.addCell(createInfoCell("E-mail:", subtitleFont, 10f))
                infoTable.addCell(createInfoCell(lubricenter.email, subtitleFont, 10f))

                infoCell.add(infoTable)
                headerTable.addCell(infoCell)

                document.add(headerTable)

                // Agregar línea divisoria
                val divider = Paragraph()
                divider.setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                document.add(divider)

            } catch (e: Exception) {
                Log.e("PdfGenerator", "Error al agregar encabezado", e)
            }
        }

        /**
         * Crea una celda para información con formato estándar
         */
        private fun createInfoCell(text: String, font: com.itextpdf.kernel.font.PdfFont, fontSize: Float): Cell {
            return Cell()
                .setBorder(Border.NO_BORDER)
                .add(Paragraph(text).setFont(font).setFontSize(fontSize))
        }

        /**
         * Agrega información del servicio, operario y fecha
         */
        private fun addServiceInfo(document: Document, oilChangeData: OilChangeData) {
            val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 30f, 40f)))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            // Ticket ID
            val ticketCell = Cell()
                .setBorder(Border.NO_BORDER)

            ticketCell.add(Paragraph("Ticket: ${oilChangeData.ticketId}").setBold())
            infoTable.addCell(ticketCell)

            // Operario
            val operatorCell = Cell()
                .setBorder(Border.NO_BORDER)

            operatorCell.add(Paragraph("Operario: ${oilChangeData.operatorName}"))
            infoTable.addCell(operatorCell)

            // Fecha
            val dateCell = Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)

            dateCell.add(Paragraph("Fecha: ${dateFormatter.format(Date(oilChangeData.serviceDate))}"))
            infoTable.addCell(dateCell)

            document.add(infoTable)
        }

        /**
         * Agrega la información del vehículo y cliente
         */
        private fun addVehicleInfo(document: Document, oilChangeData: OilChangeData) {
            // Caja de datos del vehículo con borde
            val vehicleTable = Table(1)
                .useAllAvailableWidth()
                .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(8f)

            // Tabla interna para datos del vehículo
            val vehicleInfoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            // Primera fila: Cliente y Vehículo
            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .add(Paragraph("Cliente: ${oilChangeData.customerName}"))
            )

            // Marca y modelo del vehículo
            val brandModel = StringBuilder()
            if (oilChangeData.vehicleBrand.isNotBlank()) {
                brandModel.append(oilChangeData.vehicleBrand)
            }
            if (oilChangeData.vehicleModel.isNotBlank()) {
                if (brandModel.isNotEmpty()) brandModel.append(" ")
                brandModel.append(oilChangeData.vehicleModel)
            }

            if (oilChangeData.vehicleYear > 0) {
                brandModel.append(" (${oilChangeData.vehicleYear})")
            }

            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(Paragraph("Vehículo: $brandModel"))
            )

            // Segunda fila: Teléfono y Patente
            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .add(Paragraph("Teléfono: ${oilChangeData.customerPhone}"))
            )

            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(Paragraph("Patente: ${oilChangeData.vehiclePlate}").setBold())
            )

            // Tercera fila: Kilometraje actual y próximo
            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .add(Paragraph("KM actuales: ${oilChangeData.currentKm}"))
            )

            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(Paragraph("Próx. cambio: ${oilChangeData.nextChangeKm} km"))
            )

            // Cuarta fila: Periodicidad
            vehicleInfoTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .add(Paragraph("Periodicidad: ${oilChangeData.periodMonths} meses"))
            )

            // Celda vacía para mantener el balance
            vehicleInfoTable.addCell(Cell().setBorder(Border.NO_BORDER))

            // Agregar la tabla interna a la tabla principal
            vehicleTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .add(vehicleInfoTable)
            )

            document.add(vehicleTable)
            document.add(Paragraph("").setMarginBottom(5f))
        }

        /**
         * Agregar información de aceite y filtros en formato compacto
         */
        private fun addOilAndFiltersInfo(document: Document, oilChangeData: OilChangeData) {
            // Tabla principal con dos columnas
            val mainTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .useAllAvailableWidth()

            // Columna izquierda: Aceite
            val oilCell = Cell().setPadding(5f)

            // Título de aceite
            oilCell.add(
                Paragraph("ACEITE")
                    .setBold()
                    .setFontSize(12f)
                    .setFontColor(COLOR_BLUE)
                    .setBorderBottom(SolidBorder(COLOR_BLUE, 1f))
            )

            // Detalles del aceite
            oilCell.add(Paragraph("Marca: ${oilChangeData.oilBrand}").setFontSize(10f))
            oilCell.add(Paragraph("Tipo: ${oilChangeData.oilType}").setFontSize(10f))
            oilCell.add(Paragraph("Viscosidad: ${oilChangeData.oilViscosity}").setFontSize(10f))
            oilCell.add(Paragraph("Cantidad: ${oilChangeData.oilQuantity} L").setFontSize(10f))

            mainTable.addCell(oilCell)

            // Columna derecha: Filtros
            val filtersCell = Cell().setPadding(5f)

            // Título de filtros
            filtersCell.add(
                Paragraph("FILTROS")
                    .setBold()
                    .setFontSize(12f)
                    .setFontColor(COLOR_RED)
                    .setBorderBottom(SolidBorder(COLOR_RED, 1f))
            )

            // Filtro de aceite
            val oilFilterText = if (oilChangeData.oilFilterChanged) {
                if (oilChangeData.oilFilterBrand.isNotBlank()) {
                    "REEMPLAZO - ${oilChangeData.oilFilterBrand}" +
                            (if (oilChangeData.oilFilterNotes != "N/A") " (${oilChangeData.oilFilterNotes})" else "")
                } else {
                    "REEMPLAZO"
                }
            } else {
                "NO"
            }
            filtersCell.add(Paragraph("Aceite: $oilFilterText").setFontSize(10f))

            // Filtro de aire
            val airFilterText = if (oilChangeData.airFilterChanged) {
                "REEMPLAZO" + (if (oilChangeData.airFilterNotes != "N/A") " (${oilChangeData.airFilterNotes})" else "")
            } else {
                "NO"
            }
            filtersCell.add(Paragraph("Aire: $airFilterText").setFontSize(10f))

            // Filtro de habitáculo
            val cabinFilterText = if (oilChangeData.cabinFilterChanged) {
                "LIMPIEZA" + (if (oilChangeData.cabinFilterNotes != "N/A") " (${oilChangeData.cabinFilterNotes})" else "")
            } else {
                "NO"
            }
            filtersCell.add(Paragraph("Habitáculo: $cabinFilterText").setFontSize(10f))

            // Filtro de combustible
            val fuelFilterText = if (oilChangeData.fuelFilterChanged) {
                "REEMPLAZO" + (if (oilChangeData.fuelFilterNotes != "N/A") " (${oilChangeData.fuelFilterNotes})" else "")
            } else {
                "NO"
            }
            filtersCell.add(Paragraph("Combustible: $fuelFilterText").setFontSize(10f))

            mainTable.addCell(filtersCell)

            document.add(mainTable)
            document.add(Paragraph("").setMarginBottom(5f))
        }

        /**
         * Agrega la sección de extras de manera compacta
         */
        private fun addExtrasInfo(document: Document, oilChangeData: OilChangeData) {
            // Solo agregar sección si hay al menos un extra
            if (!hasExtras(oilChangeData)) {
                return
            }

            // Tabla principal con una columna
            val extrasTable = Table(1)
                .useAllAvailableWidth()
                .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(5f)

            // Título de extras
            val titleCell = Cell()
                .setBorder(Border.NO_BORDER)
                .add(
                    Paragraph("EXTRAS")
                        .setBold()
                        .setFontSize(12f)
                        .setFontColor(COLOR_GREEN)
                        .setBorderBottom(SolidBorder(COLOR_GREEN, 1f))
                )

            extrasTable.addCell(titleCell)

            // Tabla interna para extras con dos columnas
            val extrasInfoTable = Table(2)
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)

            // Refrigerante
            if (oilChangeData.coolantAdded) {
                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("Refrigerante:").setFontSize(10f))
                )

                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("RELLENADO" +
                                (if (oilChangeData.coolantNotes != "N/A") " (${oilChangeData.coolantNotes})" else ""))
                            .setFontSize(10f))
                )
            }

            // Engrase
            if (oilChangeData.greaseAdded) {
                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("Engrase:").setFontSize(10f))
                )

                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("COMPLETO" +
                                (if (oilChangeData.greaseNotes != "N/A") " (${oilChangeData.greaseNotes})" else ""))
                            .setFontSize(10f))
                )
            }

            // Aditivo
            if (oilChangeData.additiveAdded) {
                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("Aditivo:").setFontSize(10f))
                )

                val additiveText = if (oilChangeData.additiveType.isNotBlank()) {
                    "${oilChangeData.additiveType}" +
                            (if (oilChangeData.additiveNotes != "N/A") " (${oilChangeData.additiveNotes})" else "")
                } else {
                    "AGREGADO" + (if (oilChangeData.additiveNotes != "N/A") " (${oilChangeData.additiveNotes})" else "")
                }

                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph(additiveText).setFontSize(10f))
                )
            }

            // Caja
            if (oilChangeData.gearboxChecked) {
                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("Caja:").setFontSize(10f))
                )

                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("REVISADO" +
                                (if (oilChangeData.gearboxNotes != "N/A") " (${oilChangeData.gearboxNotes})" else ""))
                            .setFontSize(10f))
                )
            }

            // Diferencial
            if (oilChangeData.differentialChecked) {
                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("Diferencial:").setFontSize(10f))
                )

                extrasInfoTable.addCell(
                    Cell().setBorder(Border.NO_BORDER)
                        .add(Paragraph("REVISADO" +
                                (if (oilChangeData.differentialNotes != "N/A") " (${oilChangeData.differentialNotes})" else ""))
                            .setFontSize(10f))
                )
            }

            // Agregar tabla interna a la tabla principal
            extrasTable.addCell(
                Cell().setBorder(Border.NO_BORDER)
                    .add(extrasInfoTable)
            )

            document.add(extrasTable)
            document.add(Paragraph("").setMarginBottom(5f))
        }

        /**
         * Verifica si hay al menos un extra seleccionado
         */
        private fun hasExtras(oilChangeData: OilChangeData): Boolean {
            return oilChangeData.coolantAdded ||
                    oilChangeData.greaseAdded ||
                    oilChangeData.additiveAdded ||
                    oilChangeData.gearboxChecked ||
                    oilChangeData.differentialChecked
        }

        /**
         * Agrega la sección de observaciones
         */
        private fun addObservationsSection(document: Document, observations: String) {
            // Tabla principal con una columna
            val obsTable = Table(1)
                .useAllAvailableWidth()
                .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(5f)

            // Título de observaciones
            val titleCell = Cell()
                .setBorder(Border.NO_BORDER)
                .add(
                    Paragraph("OBSERVACIONES:")
                        .setBold()
                        .setFontSize(12f)
                        .setBorderBottom(SolidBorder(COLOR_BLACK, 0.5f))
                )

            obsTable.addCell(titleCell)

            // Contenido de observaciones
            val obsContentCell = Cell()
                .setBorder(Border.NO_BORDER)
                .add(Paragraph(observations).setFontSize(10f))

            obsTable.addCell(obsContentCell)

            document.add(obsTable)
        }

        /**
         * Agrega pie de página
         */
        private fun addFooter(document: Document) {
            document.add(Paragraph("").setMarginTop(10f))

            val footerParagraph = Paragraph("HISMA SERVICIOS - hisma.com.ar")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
                .setFontColor(ColorConstants.GRAY)

            document.add(footerParagraph)
        }

        /**
         * Intenta cargar una imagen de logo desde una URL
         */
        private fun getLubricenterLogoFromUrl(context: Context, logoUrl: String): Image? {
            // En una implementación real, aquí cargaríamos la imagen desde la URL
            // Para esta demo, simplemente usamos el logo de recursos
            return getLubricenterLogo(context)
        }

        /**
         * Obtiene el logo del lubricentro desde los recursos
         */
        private fun getLubricenterLogo(context: Context): Image? {
            try {
                // Para esta demo, usamos un logo de recursos
                // Intentar primero cargar el logo personalizado si existe
                val resourceId = context.resources.getIdentifier(
                    "ic_lubricenter_logo", "drawable", context.packageName
                )

                if (resourceId != 0) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                    return bitmapToImage(bitmap)
                } else {
                    // Si no existe, usar el icono de negocio como fallback
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_business)
                    return bitmapToImage(bitmap)
                }
            } catch (e: Exception) {
                Log.e("PdfGenerator", "Error al cargar logo del lubricentro", e)
                return null
            }
        }

        /**
         * Obtiene el logo de Hisma desde los recursos
         */
        private fun getHismaLogo(context: Context): Image? {
            try {
                // En una implementación real, este sería el logo oficial de Hisma
                // Intentar primero cargar el logo personalizado si existe
                val resourceId = context.resources.getIdentifier(
                    "logo_hisma", "drawable", context.packageName
                )

                if (resourceId != 0) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                    return bitmapToImage(bitmap)
                } else {
                    // Si no existe, usar el icono de negocio como fallback
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_business)
                    return bitmapToImage(bitmap)
                }
            } catch (e: Exception) {
                Log.e("PdfGenerator", "Error al cargar logo de Hisma", e)
                return null
            }
        }

        /**
         * Convierte un bitmap a una imagen de iText
         */
        private fun bitmapToImage(bitmap: Bitmap): Image {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = ImageDataFactory.create(stream.toByteArray())
            return Image(imageData)
        }
    }
}