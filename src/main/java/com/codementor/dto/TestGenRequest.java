package com.codementor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TestGenRequest(
        @NotBlank(message = "O nome da classe é obrigatório")
        @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_]*", message = "Nome de classe Java inválido")
        String className,

        @NotBlank(message = "O código não pode ser vazio")
        @Size(max = 8000, message = "O código excede o limite de 8000 caracteres")
        String code) {
}
