package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Gpt4ResponseParserTest {

  @Test
  fun `parses result property from JSON`() {
    val json = """{"status":true,"creator":"nexadev","result":"PHP adalah bahasa skrip server-side."}"""
    assertEquals("PHP adalah bahasa skrip server-side.", Gpt4ResponseParser.parseResponseBody(json))
  }

  @Test
  fun `parses nested data answer property`() {
    val json = """{"success":true,"data":{"answer":"x = 4"}}"""
    assertEquals("x = 4", Gpt4ResponseParser.parseResponseBody(json))
  }

  @Test
  fun `parses feloai result ignoring extra sources array`() {
    val json = """{"status":true,"creator":"Faa","result":{"answer":"PHP adalah bahasa pemrograman server-side.","sources":["https://php.net"]}}"""
    assertEquals("PHP adalah bahasa pemrograman server-side.", Gpt4ResponseParser.parseResponseBody(json))
  }

  @Test
  fun `parses nexray claude result format`() {
    val json = """{"status":true,"author":"nexray","result":"HTML adalah singkatan dari HyperText Markup Language."}"""
    assertEquals("HTML adalah singkatan dari HyperText Markup Language.", Gpt4ResponseParser.parseResponseBody(json))
  }

  @Test
  fun `parses OpenAI choices format`() {
    val json = """{"id":"1","choices":[{"message":{"role":"assistant","content":"Langkah 1: ..."}}]}"""
    assertEquals("Langkah 1: ...", Gpt4ResponseParser.parseResponseBody(json))
  }

  @Test
  fun `parses unknown custom property without assuming key name`() {
    val json = """{"status":true,"gpt_output_text":"Hasil akhirnya adalah 42."}"""
    assertEquals("Hasil akhirnya adalah 42.", Gpt4ResponseParser.parseResponseBody(json))
  }

  @Test
  fun `throws ApiResponseException on error JSON so errors are never shown as AI answer`() {
    val json = """{"status":false,"error":"Rate limit exceeded"}"""
    assertThrows(ApiResponseException::class.java) {
      Gpt4ResponseParser.parseResponseBody(json)
    }
  }

  @Test
  fun `throws ApiResponseException on HTML error page`() {
    val html = "<!DOCTYPE html><html><body>502 Bad Gateway</body></html>"
    assertThrows(ApiResponseException::class.java) {
      Gpt4ResponseParser.parseResponseBody(html)
    }
  }
}
