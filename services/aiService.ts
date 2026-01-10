import { GoogleGenAI } from "@google/genai";

// Initialize the Google GenAI client
// Using process.env.API_KEY as strictly required. 
// Note: In a real production app, ensure this key is restricted or proxied if used on the client side.
const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });

export const aiService = {
  analyzeIssue: async (title: string, description: string): Promise<string> => {
    try {
      const response = await ai.models.generateContent({
        model: 'gemini-3-flash-preview',
        contents: `You are a QA Lead. Analyze this bug report and provide 3 concise bullet points: 1) Potential root cause, 2) Key verification step, 3) Severity assessment.\n\nBug: ${title}\nDetails: ${description}`,
        config: {
            temperature: 0.2, // Low temperature for more analytical/consistent results
        }
      });
      return response.text || "No analysis available.";
    } catch (error) {
      console.error("AI Analysis failed", error);
      return "Unable to analyze issue at this time. Please check your network or API key.";
    }
  }
};