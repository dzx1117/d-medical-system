package com.medical.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionDTO {

    @NotNull(message = "病历ID不能为空")
    private Long medicalRecordId;

    @NotEmpty(message = "请至少开一种药品")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "药品ID不能为空")
        private Long medicineId;

        private String dosage;

        private String frequency;

        private String duration;

        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
