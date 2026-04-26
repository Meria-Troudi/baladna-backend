package tn.esprit.spring.baladna.rh.service;

import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.rh.dto.AtsResult;
import tn.esprit.spring.baladna.rh.entity.Interview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private int analyzeRequiredSkills(String cvLower, Interview interview, List<String> feedback) {
        if (interview.getRequiredSkills() == null) {
            return 0;
        }

        String[] required = interview.getRequiredSkills().toLowerCase().split(",");
        int matched = 0;

        for (String skill : required) {
            if (cvLower.contains(skill.trim())) {
                matched++;
            }
        }

        int skillScore = (int) ((double) matched / required.length * 40);

        if (matched == required.length) {
            feedback.add("All required skills were found (" + matched + "/" + required.length + ").");
        } else if (matched > 0) {
            feedback.add("Partial required skills match: " + matched + "/" + required.length + " found.");
        } else {
            feedback.add("No required skill was found.");
        }

        return skillScore;
    }

    private int analyzeExperience(String cvLower, Interview interview, List<String> feedback) {
        if (interview.getExperienceYears() == null) {
            return 10;
        }

        int yearsFound = extractYearsOfExperience(cvLower);

        if (yearsFound >= interview.getExperienceYears()) {
            feedback.add("Experience meets the requirement: " + yearsFound + " years.");
            return 20;
        } else if (yearsFound > 0) {
            feedback.add("Experience below requirement: " + yearsFound + " years (required: "
                    + interview.getExperienceYears() + " years).");
            return 8;
        } else {
            feedback.add("No experience was detected.");
            return 0;
        }
    }

    private int analyzeBaladnaSkills(String cvLower, List<String> feedback) {
        int score = 0;

        List<String> hospitalityKeywords = Arrays.asList(
                "accueil", "hospitalite", "hospitality", "reception",
                "hotellerie", "hotel", "service client", "customer service",
                "guest", "hebergement", "accommodation"
        );
        int hospitalityScore = countKeywords(cvLower, hospitalityKeywords);
        if (hospitalityScore >= 3) {
            feedback.add("Strong hospitality skills detected.");
            score += 8;
        } else if (hospitalityScore >= 1) {
            feedback.add("Some hospitality skills detected.");
            score += 4;
        }

        List<String> tourismKeywords = Arrays.asList(
                "tourisme", "tourism", "guide", "voyage", "travel", "excursion",
                "circuit", "itineraire", "itinerary", "destinations", "patrimoine",
                "heritage", "culture", "culturel", "cultural", "site touristique",
                "monument", "musee", "museum", "decouverte", "discovery"
        );
        int tourismScore = countKeywords(cvLower, tourismKeywords);
        if (tourismScore >= 3) {
            feedback.add("Strong tourism and travel profile detected.");
            score += 8;
        } else if (tourismScore >= 1) {
            feedback.add("Some tourism experience detected.");
            score += 4;
        }

        List<String> artisanKeywords = Arrays.asList(
                "artisanat", "artisan", "crafts", "craft", "poterie", "pottery",
                "tissage", "weaving", "broderie", "embroidery", "zellige",
                "medina", "souk", "tunisie", "tunisia", "tunisien", "tunisian",
                "berbere", "berber", "tradition", "traditionnel", "traditional",
                "authentique", "authentic", "local", "communaute", "community"
        );
        int artisanScore = countKeywords(cvLower, artisanKeywords);
        if (artisanScore >= 2) {
            feedback.add("Knowledge of Tunisian craft and culture detected.");
            score += 5;
        } else if (artisanScore >= 1) {
            feedback.add("Some cultural knowledge detected.");
            score += 2;
        }

        List<String> languageKeywords = Arrays.asList(
                "arabe", "arabic", "francais", "french", "anglais", "english",
                "bilingue", "bilingual", "multilingue", "multilingual",
                "allemand", "german", "espagnol", "spanish", "italien", "italian"
        );
        int langScore = countKeywords(cvLower, languageKeywords);
        if (langScore >= 3) {
            feedback.add("Multilingual profile detected, which is valuable for tourism.");
            score += 4;
        } else if (langScore >= 1) {
            feedback.add("Partial language skills detected.");
            score += 2;
        }

        return score;
    }

    private int analyzeStructure(String cvLower, List<String> feedback) {
        int score = 0;

        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("experience|travail|work|emploi|employment|poste|position", "Professional experience");
        sections.put("formation|education|diplome|diploma|etude|study|universite|university|ecole|school", "Education");
        sections.put("competence|skill|savoir|know|capacite|ability|maitrise|expertise", "Skills");
        sections.put("contact|email|telephone|phone|tel|adresse|address", "Contact details");
        sections.put("langue|language|bilingue|francais|arabic|anglais", "Languages");

        for (Map.Entry<String, String> entry : sections.entrySet()) {
            boolean found = Arrays.stream(entry.getKey().split("\\|"))
                    .anyMatch(cvLower::contains);
            if (found) {
                score += 3;
            } else {
                feedback.add("Missing section: " + entry.getValue());
            }
        }

        return score;
    }

    private int countKeywords(String cvLower, List<String> keywords) {
        return (int) keywords.stream().filter(cvLower::contains).count();
    }

    private int extractYearsOfExperience(String cvText) {
        String[] patterns = {
                "(\\d+)\\s*ans?\\s*d.experience",
                "(\\d+)\\s*years?\\s*of\\s*experience",
                "(\\d+)\\s*annees?\\s*d.experience",
                "(\\d+)\\s*ans?",
                "(\\d+)\\s*years?"
        };

        for (String pattern : patterns) {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(cvText);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return 0;
    }
}
