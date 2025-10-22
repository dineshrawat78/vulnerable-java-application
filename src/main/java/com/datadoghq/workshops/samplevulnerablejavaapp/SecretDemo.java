/**
 * SecretDemo.java
 *
 * Educational demo: shows (1) an insecure pattern using a hard-coded secret placeholder
 *                     (2) a safe alternative using environment variables
 *                     (3) an example of a tiny "detector" to find obvious hard-coded secrets
 *
 * IMPORTANT: This file intentionally DOES NOT contain any real credentials or secrets.
 *            If you copy the insecure pattern for testing, make sure the "secret" is a placeholder,
 *            never use real API keys, passwords or tokens here.
 *
 * Remediation tips:
 *  - Never hard-code secrets or credentials in source.
 *  - Do not log secrets. Mask them if needed (e.g. show only last 4 chars).
 *  - Use environment variables, OS-level secret stores, or a secret manager (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault, etc).
 *  - Add secret-detection to CI (truffleHog, git-secrets, detect-secrets).
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecretDemo {

    public static void main(String[] args) {
        System.out.println("=== Running insecure example (placeholder only) ===");
        insecureExample();

        System.out.println("\n=== Running secure example ===");
        secureExample();

        System.out.println("\n=== Running tiny detector on this source string (simulated) ===");
        // Simulated source snippet (in real world you would scan files from repo)
        String simulatedSource = "String apiKey = \"<HARD_CODED_SECRET>\"; // DO NOT COMMIT real keys";
        detectHardCodedSecrets(simulatedSource);
    }

    /**
     * Insecure example: demonstrates the pattern the user asked about (hard-coded + logging).
     * NOTE: This uses a placeholder string "<HARD_CODED_SECRET>" and not a real secret.
     *
     * DO NOT do this in production.
     */
    private static void insecureExample() {
        // Hard-coded secret placeholder
        String hardCodedApiKey = "<HARD_CODED_SECRET>"; // <-- placeholder only!

        String password = "test@1234sdfasf"; // <-- placeholder only!

        String apiKey = "sec1-test1234ugugugiugtiutuytuyfuyhfghjfjhgjhgjhghjghjkhjkhjkhkjy"; // <-- placeholder only!

        // Insecure: logging the secret directly (for demonstration only)
        System.out.println("Insecure log: using API key = " + hardCodedApiKey);
        System.out.println("Insecure log: using API key = " + apiKey);
         System.out.println("Insecure log: using password = " + password);

        // Simulate using the secret
        boolean ok = callExternalServiceWithKey(hardCodedApiKey);
        System.out.println("Call successful: " + ok);
    }

    /**
     * Secure example: retrieves secret from environment (or any proper secret manager) and masks logs.
     */
    private static void secureExample() {
        // Preferred: fetch from environment variable (or secret manager). Fallback to null if missing.
        String apiKey = System.getenv("MY_APP_API_KEY"); // set in OS/container, never in code
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Secure: API key not found in environment. Abort or use safe fallback.");
            return;
        }

        // NEVER log the full secret. Mask before logging.
        System.out.println("Secure log: using API key (masked) = " + maskSecret(apiKey));

        // Use the secret safely
        boolean ok = callExternalServiceWithKey(apiKey);
        System.out.println("Call successful: " + ok);
    }

    /**
     * Simulates calling an external service with a key.
     * This is a dummy function for demonstration and returns true if key non-empty.
     */
    private static boolean callExternalServiceWithKey(String key) {
        if (key == null || key.isEmpty()) return false;
        // ... perform API call (omitted)
        return true;
    }

    /**
     * Masks a secret for safe logging. Example: show first 0 and last 4 chars (configurable).
     */
    private static String maskSecret(String secret) {
        if (secret == null) return null;
        int show = 4; // show last 4 chars only
        if (secret.length() <= show) return "****";
        String last = secret.substring(secret.length() - show);
        return "****" + last;
    }

    /**
     * Very small heuristic to detect obvious hard-coded secrets in a string of source.
     * This is NOT a full secret scanner — just an educational demo.
     *
     * Detects patterns like:
     *   - "String .* = \"<something that looks long and secret>\""
     *   - common keywords like "password", "apikey", "secret", "token" near an assignment
     */
    private static void detectHardCodedSecrets(String source) {
        // Pattern: variable assignment to a quoted literal (basic)
        Pattern assignmentPattern = Pattern.compile("\\b(String|char|var|final)\\b\\s+([a-zA-Z0-9_]+)\\s*=\\s*\"([^\"]{4,})\"", Pattern.CASE_INSENSITIVE);
        Matcher m = assignmentPattern.matcher(source);
        boolean found = false;
        while (m.find()) {
            String varName = m.group(2);
            String literal = m.group(3);
            // Check for password-like variable names OR long random-looking literal
            if (varName.toLowerCase().contains("password") ||
                varName.toLowerCase().contains("secret") ||
                varName.toLowerCase().contains("apikey") ||
                varName.toLowerCase().contains("token") ||
                looksLikeRandom(literal)) {

                System.out.println("WARNING: possible hard-coded secret found: variable '" + varName + "' with literal length " + literal.length());
                found = true;
            }
        }

        // Extra quick check for comments or assignments containing keywords
        Pattern keywordPattern = Pattern.compile("\\b(password|passwd|secret|apikey|api_key|token)\\b", Pattern.CASE_INSENSITIVE);
        if (keywordPattern.matcher(source).find()) {
            System.out.println("NOTE: secret-related keyword(s) found in source (further manual review recommended).");
            found = true;
        }

        if (!found) {
            System.out.println("No obvious hard-coded secrets detected by this simple heuristic.");
        }
    }

    /**
     * Heuristic: does the literal look random (base64-like or long hex)?
     * Very simplistic — only for demo.
     */
    private static boolean looksLikeRandom(String s) {
        // If length > 20 and contains a lot of alphanumerics/punctuation typical of keys
        if (s.length() > 20) {
            int alphaNumCount = 0;
            for (char c : s.toCharArray()) {
                if (Character.isLetterOrDigit(c) || c == '+' || c == '/' || c == '=' || c == '-') alphaNumCount++;
            }
            return alphaNumCount >= s.length() * 0.8;
        }
        return false;
    }
}
