package snd.komf.util

enum class OsPlatform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        val Current: OsPlatform by lazy {
            val name = System.getProperty("os.name")
            when {
                name?.startsWith("Linux") == true -> Linux
                name?.startsWith("Win") == true -> Windows
                name?.startsWith("Mac OS X") == true -> MacOS
                else -> Unknown
            }
        }
    }
}
