package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeStock(
        symbol: String,
        companyName: String,
        currentPrice: Double,
        priceChange: Double,
        marketCap: String,
        peRatio: Double?,
        volume: String,
        high52w: Double,
        low52w: Double,
        customPrompt: String? = null
    ): GeminiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or placeholder.")
            return@withContext GeminiAnalysisResult(
                success = false,
                sentiment = "HOLD",
                score = 50,
                summary = "API Key not configured. Please enter your GEMINI_API_KEY into the Secrets Panel in AI Studio to enable live AI analysis.",
                technicalAnalysis = "Metrics look active but AI is offline. Price: $$currentPrice ($priceChange%).",
                catalysts = listOf("Secrets Panel configuration required."),
                risks = listOf("Missing API Key."),
                fairValue = currentPrice
            )
        }

        val systemInstruction = """
            You are Finpass AI, an elite stock analysis terminal. Analyze the given stock symbols and provide highly clear, accurate, and professional investment breakdowns in a structured JSON response.
            Your output MUST be a valid JSON object matching the following fields exactly, with NO markdown formatting wrapper, NO backticks (like ```json ... ```), and NO surrounding text. Output ONLY the JSON:
            {
               "sentiment": "BUY" or "SELL" or "HOLD",
               "score": Integer between 0 and 100,
               "summary": "String summarizing company position",
               "technicalAnalysis": "String with technical overview",
               "catalysts": ["String 1", "String 2"],
               "risks": ["String 1", "String 2"],
               "fairValue": Double value
            }
        """.trimIndent()

        val promptText = if (customPrompt != null) {
            "Custom Query: $customPrompt\n\nAnalyze the stock $symbol ($companyName) with current price $$currentPrice, price change $priceChange, market cap $marketCap, P/E ratio ${peRatio ?: "N/A"}, volume $volume, 52-week high $$high52w, and 52-week low $$low52w."
        } else {
            "Analyze the stock $symbol ($companyName) with current price $$currentPrice, price change $priceChange, market cap $marketCap, P/E ratio ${peRatio ?: "N/A"}, volume $volume, 52-week high $$high52w, and 52-week low $$low52w."
        }

        try {
            // Build Request JSON
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: Code ${response.code}, Msg: $errorMsg")
                    return@withContext GeminiAnalysisResult(
                        success = false,
                        sentiment = "HOLD",
                        score = 50,
                        summary = "Unable to fetch AI overview. System received status code ${response.code}.",
                        technicalAnalysis = "API is currently responsive but returning error.",
                        catalysts = emptyList(),
                        risks = listOf("Server communication issue."),
                        fairValue = currentPrice
                    )
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                Log.d(TAG, "Response: $responseBody")

                val root = JSONObject(responseBody)
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        val responseText = parts.getJSONObject(0).getString("text").trim()
                        // Strip any markdown backticks in case model didn't respect instruction
                        val cleanJson = if (responseText.startsWith("```")) {
                            responseText
                                .removePrefix("```json")
                                .removePrefix("```")
                                .removeSuffix("```")
                                .trim()
                        } else {
                            responseText
                        }

                        val parsedResult = JSONObject(cleanJson)
                        val catalystsList = mutableListOf<String>()
                        val catArray = parsedResult.optJSONArray("catalysts")
                        if (catArray != null) {
                            for (i in 0 until catArray.length()) {
                                catalystsList.add(catArray.getString(i))
                            }
                        }

                        val risksList = mutableListOf<String>()
                        val riskArray = parsedResult.optJSONArray("risks")
                        if (riskArray != null) {
                            for (i in 0 until riskArray.length()) {
                                risksList.add(riskArray.getString(i))
                            }
                        }

                        return@withContext GeminiAnalysisResult(
                            success = true,
                            sentiment = parsedResult.optString("sentiment", "HOLD"),
                            score = parsedResult.optInt("score", 50),
                            summary = parsedResult.optString("summary", "No summary provided."),
                            technicalAnalysis = parsedResult.optString("technicalAnalysis", "No technical analysis provided."),
                            catalysts = catalystsList,
                            risks = risksList,
                            fairValue = parsedResult.optDouble("fairValue", currentPrice)
                        )
                    }
                }

                return@withContext GeminiAnalysisResult(
                    success = false,
                    sentiment = "HOLD",
                    score = 50,
                    summary = "No response text found from AI.",
                    technicalAnalysis = "",
                    catalysts = emptyList(),
                    risks = emptyList(),
                    fairValue = currentPrice
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            return@withContext GeminiAnalysisResult(
                success = false,
                sentiment = "HOLD",
                score = 50,
                summary = "Error during AI consultation: ${e.localizedMessage ?: "Unknown connection error"}.",
                technicalAnalysis = "Please make sure you are connected to the network.",
                catalysts = emptyList(),
                risks = listOf(e.localizedMessage ?: "Network error"),
                fairValue = currentPrice
            )
        }
    }

    suspend fun callGeminiRawText(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "⚠️ API Key not configured. Please enter your GEMINI_API_KEY into the Secrets Panel in AI Studio to enable live AI analysis on your inventory database."
        }

        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "❌ Error: Received API status ${response.code}. Try again later."
                }
                val responseBody = response.body?.string() ?: return@withContext "❌ Empty response from Gemini AI."
                val root = JSONObject(responseBody)
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text").trim()
                    }
                }
                return@withContext "❌ AI could not find suitable response content."
            }
        } catch (e: Exception) {
            return@withContext "❌ Error: ${e.localizedMessage ?: "Unknown connection error"}. Check network connection."
        }
    }
}

data class GeminiAnalysisResult(
    val success: Boolean,
    val sentiment: String,
    val score: Int,
    val summary: String,
    val technicalAnalysis: String,
    val catalysts: List<String>,
    val risks: List<String>,
    val fairValue: Double
)
