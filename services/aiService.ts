import { GoogleGenAI } from "@google/genai";

// Prefer Vite env vars; fall back to Node env for tests/tooling.
const apiKey =
  (import.meta as any).env?.VITE_GEMINI_API_KEY ||
  (import.meta as any).env?.VITE_GENAI_API_KEY ||
  (typeof process !== "undefined"
    ? process.env.GEMINI_API_KEY || process.env.API_KEY
    : undefined);

// Only create the client when a key is available so the app doesn't crash.
const aiClient = apiKey ? new GoogleGenAI({ apiKey }) : null;

export const aiService = {
  analyzeIssue: async (title: string, description: string): Promise<string> => {
    if (!aiClient) {
      return "AI analysis disabled (no Gemini API key configured). Add VITE_GEMINI_API_KEY to .env.local to enable.";
    }

    try {
      const response = await aiClient.models.generateContent({
        model: "gemini-3-flash-preview",
        contents: `You are a QA Lead. Analyze this bug report and provide 3 concise bullet points: 1) Potential root cause, 2) Key verification step, 3) Severity assessment.\n\nBug: ${title}\nDetails: ${description}`,
        config: {
          temperature: 0.2, // Low temperature for more analytical/consistent results
        },
      });
      return response.text || "No analysis available.";
    } catch (error) {
      console.error("AI Analysis failed", error);
      return "Unable to analyze issue at this time. Please check your network or API key.";
    }
  },

  generateTests: async (appName: string, description: string): Promise<string[]> => {
      if (!aiClient) {
        // Return dummy data if AI is disabled, to allow testing the UI
        return ["Verify login functionality", "Check user profile update", "Test checkout flow (Mock)"];
      }

      try {
        const response = await aiClient.models.generateContent({
            model: "gemini-3-flash-preview",
            contents: `You are a QA Lead. Generate a checklist of 5-10 essential functional verification tests for a software project named "${appName}" with the following description/context: "${description}". Return ONLY the list of tests, one per line, without numbering or bullets.`,
            config: {
                temperature: 0.4,
            },
        });
        const text = response.text || "";
        return text.split('\n').map(t => t.replace(/^[-*•\d\.]+\s+/, '').trim()).filter(Boolean);
      } catch (error) {
          console.error("AI Test Generation failed", error);
          return [];
      }
  }
};
