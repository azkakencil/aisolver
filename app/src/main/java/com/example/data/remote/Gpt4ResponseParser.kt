package com.example.data.remote

class ApiResponseException(message: String) : Exception(message)

object Gpt4ResponseParser {

  private val preferredAnswerKeys =
    listOf(
      "result",
      "answer",
      "response",
      "reply",
      "content",
      "message",
      "text",
      "output",
      "claude",
      "feloai",
      "felo",
      "gpt4",
      "ai",
      "data",
    )

  private val metadataKeys =
    setOf(
      "status",
      "success",
      "code",
      "statuscode",
      "creator",
      "author",
      "timestamp",
      "model",
      "id",
      "object",
      "query",
      "q",
      "error",
      "err",
      "errormessage",
    )

  /**
   * Flexibly parses the actual JSON, SSE, or plain-text payload returned by the backend API
   * without assuming fixed property names, and rejects error payloads so API errors are never
   * displayed as AI answers.
   */
  fun parseResponseBody(rawBody: String): String {
    val trimmed = rawBody.trim()
    if (trimmed.isEmpty()) {
      throw ApiResponseException("Server AI mengembalikan respons kosong.")
    }

    if (trimmed.startsWith("data:")) {
      val sseContent = parseSseLines(trimmed)
      if (sseContent.isNotBlank()) return sseContent
    }

    if (trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) ||
      trimmed.startsWith("<html", ignoreCase = true)
    ) {
      throw ApiResponseException("Server AI sedang tidak dapat diakses (HTML error response).")
    }

    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      val parsed = MiniJson.parse(trimmed)
      return when (parsed) {
        is Map<*, *> -> parseJsonMap(parsed)
        is List<*> ->
          extractFromList(parsed)
            ?: throw ApiResponseException("Format array JSON respons AI tidak berisi jawaban.")
        else -> throw ApiResponseException("Format JSON respons AI tidak dikenali.")
      }
    }

    if (isLikelyErrorText(trimmed)) {
      throw ApiResponseException(trimmed)
    }
    return trimmed
  }

  fun extractSseChunk(line: String): String? {
    val clean = line.trim()
    if (!clean.startsWith("data:")) return null
    val payload = clean.removePrefix("data:").trim()
    if (payload.isEmpty() || payload == "[DONE]") return null
    return if (payload.startsWith("{")) {
      runCatching {
          val map = MiniJson.parse(payload) as? Map<*, *> ?: return@runCatching null
          parseJsonMap(map)
        }
        .getOrNull()
    } else {
      payload
    }
  }

  private fun parseSseLines(raw: String): String {
    val sb = StringBuilder()
    raw.lineSequence().forEach { line ->
      val chunk = extractSseChunk(line)
      if (!chunk.isNullOrEmpty()) sb.append(chunk)
    }
    return sb.toString().trim()
  }

  private fun parseJsonMap(map: Map<*, *>): String {
    val statusVal = findValueIgnoreCase(map, "status")
    val successVal = findValueIgnoreCase(map, "success")
    val codeVal = (findValueIgnoreCase(map, "code") as? Number)?.toInt() ?: 200

    val hasFalseStatus = (statusVal == false) || (successVal == false)
    val explicitError =
      listOf("error", "err", "errorMessage", "error_message").firstNotNullOfOrNull { key ->
        val v = findValueIgnoreCase(map, key)
        when (v) {
          null -> null
          is Boolean -> if (v) "Terjadi kesalahan pada API AI." else null
          is Map<*, *> ->
            (findValueIgnoreCase(v, "message") as? String)?.takeIf { it.isNotBlank() }
              ?: v.toString()
          else -> v.toString().takeIf { it.isNotBlank() && !it.equals("false", ignoreCase = true) }
        }
      }

    if (hasFalseStatus || codeVal >= 400 || explicitError != null) {
      val msg =
        explicitError
          ?: (findValueIgnoreCase(map, "message") as? String)?.takeIf { it.isNotBlank() }
          ?: (findValueIgnoreCase(map, "msg") as? String)?.takeIf { it.isNotBlank() }
          ?: "API mengembalikan status gagal (kode $codeVal)."
      throw ApiResponseException(msg)
    }

    val choices = findValueIgnoreCase(map, "choices") as? List<*>
    if (!choices.isNullOrEmpty()) {
      val first = choices.firstOrNull() as? Map<*, *>
      if (first != null) {
        val msgObj =
          (findValueIgnoreCase(first, "message") as? Map<*, *>)
            ?: (findValueIgnoreCase(first, "delta") as? Map<*, *>)
        val choiceContent =
          (msgObj?.let { findValueIgnoreCase(it, "content") } as? String)?.takeIf {
            it.isNotBlank()
          }
            ?: (findValueIgnoreCase(first, "text") as? String)?.takeIf { it.isNotBlank() }
        if (!choiceContent.isNullOrBlank()) return choiceContent.trim()
      }
    }

    for (key in preferredAnswerKeys) {
      val value = findValueIgnoreCase(map, key) ?: continue
      val extracted = extractValue(value)
      if (!extracted.isNullOrBlank()) return extracted.trim()
    }

    val candidates = mutableListOf<String>()
    for ((rawKey, value) in map) {
      val k = rawKey?.toString()?.lowercase() ?: continue
      if (k in metadataKeys) continue
      val extracted = extractValue(value)
      if (!extracted.isNullOrBlank()) candidates.add(extracted.trim())
    }

    val best = candidates.maxByOrNull { it.length }
    if (!best.isNullOrBlank()) return best

    throw ApiResponseException("Respons API tidak mengandung teks jawaban AI.")
  }

  private fun findValueIgnoreCase(map: Map<*, *>, target: String): Any? {
    if (map.containsKey(target)) return map[target]
    return map.entries.firstOrNull { it.key?.toString()?.equals(target, ignoreCase = true) == true }
      ?.value
  }

  private fun extractValue(value: Any?): String? {
    return when (value) {
      null,
      is Boolean,
      is Number,
      -> null
      is String -> value.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
      is Map<*, *> -> runCatching { parseJsonMap(value) }.getOrNull()
      is List<*> -> extractFromList(value)
      else -> value.toString().takeIf { it.isNotBlank() }
    }
  }

  private fun extractFromList(list: List<*>): String? {
    val parts = list.mapNotNull { extractValue(it) }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
  }

  private fun isLikelyErrorText(text: String): Boolean {
    val lower = text.lowercase()
    return (lower.startsWith("error:") ||
      lower.contains("502 bad gateway") ||
      lower.contains("504 gateway") ||
      lower.contains("404 not found"))
  }
}

private object MiniJson {
  fun parse(input: String): Any? {
    val parser = Parser(input)
    val value = parser.readValue()
    parser.skipWhitespace()
    return value
  }

  private class Parser(private val src: String) {
    private var idx = 0

    fun skipWhitespace() {
      while (idx < src.length && src[idx].isWhitespace()) idx++
    }

    fun readValue(): Any? {
      skipWhitespace()
      if (idx >= src.length) throw IllegalArgumentException("Unexpected end of JSON")
      return when (val c = src[idx]) {
        '{' -> readObject()
        '[' -> readArray()
        '"' -> readString()
        't',
        'f' -> readBoolean()
        'n' -> readNull()
        '-',
        in '0'..'9' -> readNumber()
        else -> throw IllegalArgumentException("Unexpected char '$c' at $idx")
      }
    }

    private fun readObject(): Map<String, Any?> {
      idx++ // '{'
      val map = LinkedHashMap<String, Any?>()
      skipWhitespace()
      if (idx < src.length && src[idx] == '}') {
        idx++
        return map
      }
      while (idx < src.length) {
        skipWhitespace()
        val key = readString()
        skipWhitespace()
        if (idx >= src.length || src[idx] != ':') {
          throw IllegalArgumentException("Expected ':' at $idx")
        }
        idx++
        val value = readValue()
        map[key] = value
        skipWhitespace()
        if (idx < src.length && src[idx] == ',') {
          idx++
        } else if (idx < src.length && src[idx] == '}') {
          idx++
          return map
        } else {
          throw IllegalArgumentException("Expected ',' or '}' at $idx")
        }
      }
      return map
    }

    private fun readArray(): List<Any?> {
      idx++ // '['
      val list = ArrayList<Any?>()
      skipWhitespace()
      if (idx < src.length && src[idx] == ']') {
        idx++
        return list
      }
      while (idx < src.length) {
        list.add(readValue())
        skipWhitespace()
        if (idx < src.length && src[idx] == ',') {
          idx++
        } else if (idx < src.length && src[idx] == ']') {
          idx++
          return list
        } else {
          throw IllegalArgumentException("Expected ',' or ']' at $idx")
        }
      }
      return list
    }

    private fun readString(): String {
      if (src[idx] != '"') throw IllegalArgumentException("Expected '\"' at $idx")
      idx++
      val sb = StringBuilder()
      while (idx < src.length) {
        val c = src[idx++]
        when (c) {
          '"' -> return sb.toString()
          '\\' -> {
            if (idx >= src.length) break
            when (val esc = src[idx++]) {
              '"',
              '\\',
              '/' -> sb.append(esc)
              'b' -> sb.append('\b')
              'f' -> sb.append('\u000C')
              'n' -> sb.append('\n')
              'r' -> sb.append('\r')
              't' -> sb.append('\t')
              'u' -> {
                if (idx + 4 <= src.length) {
                  val hex = src.substring(idx, idx + 4)
                  sb.append(hex.toInt(16).toChar())
                  idx += 4
                }
              }
              else -> sb.append(esc)
            }
          }
          else -> sb.append(c)
        }
      }
      throw IllegalArgumentException("Unterminated string")
    }

    private fun readBoolean(): Boolean {
      return if (src.startsWith("true", idx)) {
        idx += 4
        true
      } else if (src.startsWith("false", idx)) {
        idx += 5
        false
      } else {
        throw IllegalArgumentException("Invalid boolean at $idx")
      }
    }

    private fun readNull(): Any? {
      if (src.startsWith("null", idx)) {
        idx += 4
        return null
      }
      throw IllegalArgumentException("Invalid null at $idx")
    }

    private fun readNumber(): Number {
      val start = idx
      while (idx < src.length && src[idx] in "0123456789+-.eE") idx++
      val numStr = src.substring(start, idx)
      return if (numStr.contains('.') || numStr.contains('e', true)) {
        numStr.toDouble()
      } else {
        numStr.toLong()
      }
    }
  }
}
