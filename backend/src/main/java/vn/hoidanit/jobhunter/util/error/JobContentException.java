package vn.hoidanit.jobhunter.util.error;

import java.util.List;
import lombok.Getter;

@Getter
public class JobContentException extends Exception {
    private List<String> violatedWords;

    public JobContentException(String message, List<String> violatedWords) {
        super(message);
        this.violatedWords = violatedWords;
    }
}