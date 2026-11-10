package com.example.data.db

import com.example.data.model.Member

object SeedData {
    private val UAE_CITIES = setOf("DUBAI", "ABUDHABI", "AL AIN", "DUBAII")

    private fun capitalize(str: String): String {
        return str.lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun normalizeEmirate(city: String): String {
        return when (city.uppercase()) {
            "DUBAII" -> "Dubai"
            "DUBAI" -> "Dubai"
            "ABUDHABI" -> "Abu Dhabi"
            "AL AIN" -> "Al Ain"
            else -> capitalize(city)
        }
    }

    private fun parseLine(id: Long, line: String, currency: String): Member {
        val parts = line.split("|")
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val place = parts.getOrNull(1)?.trim().orEmpty()
        val mobile = parts.getOrNull(2)?.trim().orEmpty()
        val month = parts.getOrNull(3)?.trim().orEmpty()
        val amount = parts.getOrNull(4)?.trim()?.toDoubleOrNull() ?: 0.0

        val isUae = UAE_CITIES.contains(place.uppercase())
        val emirate = if (isUae) normalizeEmirate(place) else ""
        val country = if (isUae) "UAE" else capitalize(place)

        return Member(
            id = id,
            name = name,
            emirateOrCity = emirate,
            country = country,
            mobile = mobile,
            allottedMonth = month,
            amount = amount,
            currency = currency
        )
    }

    private val AED_RAW = """
Azeez KC|AL AIN|507607968|ALL|250
Mohammed Ali AT|ABUDHABI|559336123|JAN|300
Muujeeb KP|ABUDHABI|501527927|JAN|250
Asharaf Haji|AL AIN|506733580|FEB|250
Abdulla T|DUBAI|505714977|FEB|250
Sidheeq Karipayi|DUBAI||MAR|200
Hakeem KP|ABUDHABI|547385365|MAR|600
Kamarudheen Tp|DUBAI|556538992|MAR|400
Majeed AT|ABUDHABI|508786447|APR|250
Ashiq dhaher|AL AIN||APR|150
Jabir Kp|DUBAI|527931293|APR|250
Fasil PS|ABUDHABI|552299263|APR|250
Hashim KP|ABUDHABI|567877411|APR|250
Hashm KC|DUBAI|9745807373|APR|360
Muhammed Ali Kadampbuzha|ABUDHABI|554309595|MAY|200
Mahamood T|AL AIN|543799226|MAY|250
Ubaid KP|ABUDHABI|561701575|MAY|360
Mansoor KC|AL AIN|555207449|JUN|250
Latheef Pattenghal|ABUDHABI|9544630121|JUN|250
Sakeer|AL AIN|525576198|JUL|100
Munawer CP|AL AIN|545858244|JUL|150
Uwais TP|ABUDHABI|568981230|AUG|250
Basheer KC|DUBAI|555608729|AUG|300
Mohammed ali|SAUDI||AUG|150
Jubair KC|DUBAI||AUG|240
Ali Thurakkal|AL AIN|559559861|SEP|100
Irshad|AL AIN|528151979|OCT|150
Ashiq|AL AIN|506693510|OCT|250
Shafi|AL AIN|9526624294|NOV|250
Nazser KA|QATAR|97477790030|NOV|120
Jabir kc|DUBAI||NOV|250
Munaver TP|DUBAI|8891180922|NOV|250
Saffudheen KP|DUBAI|9947737350|DEC|240
Latheef Kalody|ABUDHABI|558782554|DEC|120
Mubaris TP|DUBAI|9633106322|DEC|120
Shareef Kalody|ABUDHABI|508257521|DEC|250
Sulthan KC|AL AIN||DEC|250
Haseeb KC|AL AIN||DEC|240
Ismayil Colonai|SAUDI||DEC|0
""".trimIndent()

    private val INR_RAW = """
Salman UK|UK|447435445716|JAN|2000
Gafoor Karipayi|DUBAI|563053274|FEB|3750
Safwan KP|ABUDHABI|9496890742|MAR|2500
Nourudheen Tp|DUBAI||MAR|6000
Shareef K|DUBAI|552447901|MAR|1200
Mujeeb Pathyil|DUBAI||MAR|5000
Ashraf KP|DUBAI||MAR|1000
Sakir saudi|SAUDI|919048222740|APR|2500
Yaser Saudi|SAUDI|8714203495|JUN|5000
Hunais Oman|OMAN|9847894240|JUN|5000
Shafeeq Oman|OMAN|96894379530|AUG|2000
Younus Qatar|QATAR|7510240203|SEP|2000
Suhail Qatar|QATAR|7510240203|SEP|2000
Khilar Thurakal|ABUDHABI|553396960|SEP|3000
Rashid Russia|RUSSIA|79125874112|OCT|2000
Shihab Qatar|QATAR|97430122454|DEC|5000
""".trimIndent()

    fun getInitialMembers(): List<Member> {
        val list = mutableListOf<Member>()
        var currentId = 1L

        AED_RAW.lines().filter { it.isNotBlank() }.forEach { line ->
            list.add(parseLine(currentId++, line, "AED"))
        }

        INR_RAW.lines().filter { it.isNotBlank() }.forEach { line ->
            list.add(parseLine(currentId++, line, "INR"))
        }

        return list
    }
}
