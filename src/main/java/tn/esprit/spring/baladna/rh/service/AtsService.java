package tn.esprit.spring.baladna.rh.service;

import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.rh.dto.AtsResult;
import tn.esprit.spring.baladna.rh.entity.Interview;

import java.util.*;

@Service
public class AtsService {

    public AtsResult analyzeCV(String cvText, Interview interview) {
        int score = 0;
        List<String> feedback = new ArrayList<>();
        String cvLower = cvText.toLowerCase();

        score += analyzeRequiredSkills(cvLower, interview, feedback);
        score += analyzeExperience(cvLower, interview, feedback);
        score += analyzeBaladnaSkills(cvLower, feedback);
        score += analyzeStructure(cvLower, feedback);

        score = Math.min(score, 100);

        return AtsResult.builder()
                .score(score)
                .feedback(String.join("\n", feedback))
                .accepted(score >= 60)
                .build();
    }

    // ===== 1. Required Skills (40 pts) =====
    private int analyzeRequiredSkills(
            String cvLower, Interview interview, List<String> feedback) {

        if (interview.getRequiredSkills() == null) return 0;

        String[] required = interview.getRequiredSkills().toLowerCase().split(",");
        int matched = 0;

        for (String skill : required) {
            if (cvLower.contains(skill.trim())) matched++;
        }

        int skillScore = (int) ((double) matched / required.length * 40);

        if (matched == required.length) {
            feedback.add("✅ All required skills found (" + matched + "/" + required.length + ")");
        } else if (matched > 0) {
            feedback.add("⚠️ Partial skills match: " + matched + "/" + required.length + " found");
        } else {
            feedback.add("❌ No required skills found in CV");
        }

        return skillScore;
    }

    // ===== 2. Experience (20 pts) =====
    private int analyzeExperience(
            String cvLower, Interview interview, List<String> feedback) {

        if (interview.getExperienceYears() == null) return 10;

        int yearsFound = extractYearsOfExperience(cvLower);

        if (yearsFound >= interview.getExperienceYears()) {
            feedback.add("✅ Sufficient experience: " + yearsFound + " year(s)");
            return 20;
        } else if (yearsFound > 0) {
            feedback.add("⚠️ Insufficient experience: " + yearsFound +
                    " year(s) found (required: " + interview.getExperienceYears() + ")");
            return 8;
        } else {
            feedback.add("❌ No experience detected in CV");
            return 0;
        }
    }

    // ===== 3. BALADNA Tourism Skills (25 pts) =====
    private int analyzeBaladnaSkills(String cvLower, List<String> feedback) {
        int score = 0;

        // 🏨 Hospitality & Reception (8 pts)
        List<String> hospitalityKeywords = Arrays.asList(
                "hospitality", "reception", "hotel", "accommodation", "hosting",
                "guest", "customer service", "front desk", "concierge",
                "check-in", "check-out", "resort", "lodge", "inn"
        );
        int hospitalityScore = countKeywords(cvLower, hospitalityKeywords);
        if (hospitalityScore >= 3) {
            feedback.add("✅ Strong hospitality and reception skills");
            score += 8;
        } else if (hospitalityScore >= 1) {
            feedback.add("⚠️ Some hospitality experience detected");
            score += 4;
        }

        // 🗺️ Tourism & Travel (8 pts)
        List<String> tourismKeywords = Arrays.asList(
                "tourism", "tourist", "guide", "travel", "tour", "excursion",
                "itinerary", "sightseeing", "heritage", "cultural", "culture",
                "monument", "museum", "historical", "archaeological", "site",
                "destination", "landmark", "exploration", "discovery"
        );
        int tourismScore = countKeywords(cvLower, tourismKeywords);
        if (tourismScore >= 3) {
            feedback.add("✅ Strong tourism and travel profile");
            score += 8;
        } else if (tourismScore >= 1) {
            feedback.add("⚠️ Some tourism experience detected");
            score += 4;
        }

        // 🎨 Tunisian Crafts & Culture (5 pts)
        List<String> artisanKeywords = Arrays.asList(
                "craft", "crafts", "artisan", "handmade", "pottery", "ceramics",
                "weaving", "embroidery", "zellige", "medina", "souk", "bazaar",
                "traditional", "authentic", "local", "heritage", "tunisia",
                "tunisian", "berber", "arabic", "community", "cultural exchange"
        );
        int artisanScore = countKeywords(cvLower, artisanKeywords);
        if (artisanScore >= 2) {
            feedback.add("✅ Knowledge of Tunisian crafts and culture");
            score += 5;
        } else if (artisanScore >= 1) {
            feedback.add("⚠️ Some cultural knowledge detected");
            score += 2;
        }

        // 🌍 Languages (4 pts)
        List<String> languageKeywords = Arrays.asList(
                "arabic", "french", "english", "german", "spanish", "italian",
                "bilingual", "multilingual", "fluent", "conversational",
                "native speaker", "language skills", "translation"
        );
        int langScore = countKeywords(cvLower, languageKeywords);
        if (langScore >= 3) {
            feedback.add("✅ Multilingual profile — major asset for tourism");
            score += 4;
        } else if (langScore >= 1) {
            feedback.add("⚠️ Some language skills detected");
            score += 2;
        }

        return score;
    }

    // ===== 4. CV Structure (15 pts) =====
    private int analyzeStructure(String cvLower, List<String> feedback) {
        int score = 0;

        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("experience|work|employment|position|job|career|professional", "Work Experience");
        sections.put("education|degree|diploma|university|college|school|study|formation", "Education");
        sections.put("skill|skills|competency|expertise|knowledge|ability|proficiency", "Skills");
        sections.put("contact|email|phone|mobile|address|tel", "Contact Information");
        sections.put("language|languages|fluent|bilingual|multilingual", "Languages");

        for (Map.Entry<String, String> entry : sections.entrySet()) {
            boolean found = Arrays.stream(entry.getKey().split("\\|"))
                    .anyMatch(cvLower::contains);
            if (found) {
                score += 3;
            } else {
                feedback.add("⚠️ Missing section: " + entry.getValue());
            }
        }

        return score;
    }

    // ===== Helpers =====
    private int countKeywords(String cvLower, List<String> keywords) {
        return (int) keywords.stream().filter(cvLower::contains).count();
    }

    private int extractYearsOfExperience(String cvText) {
        String[] patterns = {
                "(\\d+)\\s*years?\\s*of\\s*experience",
                "(\\d+)\\s*years?\\s*experience",
                "(\\d+)\\+?\\s*years?",
                "(\\d+)\\s*yrs?"
        };

        for (String pattern : patterns) {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile(pattern,
                                    java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(cvText);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return 0;
    }
}