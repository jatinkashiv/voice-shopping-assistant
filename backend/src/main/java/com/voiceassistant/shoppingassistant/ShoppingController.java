package com.voiceassistant.shoppingassistant;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

// ─────────────────────────────────────────────
// REST CONTROLLER
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ShoppingController {

    @Autowired
    private ItemRepo repo;

    @Autowired
    private GeminiService geminiService;

    /**
     * Accepts a voice command string (already transcribed from speech-to-text),
     * sends it to Gemini for understanding, and saves the result to DB.
     *
     * Example request body:
     * { "command": "add two kg of apples" }
     */
    @PostMapping("/voice-command")
    public ResponseEntity<?> handleVoiceCommand(@RequestBody Map<String, String> body) {
        String command = body.get("command");
        if (command == null || command.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "command is required"));
        }

        try {
            Item item = geminiService.parseCommandWithAI(command);
            Item saved = repo.save(item);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            // Fallback to regex NLP if Gemini fails
            Item item = parseCommandFallback(command);
            Item saved = repo.save(item);
            return ResponseEntity.ok(Map.of(
                    "item", saved,
                    "warning", "Gemini unavailable, used fallback parser: " + e.getMessage()));
        }
    }

    @GetMapping("/items")
    public List<Item> getAll() {
        return repo.findAll();
    }

    @DeleteMapping("/items")
    public void deleteAll() {
        repo.deleteAll();
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable Long id) {
        repo.deleteById(id);
    }

    // ── Regex fallback (your original logic) ──────────────────────────────────
    private Item parseCommandFallback(String command) {
        Map<String, Integer> wordToNumber = Map.of(
                "one", 1, "two", 2, "three", 3, "four", 4, "five", 5,
                "six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10);

        command = command.trim().toLowerCase();

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?i)(add|buy|need)\\s+(\\d+|one|two|three|four|five|six|seven|eight|nine|ten)\\s+(\\w+)\\s+(?:of\\s+)?(.+)");
        java.util.regex.Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            String quantityStr = matcher.group(2).toLowerCase();
            int quantity = wordToNumber.getOrDefault(quantityStr, -1);
            if (quantity == -1) {
                try { quantity = Integer.parseInt(quantityStr); }
                catch (NumberFormatException e) { quantity = 1; }
            }
            String unit = matcher.group(3).toLowerCase();
            String name = matcher.group(4).toLowerCase().trim();
            return new Item(null, name, "general", quantity, unit);
        }

        java.util.regex.Pattern simple = java.util.regex.Pattern.compile("(?i)(add|buy|need)\\s+(.+)");
        java.util.regex.Matcher sm = simple.matcher(command);
        if (sm.find()) {
            return new Item(null, sm.group(2).toLowerCase().trim(), "general", 1, "unit");
        }

        return new Item(null, command, "general", 1, "unit");
    }
}

// ─────────────────────────────────────────────
// GEMINI SERVICE
// ─────────────────────────────────────────────
@Service
class GeminiService {

    // Set in application.properties:  gemini.api.key=YOUR_KEY
    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends the voice command to Gemini and parses the structured JSON response
     * into an Item object.
     */
    public Item parseCommandWithAI(String command) throws Exception {

        // 1. Build the prompt asking Gemini to return strict JSON
        String prompt = """
                You are a shopping assistant AI. Extract structured data from this voice command.

                Voice command: "%s"

                Respond ONLY with valid JSON (no markdown, no explanation):
                {
                  "name": "<item name>",
                  "category": "<category: fruit/vegetable/dairy/meat/beverage/snack/general>",
                  "quantity": <number>,
                  "unit": "<unit: kg/g/litre/ml/piece/dozen/pack/unit>"
                }

                If you cannot extract a clear item, use sensible defaults.
                """.formatted(command);

        // 2. Build Gemini request body
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,        // low temp = more deterministic JSON
                        "maxOutputTokens", 200
                )
        );

        // 3. Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 4. Call Gemini API (API key passed as query param)
        String url = GEMINI_URL + "?key=" + apiKey;
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // 5. Parse Gemini response
        return parseGeminiResponse(response.getBody(), command);
    }

    private Item parseGeminiResponse(String responseBody, String originalCommand) throws Exception {
        JsonNode root = mapper.readTree(responseBody);

        // Extract the text from Gemini's response structure:
        // candidates[0].content.parts[0].text
        String text = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();

        // Strip any accidental markdown fences
        text = text.replaceAll("```json|```", "").trim();

        // Parse the JSON Gemini returned
        JsonNode itemJson = mapper.readTree(text);

        String name     = itemJson.path("name").asText("unknown").toLowerCase();
        String category = itemJson.path("category").asText("general").toLowerCase();
        int quantity    = itemJson.path("quantity").asInt(1);
        String unit     = itemJson.path("unit").asText("unit").toLowerCase();

        return new Item(null, name, category, quantity, unit);
    }
}

// ─────────────────────────────────────────────
// REPOSITORY
// ─────────────────────────────────────────────
@Repository
interface ItemRepo extends JpaRepository<Item, Long> {
}  