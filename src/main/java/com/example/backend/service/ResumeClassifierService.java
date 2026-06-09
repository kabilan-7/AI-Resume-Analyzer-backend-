package com.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.model.JobCriteria;
import com.example.backend.model.ScreeningResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeClassifierService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a senior HR recruiter and technical evaluator.
            Your job is to screen resumes objectively and return structured JSON only.
            Never include markdown fences, preamble, or explanation in your response.
            """;

    private static final String USER_PROMPT = """
            Evaluate the resume below against the job requirements.

            Job title: {jobTitle}
            Required skills: {requiredSkills}
            Minimum years of experience: {minExperience}

            Resume text:
            ---
            {resumeText}
            ---

            Return ONLY a JSON object with this exact structure:
            {
              "classification": "STRONG_FIT" | "POSSIBLE_FIT" | "NOT_FIT",
              "score": <integer 0-100>,
              "matchedSkills": ["skill1", "skill2"],
              "missingSkills": ["skill3"],
              "yearsExperience": <integer>,
              "summary": "<2-3 sentence assessment>"
            }

            Scoring guide:
            - STRONG_FIT  = score 75-100: meets all or nearly all requirements
            - POSSIBLE_FIT = score 40-74: meets some requirements, trainable gaps
            - NOT_FIT     = score 0-39:  significant mismatch in skills or experience
            """;

    public ScreeningResult classify(String resumeText, JobCriteria criteria) {
        String userMessage = USER_PROMPT
                .replace("{jobTitle}", criteria.jobTitle())
                .replace("{requiredSkills}", String.join(", ", criteria.requiredSkills()))
                .replace("{minExperience}", String.valueOf(criteria.minYearsExperience()))
                .replace("{resumeText}", resumeText);

        String raw = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        return parse(raw);
    }

    private ScreeningResult parse(String raw) {
        try {
            // Strip markdown fences if the model wraps anyway
            String clean = raw.replaceAll("(?s)```json\\s*|```", "").trim();
            return objectMapper.readValue(clean, ScreeningResult.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", raw, e);
            throw new IllegalStateException("AI returned unexpected format", e);
        }
    }
}
