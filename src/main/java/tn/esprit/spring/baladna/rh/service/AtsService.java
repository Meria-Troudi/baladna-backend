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

        // ✅ 1. Compétences requises par le poste (40 pts)
        score += analyzeRequiredSkills(cvLower, interview, feedback);

        // ✅ 2. Expérience (20 pts)
        score += analyzeExperience(cvLower, interview, feedback);

        // ✅ 3. Compétences métier BALADNA/Tourisme (25 pts)
        score += analyzeBaladnaSkills(cvLower, feedback);

        // ✅ 4. Structure du CV (15 pts)
        score += analyzeStructure(cvLower, feedback);

        score = Math.min(score, 100);

        return AtsResult.builder()
                .score(score)
                .feedback(String.join("\n", feedback))
                .accepted(score >= 60)
                .build();
    }

    // ===== 1. Compétences requises par le poste =====
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
            feedback.add("✅ Toutes les compétences requises sont présentes (" + matched + "/" + required.length + ")");
        } else if (matched > 0) {
            feedback.add("⚠️ Compétences partielles : " + matched + "/" + required.length + " trouvées");
        } else {
            feedback.add("❌ Aucune compétence requise trouvée");
        }

        return skillScore;
    }

    // ===== 2. Expérience =====
    private int analyzeExperience(
            String cvLower, Interview interview, List<String> feedback) {

        if (interview.getExperienceYears() == null) return 10;

        int yearsFound = extractYearsOfExperience(cvLower);

        if (yearsFound >= interview.getExperienceYears()) {
            feedback.add("✅ Expérience suffisante : " + yearsFound + " ans");
            return 20;
        } else if (yearsFound > 0) {
            feedback.add("⚠️ Expérience insuffisante : " + yearsFound + " ans (requis : " + interview.getExperienceYears() + " ans)");
            return 8;
        } else {
            feedback.add("❌ Aucune expérience détectée");
            return 0;
        }
    }

    // ===== 3. Compétences métier BALADNA =====
    private int analyzeBaladnaSkills(String cvLower, List<String> feedback) {
        int score = 0;

        // 🏨 Hospitalité & Accueil (8 pts)
        List<String> hospitalityKeywords = Arrays.asList(
                "accueil", "hospitalité", "hospitality", "réception", "reception",
                "hôtellerie", "hotellerie", "hotel", "hôtel", "service client",
                "customer service", "guest", "hébergement", "accommodation"
        );
        int hospitalityScore = countKeywords(cvLower, hospitalityKeywords);
        if (hospitalityScore >= 3) {
            feedback.add("✅ Excellentes compétences en hospitalité");
            score += 8;
        } else if (hospitalityScore >= 1) {
            feedback.add("⚠️ Quelques compétences en hospitalité");
            score += 4;
        }

        // 🗺️ Tourisme & Voyage (8 pts)
        List<String> tourismKeywords = Arrays.asList(
                "tourisme", "tourism", "guide", "voyage", "travel", "excursion",
                "circuit", "itinéraire", "itinerary", "destinations", "patrimoine",
                "heritage", "culture", "culturel", "cultural", "site touristique",
                "monument", "musée", "museum", "découverte", "discovery"
        );
        int tourismScore = countKeywords(cvLower, tourismKeywords);
        if (tourismScore >= 3) {
            feedback.add("✅ Excellent profil tourisme/voyage");
            score += 8;
        } else if (tourismScore >= 1) {
            feedback.add("⚠️ Quelques expériences en tourisme");
            score += 4;
        }

        // 🎨 Artisanat & Culture tunisienne (5 pts)
        List<String> artisanKeywords = Arrays.asList(
                "artisanat", "artisan", "crafts", "craft", "poterie", "pottery",
                "tissage", "weaving", "broderie", "embroidery", "zellige",
                "medina", "souk", "tunisie", "tunisia", "tunisien", "tunisian",
                "berbère", "berber", "tradition", "traditionnel", "traditional",
                "authentique", "authentic", "local", "communauté", "community"
        );
        int artisanScore = countKeywords(cvLower, artisanKeywords);
        if (artisanScore >= 2) {
            feedback.add("✅ Connaissance de l'artisanat et culture tunisienne");
            score += 5;
        } else if (artisanScore >= 1) {
            feedback.add("⚠️ Quelques connaissances culturelles");
            score += 2;
        }

        // 🌍 Langues (4 pts)
        List<String> languageKeywords = Arrays.asList(
                "arabe", "arabic", "français", "french", "anglais", "english",
                "bilingue", "bilingual", "multilingue", "multilingual",
                "allemand", "german", "espagnol", "spanish", "italien", "italian"
        );
        int langScore = countKeywords(cvLower, languageKeywords);
        if (langScore >= 3) {
            feedback.add("✅ Profil multilingue — atout majeur pour le tourisme");
            score += 4;
        } else if (langScore >= 1) {
            feedback.add("⚠️ Compétences linguistiques partielles");
            score += 2;
        }

        return score;
    }

    // ===== 4. Structure du CV =====
    private int analyzeStructure(String cvLower, List<String> feedback) {
        int score = 0;

        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("expérience|experience|travail|work|emploi|employment|poste|position", "Expérience professionnelle");
        sections.put("formation|education|diplôme|diploma|étude|study|université|university|école|school", "Formation");
        sections.put("compétence|skill|savoir|know|capacité|ability|maîtrise|expertise", "Compétences");
        sections.put("contact|email|téléphone|phone|tel|adresse|address", "Coordonnées");
        sections.put("langue|language|bilingue|francais|arabic|anglais", "Langues");

        for (Map.Entry<String, String> entry : sections.entrySet()) {
            boolean found = Arrays.stream(entry.getKey().split("\\|"))
                    .anyMatch(cvLower::contains);
            if (found) {
                score += 3;
            } else {
                feedback.add("⚠️ Section manquante : " + entry.getValue());
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
                "(\\d+)\\s*ans?\\s*d.expérience",
                "(\\d+)\\s*years?\\s*of\\s*experience",
                "(\\d+)\\s*années?\\s*d.expérience",
                "(\\d+)\\s*ans?",
                "(\\d+)\\s*years?"
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