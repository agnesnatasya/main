package seedu.address.model.hirelah.storage;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataConversionException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.FileUtil;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.hirelah.MetricList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class MetricStorage {
    private final Path path;
    private static final Logger logger = LogsCenter.getLogger(MetricStorage.class);

    public MetricStorage(Path newPath) {
        this.path = newPath;
    }

    public Path getPath() {
        return this.path;
    }

    /**
     * reads the data from the current Path to
     * retrieve all the information regarding Metric.
     * @return Optional<MetricList>
     * @throws DataConversionException
     */
    public Optional<MetricList> readMetric(Path filePath) throws DataConversionException {
        requireNonNull(filePath);
        Optional<JsonSerializableMetric> jsonMetric = JsonUtil.readJsonFile(
                filePath, JsonSerializableMetric.class);
        if (jsonMetric.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonMetric.get().toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataConversionException(ive);
        }
    }

    /**
     * Save the information of the Metric
     * @param  source of the data. Cannot be null.
     */
    public void saveMetrics(MetricList source) throws IOException, IllegalValueException {
        requireNonNull(source);
        requireNonNull(path);
        FileUtil.createIfMissing(path);
        JsonUtil.saveJsonFile(new JsonSerializableMetric(source), path);
    }
}
