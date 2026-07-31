package com.my.vpn.update

/**
 * Превращает текст GitHub Release в читаемый список изменений без команд и блоков кода.
 */
object ChangelogFormatter {

    fun formatEntries(raw: String): List<String> {
        if (raw.isBlank()) return listOf("См. страницу релиза на GitHub.")

        val noCodeBlocks = raw.replace(Regex("```[\\s\\S]*?```"), "\n")
        val lines = noCodeBlocks.lines()
        val entries = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (isSkippableLine(trimmed)) continue

            val item = when {
                trimmed.startsWith("##") -> trimmed.removePrefix("#").trim()
                trimmed.startsWith("###") -> trimmed.removePrefix("#").trim()
                trimmed.matches(Regex("^[-*•]\\s+.+")) ->
                    trimmed.replace(Regex("^[-*•]\\s+"), "").trim()
                trimmed.matches(Regex("^\\d+[.)]\\s+.+")) ->
                    trimmed.replace(Regex("^\\d+[.)]\\s+"), "").trim()
                else -> trimmed
            }
            if (item.isNotBlank() && !isSkippableLine(item)) {
                entries.add(item)
            }
        }

        return entries.distinct().take(40).ifEmpty {
            listOf("Обновление доступно на GitHub.")
        }
    }

    private fun isSkippableLine(line: String): Boolean {
        val lower = line.lowercase()
        if (line.startsWith("```")) return true
        if (line.startsWith("$ ") || line.startsWith("$\t")) return true
        if (line.startsWith("curl ") || line.startsWith("wget ")) return true
        if (line.startsWith("./") || line.startsWith("gradlew")) return true
        if (line.startsWith("git clone") || line.startsWith("git pull")) return true
        if (line.startsWith("npm ") || line.startsWith("pip ")) return true
        if (line.startsWith("bash ") || line.startsWith("sh ") || line.startsWith("powershell")) return true
        if (lower.startsWith("```bash") || lower.startsWith("```sh")) return true
        if (line.matches(Regex("^https?://\\S+$"))) return true
        if (line.length > 280) return true
        return false
    }
}
