package com.hisma.app.util

/**
 * Clase que proporciona datos para el autocompletado de campos en el registro de cambio de aceite
 */
object AutoCompleteDataProvider {

    // Marcas de aceites
    val OIL_BRANDS = listOf(
        "ACDelco", "Amsoil", "Bardahl", "Castrol", "Elf", "Fuchs", "Gulf", "Havoline",
        "Kendall", "Liqui Moly", "Mobil", "Motul", "Pennzoil", "Petronas", "Quaker State",
        "Ravenol", "Repsol", "Shell", "Sunoco", "Total", "Valvoline", "YPF"
    )

    // Tipos de aceite
    val OIL_TYPES = listOf(
        "MINERAL", "SEMI-SINTÉTICO", "SINTÉTICO"
    )

    // Viscosidades de aceite
    val OIL_VISCOSITIES = listOf(
        "SAE 20", "SAE 30", "SAE 40", "SAE 50", "SAE 60", "0W-20", "0W-30", "0W-40",
        "5W-20", "5W-30", "5W-40", "5W-50", "10W-30", "10W-40", "10W-60", "15W-40", "20W-50"
    )

    // Marcas de autos
    val CAR_BRANDS = listOf(
        "Acura", "Alfa Romeo", "Aston Martin", "Audi", "Bentley", "BMW", "Bugatti",
        "Cadillac", "Chevrolet", "Ferrari", "Fiat", "Ford", "Genesis", "Honda", "Hyundai",
        "Infiniti", "Jaguar", "Jeep", "Kia", "Land Rover", "Lexus", "Maserati", "Mazda",
        "Mercedes-Benz", "Mini", "Mitsubishi", "Nissan", "Peugeot", "Porsche", "Renault",
        "Rolls-Royce", "SEAT", "Skoda", "Subaru", "Suzuki", "Tesla", "Toyota", "Volkswagen", "Volvo"
    )

    // Marcas de motos
    val MOTORCYCLE_BRANDS = listOf(
        "AKT Motos", "Aprilia", "Bajaj", "Benelli", "Bimota", "BMW", "Carabela", "CFMoto",
        "Corven", "Ducati", "Gilera", "Guerrero", "Harley-Davidson", "Honda", "Husqvarna",
        "Indian", "Italika", "Izuka", "Jawa", "Kawasaki", "Keeway", "Keller", "KTM", "Kymco",
        "Mondial", "Motomel", "Moto Guzzi", "Motos Argentinas", "Okinoi", "Peugeot", "Piaggio",
        "Royal Enfield", "Suzuki", "Triumph", "TVS", "United Motors", "Ural", "Vento", "Vespa",
        "Voge", "Yamaha", "Zanella", "Zontes"
    )

    // Marcas de camiones
    val TRUCK_BRANDS = listOf(
        "Aeolus", "Agrale", "Alfa Romeo", "Aro", "Asia", "Bedford", "Belavtomaz", "Bobcat",
        "Caterpillar", "Chevrolet", "Citroën", "Daewoo", "Daf", "Daihatsu", "Deutz", "Deutz Agrale",
        "DFM", "Dimex", "Dina", "Dodge", "Elvetica", "F.E.R.E.S.A.", "Fiat", "Ford", "GAZ", "GMC",
        "Grosspal", "Heibao", "Hino", "Hyundai", "Internacional", "International", "Isuzu", "Iveco",
        "JAC", "Jinbei", "Kamaz", "Kenworth", "KIA", "Liaz", "Mack", "Man", "Mazda", "Mercedes-Benz",
        "Mitsubishi", "Nissan", "Opel", "Pauny", "Peterbilt", "Peugeot", "Pincen", "Plymouth",
        "Rastrojero", "Renault", "Renault Trucks", "Sanxing", "Scania", "Skoda", "SsangYong",
        "Tata", "Toyota", "Volare", "Volkswagen", "Volvo", "Yuejin"
    )

    // Todas las marcas de vehículos (para autocompletado común)
    val ALL_VEHICLE_BRANDS: List<String> by lazy {
        (CAR_BRANDS + MOTORCYCLE_BRANDS + TRUCK_BRANDS).distinct().sorted()
    }

    // Marcas de filtros
    val FILTER_BRANDS = listOf(
        "ACDelco", "Bosch", "Champion", "Fram", "K&N", "Mahle", "Mann", "Motorcraft",
        "Nippon Denso", "Purolator", "Ryco", "Sakura", "Toyota", "Valeo", "WIX"
    )

    // Notas comunes
    val COMMON_NOTES = listOf(
        "Revisado", "Sin novedad", "Cambiado", "Limpiado", "Rellenado", "Ajustado"
    )

    // Tipos de aditivos
    val ADDITIVE_TYPES = listOf(
        "Limpiador de inyectores", "Limpiador de sistema de combustible",
        "Elevador de octanaje", "Limpiador de válvulas", "Mejorador de viscosidad",
        "Antifricción", "Limpiador de cárter", "Tapafugas"
    )
}