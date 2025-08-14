package wikalloy;

import java.io.Serializable;

public record MeasurementResult(String name, long createTime, double insertTime, double selectTime, double space) implements Serializable {
    /* no-op */
}
