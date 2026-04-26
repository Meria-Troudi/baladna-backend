package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.transport.dto.TransportAiDelayModelPredictionDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiDelayModelSummaryDTO;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportAiDelayModel;
import tn.esprit.spring.baladna.transport.entity.TransportAiTripDataset;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;
import tn.esprit.spring.baladna.transport.repository.TransportAiDelayModelRepository;
import tn.esprit.spring.baladna.transport.repository.TransportAiTripDatasetRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransportAiDelayModelService {

    private static final int MINIMUM_TRAINING_ROWS = 700;
    private static final double LEARNING_RATE = 0.03;
    private static final double REGULARIZATION = 0.001;
    private static final int FEATURE_COUNT = 10;

    private final TransportAiDelayModelRepository delayModelRepository;
    private final TransportAiTripDatasetRepository tripDatasetRepository;
    private final TransportAiDatasetService transportAiDatasetService;
    private final UserRepository userRepository;

    @Transactional
    public TransportAiDelayModelSummaryDTO trainModelForHost(String hostEmail) {
        if (hostEmail == null || hostEmail.isBlank()) {
            throw new RuntimeException("Host email is required to train the AI model.");
        }

        transportAiDatasetService.syncHostTripDatasets(hostEmail);

        User host = userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host not found."));
        TrainingContext trainingContext = buildTrainingContext(hostEmail);

        if (trainingContext.getEligibleSampleCount() == 0) {
            throw new RuntimeException("No dataset records are available yet for model training.");
        }

        if (!trainingContext.hasEnoughSamples()) {
            throw new RuntimeException(
                    "At least " + MINIMUM_TRAINING_ROWS + " dataset rows are required before training the AI model. "
                            + "Currently available: " + trainingContext.getEligibleSampleCount()
                            + " eligible rows (" + trainingContext.getRealSampleCount() + " real, "
                            + trainingContext.getBootstrapSampleCount() + " bootstrap-import)."
            );
        }

        return trainModel(host, trainingContext);
    }

    @Transactional
    public void maybeRefreshModelForHost(String hostEmail) {
        if (hostEmail == null || hostEmail.isBlank()) {
            return;
        }

        TrainingContext trainingContext = buildTrainingContext(hostEmail);
        Optional<TransportAiDelayModel> activeModel = delayModelRepository.findTopByHostEmailAndActiveTrueOrderByTrainedAtDesc(hostEmail);

        if (!shouldTrainModel(trainingContext, activeModel)) {
            return;
        }

        User host = userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host not found."));
        trainModel(host, trainingContext);
    }

    @Transactional(readOnly = true)
    public TransportAiDelayModelSummaryDTO getModelSummaryForHost(String hostEmail) {
        TrainingContext trainingContext = buildTrainingContext(hostEmail);
        Optional<TransportAiDelayModel> activeModel = delayModelRepository.findTopByHostEmailAndActiveTrueOrderByTrainedAtDesc(hostEmail);
        boolean refreshNeeded = shouldTrainModel(trainingContext, activeModel);

        if (activeModel.isPresent() && isQualifiedModel(activeModel.get())) {
            TransportAiDelayModelSummaryDTO summary = toSummary(activeModel.get());
            if (refreshNeeded) {
                summary.setNotes(appendRefreshPendingNote(summary.getNotes(), trainingContext));
            }
            return summary;
        }

        int trainingReadySamples = trainingContext.getEligibleSampleCount();
        int realTrainingReadySamples = trainingContext.getRealSampleCount();
        int bootstrapTrainingReadySamples = trainingContext.getBootstrapSampleCount();
        return TransportAiDelayModelSummaryDTO.builder()
                .trained(false)
                .sampleCount(trainingReadySamples)
                .predictionSource("RULE_BASED")
                .notes(buildUntrainedSummaryNotes(trainingContext, refreshNeeded))
                .build();
    }

    @Transactional(readOnly = true)
    public TransportAiDelayModelPredictionDTO predictDelayWithModel(Transport transport, int fallbackDelayMinutes) {
        if (transport == null || transport.getHost() == null || transport.getHost().getEmail() == null) {
            return null;
        }

        Optional<TransportAiDelayModel> activeModel = delayModelRepository
                .findTopByHostEmailAndActiveTrueOrderByTrainedAtDesc(transport.getHost().getEmail());

        if (activeModel.isEmpty()) {
            return null;
        }

        TransportAiDelayModel model = activeModel.get();
        if (!isQualifiedModel(model)) {
            return null;
        }

        double[] features = toFeatureVector(transport);
        double predicted = model.getInterceptWeight()
                + (model.getRainWeight() * features[0])
                + (model.getSandstormWeight() * features[1])
                + (model.getStormWeight() * features[2])
                + (model.getTrafficJamWeight() * features[3])
                + (model.getRushHourWeight() * features[4])
                + (model.getPeakWeekdayWeight() * features[5])
                + (model.getLongRouteWeight() * features[6])
                + (model.getHighOccupancyWeight() * features[7])
                + (model.getStrongWindWeight() * features[8])
                + (model.getHeavyPrecipitationWeight() * features[9]);

        int predictionMinutes = (int) Math.round(Math.max(fallbackDelayMinutes, predicted));
        predictionMinutes = Math.max(0, Math.min(120, predictionMinutes));

        return TransportAiDelayModelPredictionDTO.builder()
                .modelId(model.getId())
                .predictionMinutes(predictionMinutes)
                .sampleCount(model.getSampleCount())
                .meanAbsoluteError(model.getMeanAbsoluteError())
                .trainedAt(model.getTrainedAt())
                .build();
    }

    private TransportAiDelayModelSummaryDTO trainModel(User host, TrainingContext trainingContext) {
        List<TransportAiTripDataset> samples = trainingContext.getTrainingSamples();

        double intercept = samples.stream()
                .map(TransportAiTripDataset::getActualDelayMinutes)
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);
        double[] weights = new double[FEATURE_COUNT];

        int iterations = Math.max(800, Math.min(5000, samples.size() * 140));
        for (int iteration = 0; iteration < iterations; iteration++) {
            double gradientIntercept = 0.0;
            double[] gradientWeights = new double[FEATURE_COUNT];

            for (TransportAiTripDataset sample : samples) {
                double[] features = toFeatureVector(sample);
                double target = sample.getActualDelayMinutes();
                double prediction = intercept + dot(weights, features);
                double error = prediction - target;

                gradientIntercept += error;
                for (int index = 0; index < FEATURE_COUNT; index++) {
                    gradientWeights[index] += error * features[index];
                }
            }

            double sampleCount = samples.size();
            intercept -= LEARNING_RATE * (gradientIntercept / sampleCount);
            for (int index = 0; index < FEATURE_COUNT; index++) {
                double gradient = (gradientWeights[index] / sampleCount) + (REGULARIZATION * weights[index]);
                weights[index] -= LEARNING_RATE * gradient;
            }
        }

        double mae = 0.0;
        double mse = 0.0;
        for (TransportAiTripDataset sample : samples) {
            double prediction = intercept + dot(weights, toFeatureVector(sample));
            double error = prediction - sample.getActualDelayMinutes();
            mae += Math.abs(error);
            mse += error * error;
        }
        mae /= samples.size();
        mse /= samples.size();
        double rmse = Math.sqrt(mse);

        delayModelRepository.deactivateActiveModels(host.getEmail());

        TransportAiDelayModel model = TransportAiDelayModel.builder()
                .host(host)
                .sampleCount(samples.size())
                .interceptWeight(roundDouble(intercept))
                .rainWeight(roundDouble(weights[0]))
                .sandstormWeight(roundDouble(weights[1]))
                .stormWeight(roundDouble(weights[2]))
                .trafficJamWeight(roundDouble(weights[3]))
                .rushHourWeight(roundDouble(weights[4]))
                .peakWeekdayWeight(roundDouble(weights[5]))
                .longRouteWeight(roundDouble(weights[6]))
                .highOccupancyWeight(roundDouble(weights[7]))
                .strongWindWeight(roundDouble(weights[8]))
                .heavyPrecipitationWeight(roundDouble(weights[9]))
                .meanAbsoluteError(roundDouble(mae))
                .rootMeanSquaredError(roundDouble(rmse))
                .trainedAt(LocalDateTime.now())
                .active(true)
                .trainingDataMode(trainingContext.getTrainingDataMode())
                .realSampleCount(trainingContext.getRealSampleCount())
                .bootstrapSampleCount(trainingContext.getBootstrapSampleCount())
                .datasetFingerprint(trainingContext.getDatasetFingerprint())
                .notes(buildTrainingNotes(
                        samples.size(),
                        mae,
                        trainingContext.isUseRealOnlySamples(),
                        trainingContext.getRealSampleCount(),
                        trainingContext.getBootstrapSampleCount()
                ))
                .build();

        TransportAiDelayModel saved = delayModelRepository.save(model);
        return toSummary(saved);
    }

    private TrainingContext buildTrainingContext(String hostEmail) {
        List<TransportAiTripDataset> eligibleSamples = tripDatasetRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail).stream()
                .filter(sample -> sample.getActualDelayMinutes() != null)
                .toList();
        List<TransportAiTripDataset> realSamples = tripDatasetRepository
                .findByHostEmailAndDataOriginAndActualDelayMinutesIsNotNullOrderByDepartureDateDesc(
                        hostEmail,
                        TransportAiDatasetService.DATA_ORIGIN_REAL
                );
        int bootstrapSampleCount = (int) tripDatasetRepository.countByHostEmailAndDataOriginAndActualDelayMinutesIsNotNull(
                hostEmail,
                TransportAiDatasetService.DATA_ORIGIN_BOOTSTRAP_IMPORT
        );
        boolean useRealOnlySamples = realSamples.size() >= MINIMUM_TRAINING_ROWS;
        List<TransportAiTripDataset> trainingSamples = useRealOnlySamples ? realSamples : eligibleSamples;
        String trainingDataMode = useRealOnlySamples ? "REAL_ONLY" : "MIXED_WITH_BOOTSTRAP";

        return new TrainingContext(
                eligibleSamples,
                realSamples.size(),
                bootstrapSampleCount,
                useRealOnlySamples,
                trainingSamples,
                trainingDataMode,
                buildDatasetFingerprint(trainingSamples, trainingDataMode, realSamples.size(), bootstrapSampleCount)
        );
    }

    private boolean shouldTrainModel(TrainingContext trainingContext, Optional<TransportAiDelayModel> activeModel) {
        if (!trainingContext.hasEnoughSamples()) {
            return false;
        }

        if (activeModel.isEmpty() || !isQualifiedModel(activeModel.get())) {
            return true;
        }

        TransportAiDelayModel currentModel = activeModel.get();
        if (!Objects.equals(currentModel.getSampleCount(), trainingContext.getTrainingSampleCount())) {
            return true;
        }
        if (!Objects.equals(currentModel.getTrainingDataMode(), trainingContext.getTrainingDataMode())) {
            return true;
        }
        if (!Objects.equals(currentModel.getRealSampleCount(), trainingContext.getRealSampleCount())) {
            return true;
        }
        if (!Objects.equals(currentModel.getBootstrapSampleCount(), trainingContext.getBootstrapSampleCount())) {
            return true;
        }
        return !Objects.equals(currentModel.getDatasetFingerprint(), trainingContext.getDatasetFingerprint());
    }

    private TransportAiDelayModelSummaryDTO toSummary(TransportAiDelayModel model) {
        return TransportAiDelayModelSummaryDTO.builder()
                .modelId(model.getId())
                .trained(true)
                .sampleCount(model.getSampleCount())
                .meanAbsoluteError(model.getMeanAbsoluteError())
                .rootMeanSquaredError(model.getRootMeanSquaredError())
                .trainedAt(model.getTrainedAt())
                .predictionSource("TRAINED_MODEL")
                .notes(model.getNotes())
                .build();
    }

    private boolean isQualifiedModel(TransportAiDelayModel model) {
        return model != null
                && Boolean.TRUE.equals(model.getActive())
                && model.getSampleCount() != null
                && model.getSampleCount() >= MINIMUM_TRAINING_ROWS;
    }

    private double[] toFeatureVector(TransportAiTripDataset sample) {
        double[] features = new double[FEATURE_COUNT];
        features[0] = equalsValue(sample.getWeather(), "RAIN");
        features[1] = equalsValue(sample.getWeather(), "SANDSTORM");
        features[2] = equalsValue(sample.getWeather(), "STORM");
        features[3] = Boolean.TRUE.equals(sample.getTrafficJam()) ? 1.0 : 0.0;
        features[4] = isRushHour(sample.getDepartureHour()) ? 1.0 : 0.0;
        features[5] = isPeakWeekday(sample.getDepartureDayOfWeek()) ? 1.0 : 0.0;
        features[6] = sample.getDistanceKm() != null && sample.getDistanceKm() >= 120 ? 1.0 : 0.0;
        features[7] = sample.getOccupancyRate() != null && sample.getOccupancyRate() >= 85 ? 1.0 : 0.0;
        features[8] = sample.getWeatherWindSpeed() != null && sample.getWeatherWindSpeed() >= 60 ? 1.0 : 0.0;
        features[9] = sample.getWeatherPrecipitation() != null && sample.getWeatherPrecipitation() >= 10 ? 1.0 : 0.0;
        return features;
    }

    private double[] toFeatureVector(Transport transport) {
        double[] features = new double[FEATURE_COUNT];
        features[0] = transport.getWeather() == WeatherCondition.RAIN ? 1.0 : 0.0;
        features[1] = transport.getWeather() == WeatherCondition.SANDSTORM ? 1.0 : 0.0;
        features[2] = transport.getWeather() == WeatherCondition.STORM ? 1.0 : 0.0;
        features[3] = Boolean.TRUE.equals(transport.getTrafficJam()) ? 1.0 : 0.0;
        features[4] = transport.getDepartureDate() != null && isRushHour(transport.getDepartureDate().getHour()) ? 1.0 : 0.0;
        features[5] = transport.getDepartureDate() != null && isPeakWeekday(transport.getDepartureDate().getDayOfWeek().name()) ? 1.0 : 0.0;
        features[6] = transport.getTrajet() != null && transport.getTrajet().getDistanceKm() != null && transport.getTrajet().getDistanceKm() >= 120 ? 1.0 : 0.0;
        features[7] = getOccupancyRate(transport) >= 85 ? 1.0 : 0.0;
        features[8] = transport.getWeatherWindSpeed() != null && transport.getWeatherWindSpeed() >= 60 ? 1.0 : 0.0;
        features[9] = transport.getWeatherPrecipitation() != null && transport.getWeatherPrecipitation() >= 10 ? 1.0 : 0.0;
        return features;
    }

    private double getOccupancyRate(Transport transport) {
        if (transport.getTotalCapacity() == null || transport.getTotalCapacity() <= 0 || transport.getAvailableSeats() == null) {
            return 0.0;
        }
        int occupiedSeats = Math.max(0, transport.getTotalCapacity() - transport.getAvailableSeats());
        return (occupiedSeats / (double) transport.getTotalCapacity()) * 100.0;
    }

    private boolean isRushHour(Integer departureHour) {
        if (departureHour == null) {
            return false;
        }
        return (departureHour >= 7 && departureHour <= 9) || (departureHour >= 16 && departureHour <= 19);
    }

    private boolean isPeakWeekday(String dayValue) {
        if (dayValue == null || dayValue.isBlank()) {
            return false;
        }
        try {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayValue);
            return dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.FRIDAY;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private double equalsValue(String actual, String expected) {
        return expected.equalsIgnoreCase(actual != null ? actual : "") ? 1.0 : 0.0;
    }

    private double dot(double[] weights, double[] features) {
        double value = 0.0;
        for (int index = 0; index < FEATURE_COUNT; index++) {
            value += weights[index] * features[index];
        }
        return value;
    }

    private double roundDouble(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String buildTrainingNotes(
            int sampleCount,
            double mae,
            boolean usedOnlyRealHistory,
            int realSampleCount,
            int bootstrapSampleCount
    ) {
        String datasetSourceNote = usedOnlyRealHistory
                ? "Model trained on fully real host transport history (" + realSampleCount + " rows). "
                : "Model trained on a mixed host dataset (" + realSampleCount + " real rows, " + bootstrapSampleCount + " bootstrap-import rows, " + sampleCount + " rows used). ";

        if (mae <= 5) {
            return datasetSourceNote + "Current performance looks strong.";
        }
        if (mae <= 12) {
            return datasetSourceNote + "More diverse completed trips can still improve forecasts.";
        }
        return datasetSourceNote + "Prediction error is still high. Collect more completed trips with actual delay values to improve it.";
    }

    private String buildUntrainedSummaryNotes(TrainingContext trainingContext, boolean refreshNeeded) {
        int trainingReadySamples = trainingContext.getEligibleSampleCount();
        int realTrainingReadySamples = trainingContext.getRealSampleCount();
        int bootstrapTrainingReadySamples = trainingContext.getBootstrapSampleCount();

        if (trainingReadySamples <= 0) {
            return "No training-ready dataset rows yet. Record the actual delay on completed transports to build a real training dataset of at least "
                    + MINIMUM_TRAINING_ROWS
                    + " rows. Once enough rows exist, the model will refresh automatically after transport updates.";
        }

        String note = "No trained model yet. Training-ready rows: "
                + realTrainingReadySamples
                + " real, "
                + bootstrapTrainingReadySamples
                + " bootstrap-import, "
                + trainingReadySamples
                + " total. Model training requires at least "
                + MINIMUM_TRAINING_ROWS
                + " eligible rows and will automatically prefer fully real rows once at least "
                + MINIMUM_TRAINING_ROWS
                + " real rows exist. Remaining rows: "
                + Math.max(0, MINIMUM_TRAINING_ROWS - trainingReadySamples)
                + ".";

        return refreshNeeded
                ? note + " The dataset is ready, and the next write-driven refresh or back-office training run will produce a new model."
                : note;
    }

    private String appendRefreshPendingNote(String existingNote, TrainingContext trainingContext) {
        String prefix = existingNote == null || existingNote.isBlank() ? "" : existingNote + " ";
        return prefix
                + "A newer "
                + trainingContext.getTrainingDataMode().toLowerCase().replace('_', '-')
                + " dataset snapshot is available and the model will refresh on the next write-driven update or manual back-office training run.";
    }

    private String buildDatasetFingerprint(
            List<TransportAiTripDataset> trainingSamples,
            String trainingDataMode,
            int realSampleCount,
            int bootstrapSampleCount
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, trainingDataMode);
            updateDigest(digest, Integer.toString(trainingSamples.size()));
            updateDigest(digest, Integer.toString(realSampleCount));
            updateDigest(digest, Integer.toString(bootstrapSampleCount));

            trainingSamples.stream()
                    .sorted(Comparator.comparing(TransportAiTripDataset::getId, Comparator.nullsLast(Long::compareTo)))
                    .forEach(sample -> {
                        updateDigest(digest, sample.getId());
                        updateDigest(digest, sample.getDataOrigin());
                        updateDigest(digest, sample.getActualDelayMinutes());
                        updateDigest(digest, sample.getWeather());
                        updateDigest(digest, sample.getTrafficJam());
                        updateDigest(digest, sample.getDepartureHour());
                        updateDigest(digest, sample.getDepartureDayOfWeek());
                        updateDigest(digest, sample.getDistanceKm());
                        updateDigest(digest, sample.getOccupancyRate());
                        updateDigest(digest, sample.getWeatherWindSpeed());
                        updateDigest(digest, sample.getWeatherPrecipitation());
                        updateDigest(digest, sample.getUpdatedAt());
                    });

            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to build the AI dataset fingerprint.", exception);
        }
    }

    private void updateDigest(MessageDigest digest, Object value) {
        String normalized = value == null ? "null" : value.toString();
        digest.update(normalized.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '|');
    }

    private static final class TrainingContext {
        private final List<TransportAiTripDataset> eligibleSamples;
        private final int realSampleCount;
        private final int bootstrapSampleCount;
        private final boolean useRealOnlySamples;
        private final List<TransportAiTripDataset> trainingSamples;
        private final String trainingDataMode;
        private final String datasetFingerprint;

        private TrainingContext(
                List<TransportAiTripDataset> eligibleSamples,
                int realSampleCount,
                int bootstrapSampleCount,
                boolean useRealOnlySamples,
                List<TransportAiTripDataset> trainingSamples,
                String trainingDataMode,
                String datasetFingerprint
        ) {
            this.eligibleSamples = eligibleSamples;
            this.realSampleCount = realSampleCount;
            this.bootstrapSampleCount = bootstrapSampleCount;
            this.useRealOnlySamples = useRealOnlySamples;
            this.trainingSamples = trainingSamples;
            this.trainingDataMode = trainingDataMode;
            this.datasetFingerprint = datasetFingerprint;
        }

        private int getEligibleSampleCount() {
            return eligibleSamples.size();
        }

        private int getRealSampleCount() {
            return realSampleCount;
        }

        private int getBootstrapSampleCount() {
            return bootstrapSampleCount;
        }

        private boolean isUseRealOnlySamples() {
            return useRealOnlySamples;
        }

        private List<TransportAiTripDataset> getTrainingSamples() {
            return trainingSamples;
        }

        private int getTrainingSampleCount() {
            return trainingSamples.size();
        }

        private String getTrainingDataMode() {
            return trainingDataMode;
        }

        private String getDatasetFingerprint() {
            return datasetFingerprint;
        }

        private boolean hasEnoughSamples() {
            return getTrainingSampleCount() >= MINIMUM_TRAINING_ROWS;
        }
    }
}
