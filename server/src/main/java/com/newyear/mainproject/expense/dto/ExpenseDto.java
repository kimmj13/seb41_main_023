package com.newyear.mainproject.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
@AllArgsConstructor
public class ExpenseDto {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Post {
        @NotBlank
        private String item;

        @NotNull @Positive
        private int price;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Patch {
        private long expenseId;

        @NotBlank
        private String item;

        @NotNull @Positive
        private int price;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private long expenseId;
        private String item;
        private int price;
    }

}
