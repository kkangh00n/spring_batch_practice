package com.example.killBatch.jobParameter;

import java.util.Objects;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

@Component
public class SystemDestructionValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        if (Objects.isNull(parameters)) {
            throw new JobParametersInvalidException("파라미터가 NULL입니다");
        }

        String target = parameters.getString("system.target");
        if (target == null) {
            throw new JobParametersInvalidException("system.target 파라미터는 필수값입니다");
        }
    }
}
