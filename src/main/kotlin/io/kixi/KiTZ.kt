package io.kixi

import io.kixi.text.ParseException
import java.time.ZoneOffset

/**
 * Ki Time Zone (KiTZ) - A human-readable timezone identifier combining a country code
 * with a standard timezone abbreviation.
 *
 * KiTZ provides a standardized set of timezone identifiers using the format
 * `CC/TZ` where `CC` is the ISO 3166-1 alpha-2 country code and `TZ` is the
 * standard timezone abbreviation for that region.
 *
 * ## Format
 * ```
 * 2024/3/15@14:30:00-US/PST   // Pacific Standard Time
 * 2024/3/15@14:30:00-JP/JST   // Japan Standard Time
 * 2024/3/15@14:30:00-DE/CET   // Central European Time
 * ```
 *
 * ## Usage
 * ```kotlin
 * val pst = KiTZ["US/PST"]           // Get by ID
 * val jst = KiTZ["JP/JST"]
 *
 * println(pst.offset)                // -08:00
 * println(pst.country)               // United States
 * println(pst.countryCode)           // US
 * println(pst.abbreviation)          // PST
 *
 * // Reverse lookup
 * val tz = KiTZ.fromOffset(ZoneOffset.ofHours(-8))  // US/PST
 * ```
 *
 * @property id The full KiTZ identifier (e.g., "US/PST", "JP/JST")
 * @property offset The UTC offset for this timezone
 * @property country The full country name (e.g., "United States", "Japan")
 * @see <a href="https://github.com/kixi-io/Ki.Docs/wiki/Ki-Time-Zone-Specification">Ki Time Zone Specification</a>
 */
data class KiTZ(
    val id: String,
    val offset: ZoneOffset,
    val country: String
) {
    /**
     * The ISO 3166-1 alpha-2 country code (e.g., "US", "JP", "DE").
     */
    val countryCode: String = id.substringBefore('/')

    /**
     * The timezone abbreviation (e.g., "PST", "JST", "CET").
     */
    val abbreviation: String = id.substringAfter('/')

    override fun toString(): String = id

    companion object : Parseable<KiTZ> {
        /**
         * Map of ISO 3166-1 alpha-2 country codes to full country names.
         */
        private val COUNTRY_NAMES = mapOf(
            "AD" to "Andorra",
            "AE" to "United Arab Emirates",
            "AF" to "Afghanistan",
            "AG" to "Antigua and Barbuda",
            "AL" to "Albania",
            "AM" to "Armenia",
            "AO" to "Angola",
            "AR" to "Argentina",
            "AT" to "Austria",
            "AU" to "Australia",
            "AZ" to "Azerbaijan",
            "BA" to "Bosnia and Herzegovina",
            "BB" to "Barbados",
            "BD" to "Bangladesh",
            "BE" to "Belgium",
            "BF" to "Burkina Faso",
            "BG" to "Bulgaria",
            "BH" to "Bahrain",
            "BI" to "Burundi",
            "BJ" to "Benin",
            "BN" to "Brunei",
            "BO" to "Bolivia",
            "BR" to "Brazil",
            "BS" to "Bahamas",
            "BT" to "Bhutan",
            "BW" to "Botswana",
            "BY" to "Belarus",
            "BZ" to "Belize",
            "CA" to "Canada",
            "CD" to "Democratic Republic of the Congo",
            "CF" to "Central African Republic",
            "CG" to "Congo",
            "CH" to "Switzerland",
            "CI" to "Côte d'Ivoire",
            "CL" to "Chile",
            "CM" to "Cameroon",
            "CN" to "China",
            "CO" to "Colombia",
            "CR" to "Costa Rica",
            "CU" to "Cuba",
            "CY" to "Cyprus",
            "CZ" to "Czech Republic",
            "DE" to "Germany",
            "DJ" to "Djibouti",
            "DK" to "Denmark",
            "DM" to "Dominica",
            "DO" to "Dominican Republic",
            "DZ" to "Algeria",
            "EC" to "Ecuador",
            "EE" to "Estonia",
            "EG" to "Egypt",
            "ER" to "Eritrea",
            "ES" to "Spain",
            "ET" to "Ethiopia",
            "FI" to "Finland",
            "FJ" to "Fiji",
            "FM" to "Micronesia",
            "FR" to "France",
            "GA" to "Gabon",
            "GB" to "United Kingdom",
            "GD" to "Grenada",
            "GE" to "Georgia",
            "GH" to "Ghana",
            "GM" to "Gambia",
            "GN" to "Guinea",
            "GQ" to "Equatorial Guinea",
            "GR" to "Greece",
            "GT" to "Guatemala",
            "GW" to "Guinea-Bissau",
            "GY" to "Guyana",
            "HN" to "Honduras",
            "HR" to "Croatia",
            "HT" to "Haiti",
            "HU" to "Hungary",
            "ID" to "Indonesia",
            "IE" to "Ireland",
            "IL" to "Israel",
            "IN" to "India",
            "IQ" to "Iraq",
            "IR" to "Iran",
            "IS" to "Iceland",
            "IT" to "Italy",
            "JM" to "Jamaica",
            "JO" to "Jordan",
            "JP" to "Japan",
            "KE" to "Kenya",
            "KG" to "Kyrgyzstan",
            "KH" to "Cambodia",
            "KI" to "Kiribati",
            "KM" to "Comoros",
            "KN" to "Saint Kitts and Nevis",
            "KP" to "North Korea",
            "KR" to "South Korea",
            "KW" to "Kuwait",
            "KZ" to "Kazakhstan",
            "LA" to "Laos",
            "LB" to "Lebanon",
            "LC" to "Saint Lucia",
            "LI" to "Liechtenstein",
            "LK" to "Sri Lanka",
            "LR" to "Liberia",
            "LS" to "Lesotho",
            "LT" to "Lithuania",
            "LU" to "Luxembourg",
            "LV" to "Latvia",
            "LY" to "Libya",
            "MA" to "Morocco",
            "MC" to "Monaco",
            "MD" to "Moldova",
            "ME" to "Montenegro",
            "MG" to "Madagascar",
            "MH" to "Marshall Islands",
            "MK" to "North Macedonia",
            "ML" to "Mali",
            "MM" to "Myanmar",
            "MN" to "Mongolia",
            "MR" to "Mauritania",
            "MT" to "Malta",
            "MU" to "Mauritius",
            "MV" to "Maldives",
            "MW" to "Malawi",
            "MX" to "Mexico",
            "MY" to "Malaysia",
            "MZ" to "Mozambique",
            "NA" to "Namibia",
            "NE" to "Niger",
            "NG" to "Nigeria",
            "NI" to "Nicaragua",
            "NL" to "Netherlands",
            "NO" to "Norway",
            "NP" to "Nepal",
            "NR" to "Nauru",
            "NZ" to "New Zealand",
            "OM" to "Oman",
            "PA" to "Panama",
            "PE" to "Peru",
            "PG" to "Papua New Guinea",
            "PH" to "Philippines",
            "PK" to "Pakistan",
            "PL" to "Poland",
            "PT" to "Portugal",
            "PY" to "Paraguay",
            "QA" to "Qatar",
            "RO" to "Romania",
            "RS" to "Serbia",
            "RU" to "Russia",
            "RW" to "Rwanda",
            "SA" to "Saudi Arabia",
            "SB" to "Solomon Islands",
            "SC" to "Seychelles",
            "SD" to "Sudan",
            "SE" to "Sweden",
            "SG" to "Singapore",
            "SI" to "Slovenia",
            "SK" to "Slovakia",
            "SL" to "Sierra Leone",
            "SM" to "San Marino",
            "SN" to "Senegal",
            "SO" to "Somalia",
            "SR" to "Suriname",
            "ST" to "São Tomé and Príncipe",
            "SV" to "El Salvador",
            "SY" to "Syria",
            "SZ" to "Eswatini",
            "TD" to "Chad",
            "TG" to "Togo",
            "TH" to "Thailand",
            "TJ" to "Tajikistan",
            "TL" to "Timor-Leste",
            "TM" to "Turkmenistan",
            "TN" to "Tunisia",
            "TO" to "Tonga",
            "TR" to "Turkey",
            "TT" to "Trinidad and Tobago",
            "TV" to "Tuvalu",
            "TZ" to "Tanzania",
            "UA" to "Ukraine",
            "UG" to "Uganda",
            "US" to "United States",
            "UY" to "Uruguay",
            "UZ" to "Uzbekistan",
            "VC" to "Saint Vincent and the Grenadines",
            "VE" to "Venezuela",
            "VN" to "Vietnam",
            "VU" to "Vanuatu",
            "WS" to "Samoa",
            "ZA" to "South Africa",
            "ZM" to "Zambia",
            "ZW" to "Zimbabwe"
        )

        /** Helper to create KiTZ with country lookup */
        private fun kitz(id: String, offset: ZoneOffset): KiTZ {
            val countryCode = id.substringBefore('/')
            val country = COUNTRY_NAMES[countryCode] ?: countryCode
            return KiTZ(id, offset, country)
        }

        // ============================================================
        // All KiTZ instances organized alphabetically by country code
        // ============================================================

        // AD - Andorra
        @JvmField val AD_CET = kitz("AD/CET", ZoneOffset.of("+1"))
        @JvmField val AD_CEST = kitz("AD/CEST", ZoneOffset.of("+2"))

        // AE - United Arab Emirates
        @JvmField val AE_GST = kitz("AE/GST", ZoneOffset.of("+4"))

        // AF - Afghanistan
        @JvmField val AF_AFT = kitz("AF/AFT", ZoneOffset.of("+04:30"))

        // AG - Antigua and Barbuda
        @JvmField val AG_AST = kitz("AG/AST", ZoneOffset.of("-4"))

        // AL - Albania
        @JvmField val AL_CET = kitz("AL/CET", ZoneOffset.of("+1"))
        @JvmField val AL_CEST = kitz("AL/CEST", ZoneOffset.of("+2"))

        // AM - Armenia
        @JvmField val AM_AMT = kitz("AM/AMT", ZoneOffset.of("+4"))

        // AO - Angola
        @JvmField val AO_WAT = kitz("AO/WAT", ZoneOffset.of("+1"))

        // AR - Argentina
        @JvmField val AR_ART = kitz("AR/ART", ZoneOffset.of("-3"))

        // AT - Austria
        @JvmField val AT_CET = kitz("AT/CET", ZoneOffset.of("+1"))
        @JvmField val AT_CEST = kitz("AT/CEST", ZoneOffset.of("+2"))

        // AU - Australia
        @JvmField val AU_ACST = kitz("AU/ACST", ZoneOffset.of("+09:30"))
        @JvmField val AU_ACDT = kitz("AU/ACDT", ZoneOffset.of("+10:30"))
        @JvmField val AU_AEST = kitz("AU/AEST", ZoneOffset.of("+10"))
        @JvmField val AU_AEDT = kitz("AU/AEDT", ZoneOffset.of("+11"))
        @JvmField val AU_AWST = kitz("AU/AWST", ZoneOffset.of("+8"))
        @JvmField val AU_NFT = kitz("AU/NFT", ZoneOffset.of("+11"))
        @JvmField val AU_NFDT = kitz("AU/NFDT", ZoneOffset.of("+12"))

        // AZ - Azerbaijan
        @JvmField val AZ_AZT = kitz("AZ/AZT", ZoneOffset.of("+4"))

        // BA - Bosnia and Herzegovina
        @JvmField val BA_CET = kitz("BA/CET", ZoneOffset.of("+1"))
        @JvmField val BA_CEST = kitz("BA/CEST", ZoneOffset.of("+2"))

        // BB - Barbados
        @JvmField val BB_AST = kitz("BB/AST", ZoneOffset.of("-4"))

        // BD - Bangladesh
        @JvmField val BD_BST = kitz("BD/BST", ZoneOffset.of("+6"))

        // BE - Belgium
        @JvmField val BE_CET = kitz("BE/CET", ZoneOffset.of("+1"))
        @JvmField val BE_CEST = kitz("BE/CEST", ZoneOffset.of("+2"))

        // BF - Burkina Faso
        @JvmField val BF_GMT = kitz("BF/GMT", ZoneOffset.UTC)

        // BG - Bulgaria
        @JvmField val BG_EET = kitz("BG/EET", ZoneOffset.of("+2"))
        @JvmField val BG_EEST = kitz("BG/EEST", ZoneOffset.of("+3"))

        // BH - Bahrain
        @JvmField val BH_AST = kitz("BH/AST", ZoneOffset.of("+3"))

        // BI - Burundi
        @JvmField val BI_CAT = kitz("BI/CAT", ZoneOffset.of("+2"))

        // BJ - Benin
        @JvmField val BJ_WAT = kitz("BJ/WAT", ZoneOffset.of("+1"))

        // BN - Brunei
        @JvmField val BN_BNT = kitz("BN/BNT", ZoneOffset.of("+8"))

        // BO - Bolivia
        @JvmField val BO_BOT = kitz("BO/BOT", ZoneOffset.of("-4"))

        // BR - Brazil
        @JvmField val BR_ACT = kitz("BR/ACT", ZoneOffset.of("-5"))
        @JvmField val BR_AMT = kitz("BR/AMT", ZoneOffset.of("-4"))
        @JvmField val BR_BRT = kitz("BR/BRT", ZoneOffset.of("-3"))
        @JvmField val BR_FNT = kitz("BR/FNT", ZoneOffset.of("-2"))

        // BS - Bahamas
        @JvmField val BS_EST = kitz("BS/EST", ZoneOffset.of("-5"))
        @JvmField val BS_EDT = kitz("BS/EDT", ZoneOffset.of("-4"))

        // BT - Bhutan
        @JvmField val BT_BTT = kitz("BT/BTT", ZoneOffset.of("+6"))

        // BW - Botswana
        @JvmField val BW_CAT = kitz("BW/CAT", ZoneOffset.of("+2"))

        // BY - Belarus
        @JvmField val BY_MSK = kitz("BY/MSK", ZoneOffset.of("+3"))

        // BZ - Belize
        @JvmField val BZ_CST = kitz("BZ/CST", ZoneOffset.of("-6"))

        // CA - Canada
        @JvmField val CA_AST = kitz("CA/AST", ZoneOffset.of("-4"))
        @JvmField val CA_ADT = kitz("CA/ADT", ZoneOffset.of("-3"))
        @JvmField val CA_CST = kitz("CA/CST", ZoneOffset.of("-6"))
        @JvmField val CA_CDT = kitz("CA/CDT", ZoneOffset.of("-5"))
        @JvmField val CA_EST = kitz("CA/EST", ZoneOffset.of("-5"))
        @JvmField val CA_EDT = kitz("CA/EDT", ZoneOffset.of("-4"))
        @JvmField val CA_MST = kitz("CA/MST", ZoneOffset.of("-7"))
        @JvmField val CA_MDT = kitz("CA/MDT", ZoneOffset.of("-6"))
        @JvmField val CA_NST = kitz("CA/NST", ZoneOffset.of("-03:30"))
        @JvmField val CA_NDT = kitz("CA/NDT", ZoneOffset.of("-02:30"))
        @JvmField val CA_PST = kitz("CA/PST", ZoneOffset.of("-8"))
        @JvmField val CA_PDT = kitz("CA/PDT", ZoneOffset.of("-7"))

        // CD - Democratic Republic of the Congo
        @JvmField val CD_WAT = kitz("CD/WAT", ZoneOffset.of("+1"))
        @JvmField val CD_CAT = kitz("CD/CAT", ZoneOffset.of("+2"))

        // CF - Central African Republic
        @JvmField val CF_WAT = kitz("CF/WAT", ZoneOffset.of("+1"))

        // CG - Congo
        @JvmField val CG_WAT = kitz("CG/WAT", ZoneOffset.of("+1"))

        // CH - Switzerland
        @JvmField val CH_CET = kitz("CH/CET", ZoneOffset.of("+1"))
        @JvmField val CH_CEST = kitz("CH/CEST", ZoneOffset.of("+2"))

        // CI - Côte d'Ivoire
        @JvmField val CI_GMT = kitz("CI/GMT", ZoneOffset.UTC)

        // CL - Chile
        @JvmField val CL_CLT = kitz("CL/CLT", ZoneOffset.of("-4"))
        @JvmField val CL_CLST = kitz("CL/CLST", ZoneOffset.of("-3"))

        // CM - Cameroon
        @JvmField val CM_WAT = kitz("CM/WAT", ZoneOffset.of("+1"))

        // CN - China
        @JvmField val CN_CST = kitz("CN/CST", ZoneOffset.of("+8"))

        // CO - Colombia
        @JvmField val CO_COT = kitz("CO/COT", ZoneOffset.of("-5"))

        // CR - Costa Rica
        @JvmField val CR_CST = kitz("CR/CST", ZoneOffset.of("-6"))

        // CU - Cuba
        @JvmField val CU_CST = kitz("CU/CST", ZoneOffset.of("-5"))
        @JvmField val CU_CDT = kitz("CU/CDT", ZoneOffset.of("-4"))

        // CY - Cyprus
        @JvmField val CY_EET = kitz("CY/EET", ZoneOffset.of("+2"))
        @JvmField val CY_EEST = kitz("CY/EEST", ZoneOffset.of("+3"))

        // CZ - Czech Republic
        @JvmField val CZ_CET = kitz("CZ/CET", ZoneOffset.of("+1"))
        @JvmField val CZ_CEST = kitz("CZ/CEST", ZoneOffset.of("+2"))

        // DE - Germany
        @JvmField val DE_CET = kitz("DE/CET", ZoneOffset.of("+1"))
        @JvmField val DE_CEST = kitz("DE/CEST", ZoneOffset.of("+2"))

        // DJ - Djibouti
        @JvmField val DJ_EAT = kitz("DJ/EAT", ZoneOffset.of("+3"))

        // DK - Denmark
        @JvmField val DK_CET = kitz("DK/CET", ZoneOffset.of("+1"))
        @JvmField val DK_CEST = kitz("DK/CEST", ZoneOffset.of("+2"))

        // DM - Dominica
        @JvmField val DM_AST = kitz("DM/AST", ZoneOffset.of("-4"))

        // DO - Dominican Republic
        @JvmField val DO_AST = kitz("DO/AST", ZoneOffset.of("-4"))

        // DZ - Algeria
        @JvmField val DZ_CET = kitz("DZ/CET", ZoneOffset.of("+1"))

        // EC - Ecuador
        @JvmField val EC_ECT = kitz("EC/ECT", ZoneOffset.of("-5"))

        // EE - Estonia
        @JvmField val EE_EET = kitz("EE/EET", ZoneOffset.of("+2"))
        @JvmField val EE_EEST = kitz("EE/EEST", ZoneOffset.of("+3"))

        // EG - Egypt
        @JvmField val EG_EET = kitz("EG/EET", ZoneOffset.of("+2"))

        // ER - Eritrea
        @JvmField val ER_EAT = kitz("ER/EAT", ZoneOffset.of("+3"))

        // ES - Spain
        @JvmField val ES_CET = kitz("ES/CET", ZoneOffset.of("+1"))
        @JvmField val ES_CEST = kitz("ES/CEST", ZoneOffset.of("+2"))

        // ET - Ethiopia
        @JvmField val ET_EAT = kitz("ET/EAT", ZoneOffset.of("+3"))

        // FI - Finland
        @JvmField val FI_EET = kitz("FI/EET", ZoneOffset.of("+2"))
        @JvmField val FI_EEST = kitz("FI/EEST", ZoneOffset.of("+3"))

        // FJ - Fiji
        @JvmField val FJ_FJT = kitz("FJ/FJT", ZoneOffset.of("+12"))
        @JvmField val FJ_FJST = kitz("FJ/FJST", ZoneOffset.of("+13"))

        // FM - Micronesia
        @JvmField val FM_CHUT = kitz("FM/CHUT", ZoneOffset.of("+10"))
        @JvmField val FM_PONT = kitz("FM/PONT", ZoneOffset.of("+11"))

        // FR - France
        @JvmField val FR_CET = kitz("FR/CET", ZoneOffset.of("+1"))
        @JvmField val FR_CEST = kitz("FR/CEST", ZoneOffset.of("+2"))

        // GA - Gabon
        @JvmField val GA_WAT = kitz("GA/WAT", ZoneOffset.of("+1"))

        // GB - United Kingdom
        @JvmField val GB_GMT = kitz("GB/GMT", ZoneOffset.UTC)
        @JvmField val GB_BST = kitz("GB/BST", ZoneOffset.of("+1"))

        // GD - Grenada
        @JvmField val GD_AST = kitz("GD/AST", ZoneOffset.of("-4"))

        // GE - Georgia
        @JvmField val GE_GET = kitz("GE/GET", ZoneOffset.of("+4"))

        // GH - Ghana
        @JvmField val GH_GMT = kitz("GH/GMT", ZoneOffset.UTC)

        // GM - Gambia
        @JvmField val GM_GMT = kitz("GM/GMT", ZoneOffset.UTC)

        // GN - Guinea
        @JvmField val GN_GMT = kitz("GN/GMT", ZoneOffset.UTC)

        // GQ - Equatorial Guinea
        @JvmField val GQ_WAT = kitz("GQ/WAT", ZoneOffset.of("+1"))

        // GR - Greece
        @JvmField val GR_EET = kitz("GR/EET", ZoneOffset.of("+2"))
        @JvmField val GR_EEST = kitz("GR/EEST", ZoneOffset.of("+3"))

        // GT - Guatemala
        @JvmField val GT_CST = kitz("GT/CST", ZoneOffset.of("-6"))

        // GW - Guinea-Bissau
        @JvmField val GW_GMT = kitz("GW/GMT", ZoneOffset.UTC)

        // GY - Guyana
        @JvmField val GY_GYT = kitz("GY/GYT", ZoneOffset.of("-4"))

        // HN - Honduras
        @JvmField val HN_CST = kitz("HN/CST", ZoneOffset.of("-6"))

        // HR - Croatia
        @JvmField val HR_CET = kitz("HR/CET", ZoneOffset.of("+1"))
        @JvmField val HR_CEST = kitz("HR/CEST", ZoneOffset.of("+2"))

        // HT - Haiti
        @JvmField val HT_EST = kitz("HT/EST", ZoneOffset.of("-5"))
        @JvmField val HT_EDT = kitz("HT/EDT", ZoneOffset.of("-4"))

        // HU - Hungary
        @JvmField val HU_CET = kitz("HU/CET", ZoneOffset.of("+1"))
        @JvmField val HU_CEST = kitz("HU/CEST", ZoneOffset.of("+2"))

        // ID - Indonesia
        @JvmField val ID_WIB = kitz("ID/WIB", ZoneOffset.of("+7"))
        @JvmField val ID_WITA = kitz("ID/WITA", ZoneOffset.of("+8"))
        @JvmField val ID_WIT = kitz("ID/WIT", ZoneOffset.of("+9"))

        // IE - Ireland
        @JvmField val IE_GMT = kitz("IE/GMT", ZoneOffset.UTC)
        @JvmField val IE_IST = kitz("IE/IST", ZoneOffset.of("+1"))

        // IL - Israel
        @JvmField val IL_IST = kitz("IL/IST", ZoneOffset.of("+2"))
        @JvmField val IL_IDT = kitz("IL/IDT", ZoneOffset.of("+3"))

        // IN - India
        @JvmField val IN_IST = kitz("IN/IST", ZoneOffset.of("+05:30"))

        // IQ - Iraq
        @JvmField val IQ_AST = kitz("IQ/AST", ZoneOffset.of("+3"))

        // IR - Iran
        @JvmField val IR_IRST = kitz("IR/IRST", ZoneOffset.of("+03:30"))
        @JvmField val IR_IRDT = kitz("IR/IRDT", ZoneOffset.of("+04:30"))

        // IS - Iceland
        @JvmField val IS_GMT = kitz("IS/GMT", ZoneOffset.UTC)

        // IT - Italy
        @JvmField val IT_CET = kitz("IT/CET", ZoneOffset.of("+1"))
        @JvmField val IT_CEST = kitz("IT/CEST", ZoneOffset.of("+2"))

        // JM - Jamaica
        @JvmField val JM_EST = kitz("JM/EST", ZoneOffset.of("-5"))

        // JO - Jordan
        @JvmField val JO_EET = kitz("JO/EET", ZoneOffset.of("+2"))
        @JvmField val JO_EEST = kitz("JO/EEST", ZoneOffset.of("+3"))

        // JP - Japan
        @JvmField val JP_JST = kitz("JP/JST", ZoneOffset.of("+9"))

        // KE - Kenya
        @JvmField val KE_EAT = kitz("KE/EAT", ZoneOffset.of("+3"))

        // KG - Kyrgyzstan
        @JvmField val KG_KGT = kitz("KG/KGT", ZoneOffset.of("+6"))

        // KH - Cambodia
        @JvmField val KH_ICT = kitz("KH/ICT", ZoneOffset.of("+7"))

        // KI - Kiribati
        @JvmField val KI_GILT = kitz("KI/GILT", ZoneOffset.of("+12"))
        @JvmField val KI_PHOT = kitz("KI/PHOT", ZoneOffset.of("+13"))
        @JvmField val KI_LINT = kitz("KI/LINT", ZoneOffset.of("+14"))

        // KM - Comoros
        @JvmField val KM_EAT = kitz("KM/EAT", ZoneOffset.of("+3"))

        // KN - Saint Kitts and Nevis
        @JvmField val KN_AST = kitz("KN/AST", ZoneOffset.of("-4"))

        // KP - North Korea
        @JvmField val KP_KST = kitz("KP/KST", ZoneOffset.of("+9"))

        // KR - South Korea
        @JvmField val KR_KST = kitz("KR/KST", ZoneOffset.of("+9"))

        // KW - Kuwait
        @JvmField val KW_AST = kitz("KW/AST", ZoneOffset.of("+3"))

        // KZ - Kazakhstan
        @JvmField val KZ_AQTT = kitz("KZ/AQTT", ZoneOffset.of("+5"))
        @JvmField val KZ_ALMT = kitz("KZ/ALMT", ZoneOffset.of("+6"))

        // LA - Laos
        @JvmField val LA_ICT = kitz("LA/ICT", ZoneOffset.of("+7"))

        // LB - Lebanon
        @JvmField val LB_EET = kitz("LB/EET", ZoneOffset.of("+2"))
        @JvmField val LB_EEST = kitz("LB/EEST", ZoneOffset.of("+3"))

        // LC - Saint Lucia
        @JvmField val LC_AST = kitz("LC/AST", ZoneOffset.of("-4"))

        // LI - Liechtenstein
        @JvmField val LI_CET = kitz("LI/CET", ZoneOffset.of("+1"))
        @JvmField val LI_CEST = kitz("LI/CEST", ZoneOffset.of("+2"))

        // LK - Sri Lanka
        @JvmField val LK_IST = kitz("LK/IST", ZoneOffset.of("+05:30"))

        // LR - Liberia
        @JvmField val LR_GMT = kitz("LR/GMT", ZoneOffset.UTC)

        // LS - Lesotho
        @JvmField val LS_SAST = kitz("LS/SAST", ZoneOffset.of("+2"))

        // LT - Lithuania
        @JvmField val LT_EET = kitz("LT/EET", ZoneOffset.of("+2"))
        @JvmField val LT_EEST = kitz("LT/EEST", ZoneOffset.of("+3"))

        // LU - Luxembourg
        @JvmField val LU_CET = kitz("LU/CET", ZoneOffset.of("+1"))
        @JvmField val LU_CEST = kitz("LU/CEST", ZoneOffset.of("+2"))

        // LV - Latvia
        @JvmField val LV_EET = kitz("LV/EET", ZoneOffset.of("+2"))
        @JvmField val LV_EEST = kitz("LV/EEST", ZoneOffset.of("+3"))

        // LY - Libya
        @JvmField val LY_EET = kitz("LY/EET", ZoneOffset.of("+2"))

        // MA - Morocco
        @JvmField val MA_WET = kitz("MA/WET", ZoneOffset.UTC)
        @JvmField val MA_WEST = kitz("MA/WEST", ZoneOffset.of("+1"))

        // MC - Monaco
        @JvmField val MC_CET = kitz("MC/CET", ZoneOffset.of("+1"))
        @JvmField val MC_CEST = kitz("MC/CEST", ZoneOffset.of("+2"))

        // ME - Montenegro
        @JvmField val ME_CET = kitz("ME/CET", ZoneOffset.of("+1"))
        @JvmField val ME_CEST = kitz("ME/CEST", ZoneOffset.of("+2"))

        // MG - Madagascar
        @JvmField val MG_EAT = kitz("MG/EAT", ZoneOffset.of("+3"))

        // MH - Marshall Islands
        @JvmField val MH_MHT = kitz("MH/MHT", ZoneOffset.of("+12"))

        // MK - North Macedonia
        @JvmField val MK_CET = kitz("MK/CET", ZoneOffset.of("+1"))
        @JvmField val MK_CEST = kitz("MK/CEST", ZoneOffset.of("+2"))

        // ML - Mali
        @JvmField val ML_GMT = kitz("ML/GMT", ZoneOffset.UTC)

        // MM - Myanmar
        @JvmField val MM_MMT = kitz("MM/MMT", ZoneOffset.of("+06:30"))

        // MN - Mongolia
        @JvmField val MN_HOVT = kitz("MN/HOVT", ZoneOffset.of("+7"))
        @JvmField val MN_ULAT = kitz("MN/ULAT", ZoneOffset.of("+8"))

        // MR - Mauritania
        @JvmField val MR_GMT = kitz("MR/GMT", ZoneOffset.UTC)

        // MT - Malta
        @JvmField val MT_CET = kitz("MT/CET", ZoneOffset.of("+1"))
        @JvmField val MT_CEST = kitz("MT/CEST", ZoneOffset.of("+2"))

        // MU - Mauritius
        @JvmField val MU_MUT = kitz("MU/MUT", ZoneOffset.of("+4"))

        // MV - Maldives
        @JvmField val MV_MVT = kitz("MV/MVT", ZoneOffset.of("+5"))

        // MW - Malawi
        @JvmField val MW_CAT = kitz("MW/CAT", ZoneOffset.of("+2"))

        // MX - Mexico
        @JvmField val MX_CST = kitz("MX/CST", ZoneOffset.of("-6"))
        @JvmField val MX_CDT = kitz("MX/CDT", ZoneOffset.of("-5"))
        @JvmField val MX_EST = kitz("MX/EST", ZoneOffset.of("-5"))
        @JvmField val MX_MST = kitz("MX/MST", ZoneOffset.of("-7"))
        @JvmField val MX_MDT = kitz("MX/MDT", ZoneOffset.of("-6"))
        @JvmField val MX_PST = kitz("MX/PST", ZoneOffset.of("-8"))
        @JvmField val MX_PDT = kitz("MX/PDT", ZoneOffset.of("-7"))

        // MY - Malaysia
        @JvmField val MY_MYT = kitz("MY/MYT", ZoneOffset.of("+8"))

        // MZ - Mozambique
        @JvmField val MZ_CAT = kitz("MZ/CAT", ZoneOffset.of("+2"))

        // NA - Namibia
        @JvmField val NA_CAT = kitz("NA/CAT", ZoneOffset.of("+2"))

        // NE - Niger
        @JvmField val NE_WAT = kitz("NE/WAT", ZoneOffset.of("+1"))

        // NG - Nigeria
        @JvmField val NG_WAT = kitz("NG/WAT", ZoneOffset.of("+1"))

        // NI - Nicaragua
        @JvmField val NI_CST = kitz("NI/CST", ZoneOffset.of("-6"))

        // NL - Netherlands
        @JvmField val NL_CET = kitz("NL/CET", ZoneOffset.of("+1"))
        @JvmField val NL_CEST = kitz("NL/CEST", ZoneOffset.of("+2"))
        @JvmField val NL_AST = kitz("NL/AST", ZoneOffset.of("-4"))

        // NO - Norway
        @JvmField val NO_CET = kitz("NO/CET", ZoneOffset.of("+1"))
        @JvmField val NO_CEST = kitz("NO/CEST", ZoneOffset.of("+2"))

        // NP - Nepal
        @JvmField val NP_NPT = kitz("NP/NPT", ZoneOffset.of("+05:45"))

        // NR - Nauru
        @JvmField val NR_NRT = kitz("NR/NRT", ZoneOffset.of("+12"))

        // NZ - New Zealand
        @JvmField val NZ_NZST = kitz("NZ/NZST", ZoneOffset.of("+12"))
        @JvmField val NZ_NZDT = kitz("NZ/NZDT", ZoneOffset.of("+13"))

        // OM - Oman
        @JvmField val OM_GST = kitz("OM/GST", ZoneOffset.of("+4"))

        // PA - Panama
        @JvmField val PA_EST = kitz("PA/EST", ZoneOffset.of("-5"))

        // PE - Peru
        @JvmField val PE_PET = kitz("PE/PET", ZoneOffset.of("-5"))

        // PG - Papua New Guinea
        @JvmField val PG_PGT = kitz("PG/PGT", ZoneOffset.of("+10"))

        // PH - Philippines
        @JvmField val PH_PHT = kitz("PH/PHT", ZoneOffset.of("+8"))

        // PK - Pakistan
        @JvmField val PK_PKT = kitz("PK/PKT", ZoneOffset.of("+5"))

        // PL - Poland
        @JvmField val PL_CET = kitz("PL/CET", ZoneOffset.of("+1"))
        @JvmField val PL_CEST = kitz("PL/CEST", ZoneOffset.of("+2"))

        // PT - Portugal
        @JvmField val PT_WET = kitz("PT/WET", ZoneOffset.UTC)
        @JvmField val PT_WEST = kitz("PT/WEST", ZoneOffset.of("+1"))

        // PY - Paraguay
        @JvmField val PY_PYT = kitz("PY/PYT", ZoneOffset.of("-4"))
        @JvmField val PY_PYST = kitz("PY/PYST", ZoneOffset.of("-3"))

        // QA - Qatar
        @JvmField val QA_AST = kitz("QA/AST", ZoneOffset.of("+3"))

        // RO - Romania
        @JvmField val RO_EET = kitz("RO/EET", ZoneOffset.of("+2"))
        @JvmField val RO_EEST = kitz("RO/EEST", ZoneOffset.of("+3"))

        // RS - Serbia
        @JvmField val RS_CET = kitz("RS/CET", ZoneOffset.of("+1"))
        @JvmField val RS_CEST = kitz("RS/CEST", ZoneOffset.of("+2"))

        // RU - Russia
        @JvmField val RU_KALT = kitz("RU/KALT", ZoneOffset.of("+2"))
        @JvmField val RU_MSK = kitz("RU/MSK", ZoneOffset.of("+3"))
        @JvmField val RU_SAMT = kitz("RU/SAMT", ZoneOffset.of("+4"))
        @JvmField val RU_YEKT = kitz("RU/YEKT", ZoneOffset.of("+5"))
        @JvmField val RU_OMST = kitz("RU/OMST", ZoneOffset.of("+6"))
        @JvmField val RU_KRAT = kitz("RU/KRAT", ZoneOffset.of("+7"))
        @JvmField val RU_IRKT = kitz("RU/IRKT", ZoneOffset.of("+8"))
        @JvmField val RU_YAKT = kitz("RU/YAKT", ZoneOffset.of("+9"))
        @JvmField val RU_VLAT = kitz("RU/VLAT", ZoneOffset.of("+10"))
        @JvmField val RU_MAGT = kitz("RU/MAGT", ZoneOffset.of("+11"))
        @JvmField val RU_PETT = kitz("RU/PETT", ZoneOffset.of("+12"))

        // RW - Rwanda
        @JvmField val RW_CAT = kitz("RW/CAT", ZoneOffset.of("+2"))

        // SA - Saudi Arabia
        @JvmField val SA_AST = kitz("SA/AST", ZoneOffset.of("+3"))

        // SB - Solomon Islands
        @JvmField val SB_SBT = kitz("SB/SBT", ZoneOffset.of("+11"))

        // SC - Seychelles
        @JvmField val SC_SCT = kitz("SC/SCT", ZoneOffset.of("+4"))

        // SD - Sudan
        @JvmField val SD_CAT = kitz("SD/CAT", ZoneOffset.of("+2"))

        // SE - Sweden
        @JvmField val SE_CET = kitz("SE/CET", ZoneOffset.of("+1"))
        @JvmField val SE_CEST = kitz("SE/CEST", ZoneOffset.of("+2"))

        // SG - Singapore
        @JvmField val SG_SGT = kitz("SG/SGT", ZoneOffset.of("+8"))

        // SI - Slovenia
        @JvmField val SI_CET = kitz("SI/CET", ZoneOffset.of("+1"))
        @JvmField val SI_CEST = kitz("SI/CEST", ZoneOffset.of("+2"))

        // SK - Slovakia
        @JvmField val SK_CET = kitz("SK/CET", ZoneOffset.of("+1"))
        @JvmField val SK_CEST = kitz("SK/CEST", ZoneOffset.of("+2"))

        // SL - Sierra Leone
        @JvmField val SL_GMT = kitz("SL/GMT", ZoneOffset.UTC)

        // SM - San Marino
        @JvmField val SM_CET = kitz("SM/CET", ZoneOffset.of("+1"))
        @JvmField val SM_CEST = kitz("SM/CEST", ZoneOffset.of("+2"))

        // SN - Senegal
        @JvmField val SN_GMT = kitz("SN/GMT", ZoneOffset.UTC)

        // SO - Somalia
        @JvmField val SO_EAT = kitz("SO/EAT", ZoneOffset.of("+3"))

        // SR - Suriname
        @JvmField val SR_SRT = kitz("SR/SRT", ZoneOffset.of("-3"))

        // ST - São Tomé and Príncipe
        @JvmField val ST_GMT = kitz("ST/GMT", ZoneOffset.UTC)

        // SV - El Salvador
        @JvmField val SV_CST = kitz("SV/CST", ZoneOffset.of("-6"))

        // SY - Syria
        @JvmField val SY_EET = kitz("SY/EET", ZoneOffset.of("+2"))
        @JvmField val SY_EEST = kitz("SY/EEST", ZoneOffset.of("+3"))

        // SZ - Eswatini
        @JvmField val SZ_SAST = kitz("SZ/SAST", ZoneOffset.of("+2"))

        // TD - Chad
        @JvmField val TD_WAT = kitz("TD/WAT", ZoneOffset.of("+1"))

        // TG - Togo
        @JvmField val TG_GMT = kitz("TG/GMT", ZoneOffset.UTC)

        // TH - Thailand
        @JvmField val TH_ICT = kitz("TH/ICT", ZoneOffset.of("+7"))

        // TJ - Tajikistan
        @JvmField val TJ_TJT = kitz("TJ/TJT", ZoneOffset.of("+5"))

        // TL - Timor-Leste
        @JvmField val TL_TLT = kitz("TL/TLT", ZoneOffset.of("+9"))

        // TM - Turkmenistan
        @JvmField val TM_TMT = kitz("TM/TMT", ZoneOffset.of("+5"))

        // TN - Tunisia
        @JvmField val TN_CET = kitz("TN/CET", ZoneOffset.of("+1"))

        // TO - Tonga
        @JvmField val TO_TOT = kitz("TO/TOT", ZoneOffset.of("+13"))

        // TR - Turkey
        @JvmField val TR_TRT = kitz("TR/TRT", ZoneOffset.of("+3"))

        // TT - Trinidad and Tobago
        @JvmField val TT_AST = kitz("TT/AST", ZoneOffset.of("-4"))

        // TV - Tuvalu
        @JvmField val TV_TVT = kitz("TV/TVT", ZoneOffset.of("+12"))

        // TZ - Tanzania
        @JvmField val TZ_EAT = kitz("TZ/EAT", ZoneOffset.of("+3"))

        // UA - Ukraine
        @JvmField val UA_EET = kitz("UA/EET", ZoneOffset.of("+2"))
        @JvmField val UA_EEST = kitz("UA/EEST", ZoneOffset.of("+3"))

        // UG - Uganda
        @JvmField val UG_EAT = kitz("UG/EAT", ZoneOffset.of("+3"))

        // US - United States
        @JvmField val US_AST = kitz("US/AST", ZoneOffset.of("-4"))
        @JvmField val US_AKST = kitz("US/AKST", ZoneOffset.of("-9"))
        @JvmField val US_AKDT = kitz("US/AKDT", ZoneOffset.of("-8"))
        @JvmField val US_CHST = kitz("US/CHST", ZoneOffset.of("+10"))
        @JvmField val US_CST = kitz("US/CST", ZoneOffset.of("-6"))
        @JvmField val US_CDT = kitz("US/CDT", ZoneOffset.of("-5"))
        @JvmField val US_EST = kitz("US/EST", ZoneOffset.of("-5"))
        @JvmField val US_EDT = kitz("US/EDT", ZoneOffset.of("-4"))
        @JvmField val US_HST = kitz("US/HST", ZoneOffset.of("-10"))
        @JvmField val US_MST = kitz("US/MST", ZoneOffset.of("-7"))
        @JvmField val US_MDT = kitz("US/MDT", ZoneOffset.of("-6"))
        @JvmField val US_PST = kitz("US/PST", ZoneOffset.of("-8"))
        @JvmField val US_PDT = kitz("US/PDT", ZoneOffset.of("-7"))
        @JvmField val US_SST = kitz("US/SST", ZoneOffset.of("-11"))

        // UY - Uruguay
        @JvmField val UY_UYT = kitz("UY/UYT", ZoneOffset.of("-3"))

        // UZ - Uzbekistan
        @JvmField val UZ_UZT = kitz("UZ/UZT", ZoneOffset.of("+5"))

        // VC - Saint Vincent and the Grenadines
        @JvmField val VC_AST = kitz("VC/AST", ZoneOffset.of("-4"))

        // VE - Venezuela
        @JvmField val VE_VET = kitz("VE/VET", ZoneOffset.of("-4"))

        // VN - Vietnam
        @JvmField val VN_ICT = kitz("VN/ICT", ZoneOffset.of("+7"))

        // VU - Vanuatu
        @JvmField val VU_VUT = kitz("VU/VUT", ZoneOffset.of("+11"))

        // WS - Samoa
        @JvmField val WS_WST = kitz("WS/WST", ZoneOffset.of("+13"))

        // ZA - South Africa
        @JvmField val ZA_SAST = kitz("ZA/SAST", ZoneOffset.of("+2"))

        // ZM - Zambia
        @JvmField val ZM_CAT = kitz("ZM/CAT", ZoneOffset.of("+2"))

        // ZW - Zimbabwe
        @JvmField val ZW_CAT = kitz("ZW/CAT", ZoneOffset.of("+2"))

        /**
         * Special UTC timezone (not country-specific).
         */
        @JvmField val UTC = KiTZ("UTC", ZoneOffset.UTC, "Coordinated Universal Time")

        /**
         * Map of KiTZ identifiers to KiTZ instances.
         */
        private val BY_ID: Map<String, KiTZ> = listOf(
            // AD
            AD_CET, AD_CEST,
            // AE
            AE_GST,
            // AF
            AF_AFT,
            // AG
            AG_AST,
            // AL
            AL_CET, AL_CEST,
            // AM
            AM_AMT,
            // AO
            AO_WAT,
            // AR
            AR_ART,
            // AT
            AT_CET, AT_CEST,
            // AU
            AU_ACST, AU_ACDT, AU_AEST, AU_AEDT, AU_AWST, AU_NFT, AU_NFDT,
            // AZ
            AZ_AZT,
            // BA
            BA_CET, BA_CEST,
            // BB
            BB_AST,
            // BD
            BD_BST,
            // BE
            BE_CET, BE_CEST,
            // BF
            BF_GMT,
            // BG
            BG_EET, BG_EEST,
            // BH
            BH_AST,
            // BI
            BI_CAT,
            // BJ
            BJ_WAT,
            // BN
            BN_BNT,
            // BO
            BO_BOT,
            // BR
            BR_ACT, BR_AMT, BR_BRT, BR_FNT,
            // BS
            BS_EST, BS_EDT,
            // BT
            BT_BTT,
            // BW
            BW_CAT,
            // BY
            BY_MSK,
            // BZ
            BZ_CST,
            // CA
            CA_AST, CA_ADT, CA_CST, CA_CDT, CA_EST, CA_EDT, CA_MST, CA_MDT,
            CA_NST, CA_NDT, CA_PST, CA_PDT,
            // CD
            CD_WAT, CD_CAT,
            // CF
            CF_WAT,
            // CG
            CG_WAT,
            // CH
            CH_CET, CH_CEST,
            // CI
            CI_GMT,
            // CL
            CL_CLT, CL_CLST,
            // CM
            CM_WAT,
            // CN
            CN_CST,
            // CO
            CO_COT,
            // CR
            CR_CST,
            // CU
            CU_CST, CU_CDT,
            // CY
            CY_EET, CY_EEST,
            // CZ
            CZ_CET, CZ_CEST,
            // DE
            DE_CET, DE_CEST,
            // DJ
            DJ_EAT,
            // DK
            DK_CET, DK_CEST,
            // DM
            DM_AST,
            // DO
            DO_AST,
            // DZ
            DZ_CET,
            // EC
            EC_ECT,
            // EE
            EE_EET, EE_EEST,
            // EG
            EG_EET,
            // ER
            ER_EAT,
            // ES
            ES_CET, ES_CEST,
            // ET
            ET_EAT,
            // FI
            FI_EET, FI_EEST,
            // FJ
            FJ_FJT, FJ_FJST,
            // FM
            FM_CHUT, FM_PONT,
            // FR
            FR_CET, FR_CEST,
            // GA
            GA_WAT,
            // GB
            GB_GMT, GB_BST,
            // GD
            GD_AST,
            // GE
            GE_GET,
            // GH
            GH_GMT,
            // GM
            GM_GMT,
            // GN
            GN_GMT,
            // GQ
            GQ_WAT,
            // GR
            GR_EET, GR_EEST,
            // GT
            GT_CST,
            // GW
            GW_GMT,
            // GY
            GY_GYT,
            // HN
            HN_CST,
            // HR
            HR_CET, HR_CEST,
            // HT
            HT_EST, HT_EDT,
            // HU
            HU_CET, HU_CEST,
            // ID
            ID_WIB, ID_WITA, ID_WIT,
            // IE
            IE_GMT, IE_IST,
            // IL
            IL_IST, IL_IDT,
            // IN
            IN_IST,
            // IQ
            IQ_AST,
            // IR
            IR_IRST, IR_IRDT,
            // IS
            IS_GMT,
            // IT
            IT_CET, IT_CEST,
            // JM
            JM_EST,
            // JO
            JO_EET, JO_EEST,
            // JP
            JP_JST,
            // KE
            KE_EAT,
            // KG
            KG_KGT,
            // KH
            KH_ICT,
            // KI
            KI_GILT, KI_PHOT, KI_LINT,
            // KM
            KM_EAT,
            // KN
            KN_AST,
            // KP
            KP_KST,
            // KR
            KR_KST,
            // KW
            KW_AST,
            // KZ
            KZ_AQTT, KZ_ALMT,
            // LA
            LA_ICT,
            // LB
            LB_EET, LB_EEST,
            // LC
            LC_AST,
            // LI
            LI_CET, LI_CEST,
            // LK
            LK_IST,
            // LR
            LR_GMT,
            // LS
            LS_SAST,
            // LT
            LT_EET, LT_EEST,
            // LU
            LU_CET, LU_CEST,
            // LV
            LV_EET, LV_EEST,
            // LY
            LY_EET,
            // MA
            MA_WET, MA_WEST,
            // MC
            MC_CET, MC_CEST,
            // ME
            ME_CET, ME_CEST,
            // MG
            MG_EAT,
            // MH
            MH_MHT,
            // MK
            MK_CET, MK_CEST,
            // ML
            ML_GMT,
            // MM
            MM_MMT,
            // MN
            MN_HOVT, MN_ULAT,
            // MR
            MR_GMT,
            // MT
            MT_CET, MT_CEST,
            // MU
            MU_MUT,
            // MV
            MV_MVT,
            // MW
            MW_CAT,
            // MX
            MX_CST, MX_CDT, MX_EST, MX_MST, MX_MDT, MX_PST, MX_PDT,
            // MY
            MY_MYT,
            // MZ
            MZ_CAT,
            // NA
            NA_CAT,
            // NE
            NE_WAT,
            // NG
            NG_WAT,
            // NI
            NI_CST,
            // NL
            NL_CET, NL_CEST, NL_AST,
            // NO
            NO_CET, NO_CEST,
            // NP
            NP_NPT,
            // NR
            NR_NRT,
            // NZ
            NZ_NZST, NZ_NZDT,
            // OM
            OM_GST,
            // PA
            PA_EST,
            // PE
            PE_PET,
            // PG
            PG_PGT,
            // PH
            PH_PHT,
            // PK
            PK_PKT,
            // PL
            PL_CET, PL_CEST,
            // PT
            PT_WET, PT_WEST,
            // PY
            PY_PYT, PY_PYST,
            // QA
            QA_AST,
            // RO
            RO_EET, RO_EEST,
            // RS
            RS_CET, RS_CEST,
            // RU
            RU_KALT, RU_MSK, RU_SAMT, RU_YEKT, RU_OMST, RU_KRAT, RU_IRKT,
            RU_YAKT, RU_VLAT, RU_MAGT, RU_PETT,
            // RW
            RW_CAT,
            // SA
            SA_AST,
            // SB
            SB_SBT,
            // SC
            SC_SCT,
            // SD
            SD_CAT,
            // SE
            SE_CET, SE_CEST,
            // SG
            SG_SGT,
            // SI
            SI_CET, SI_CEST,
            // SK
            SK_CET, SK_CEST,
            // SL
            SL_GMT,
            // SM
            SM_CET, SM_CEST,
            // SN
            SN_GMT,
            // SO
            SO_EAT,
            // SR
            SR_SRT,
            // ST
            ST_GMT,
            // SV
            SV_CST,
            // SY
            SY_EET, SY_EEST,
            // SZ
            SZ_SAST,
            // TD
            TD_WAT,
            // TG
            TG_GMT,
            // TH
            TH_ICT,
            // TJ
            TJ_TJT,
            // TL
            TL_TLT,
            // TM
            TM_TMT,
            // TN
            TN_CET,
            // TO
            TO_TOT,
            // TR
            TR_TRT,
            // TT
            TT_AST,
            // TV
            TV_TVT,
            // TZ
            TZ_EAT,
            // UA
            UA_EET, UA_EEST,
            // UG
            UG_EAT,
            // US
            US_AST, US_AKST, US_AKDT, US_CHST, US_CST, US_CDT, US_EST, US_EDT,
            US_HST, US_MST, US_MDT, US_PST, US_PDT, US_SST,
            // UY
            UY_UYT,
            // UZ
            UZ_UZT,
            // VC
            VC_AST,
            // VE
            VE_VET,
            // VN
            VN_ICT,
            // VU
            VU_VUT,
            // WS
            WS_WST,
            // ZA
            ZA_SAST,
            // ZM
            ZM_CAT,
            // ZW
            ZW_CAT,
            // UTC
            UTC
        ).associateBy { it.id }

        /**
         * Reverse lookup map from ZoneOffset to list of KiTZ instances.
         */
        private val BY_OFFSET: Map<ZoneOffset, List<KiTZ>> by lazy {
            BY_ID.values
                .filter { it != UTC }
                .groupBy { it.offset }
                .mapValues { it.value.sortedBy { tz -> tz.id } }
        }

        /**
         * Preferred KiTZ for each offset (prioritizes US, then major regions).
         */
        private val PREFERRED: Map<ZoneOffset, KiTZ> by lazy {
            val countryPriority = listOf("US", "GB", "JP", "DE", "FR", "AU", "CA", "CN", "IN")

            BY_OFFSET.mapValues { (_, timezones) ->
                timezones.minWithOrNull(compareBy(
                    { tz ->
                        val index = countryPriority.indexOf(tz.countryCode)
                        if (index >= 0) index else countryPriority.size
                    },
                    { it.id }
                ))!!
            }
        }

        /**
         * Gets a KiTZ instance by its identifier.
         */
        @JvmStatic
        operator fun get(id: String): KiTZ? = BY_ID[id]

        /**
         * Gets a KiTZ instance by its identifier, throwing if not found.
         */
        @JvmStatic
        fun require(id: String): KiTZ = BY_ID[id]
            ?: throw IllegalArgumentException("Invalid KiTZ identifier: $id")

        /**
         * Parse a KiTZ identifier string.
         *
         * Valid formats include:
         * - Standard KiTZ identifiers: "US/PST", "JP/JST", "DE/CET"
         * - Special identifiers: "Z", "UTC", "GMT"
         *
         * ```kotlin
         * val pst = KiTZ.parse("US/PST")
         * val utc = KiTZ.parse("UTC")
         * ```
         *
         * @param text The KiTZ identifier string
         * @return The parsed KiTZ
         * @throws ParseException if the identifier is not valid
         */
        @JvmStatic
        fun parse(text: String): KiTZ {
            val trimmed = text.trim()

            // Handle special UTC identifiers
            return when (trimmed) {
                "Z", "UTC", "GMT" -> UTC
                else -> BY_ID[trimmed]
                    ?: throw ParseException("Invalid KiTZ identifier: $trimmed")
            }
        }

        /**
         * Parses a KiTZ identifier string into a KiTZ instance.
         *
         * @param text The KiTZ identifier string to parse
         * @return The parsed KiTZ
         * @throws ParseException if the text cannot be parsed as a valid KiTZ
         */
        override fun parseLiteral(text: String): KiTZ = parse(text)

        /**
         * Parse a KiTZ identifier, returning null on failure instead of throwing.
         *
         * @param text The KiTZ identifier string
         * @return The parsed KiTZ, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(text: String): KiTZ? = try {
            parse(text)
        } catch (e: Exception) {
            null
        }

        /**
         * Returns the preferred KiTZ for the given offset, or null if none exists.
         */
        @JvmStatic
        fun fromOffset(offset: ZoneOffset): KiTZ? {
            if (offset == ZoneOffset.UTC) return UTC
            return PREFERRED[offset]
        }

        /**
         * Returns all KiTZ instances that map to the given offset.
         */
        @JvmStatic
        fun allFromOffset(offset: ZoneOffset): List<KiTZ> {
            if (offset == ZoneOffset.UTC) return listOf(UTC)
            return BY_OFFSET[offset] ?: emptyList()
        }

        /**
         * Returns the KiTZ for a specific country and offset, if one exists.
         */
        @JvmStatic
        fun fromCountryAndOffset(countryCode: String, offset: ZoneOffset): KiTZ? {
            val upper = countryCode.uppercase()
            return allFromOffset(offset).find { it.countryCode == upper }
        }

        /**
         * Checks if a KiTZ identifier is valid.
         */
        @JvmStatic
        fun isValid(id: String): Boolean = BY_ID.containsKey(id)

        /**
         * Returns all registered KiTZ instances.
         */
        @JvmStatic
        fun all(): Collection<KiTZ> = BY_ID.values

        /**
         * Returns all KiTZ identifiers.
         */
        @JvmStatic
        fun allIds(): Set<String> = BY_ID.keys

        /**
         * Returns all unique offsets that have KiTZ mappings.
         */
        @JvmStatic
        fun allOffsets(): Set<ZoneOffset> = BY_OFFSET.keys + ZoneOffset.UTC

        /**
         * Returns all supported country names.
         */
        @JvmStatic
        fun allCountries(): Collection<String> = COUNTRY_NAMES.values.sorted()

        /**
         * Returns the country name for a given country code.
         */
        @JvmStatic
        fun countryName(countryCode: String): String? = COUNTRY_NAMES[countryCode.uppercase()]
    }
}