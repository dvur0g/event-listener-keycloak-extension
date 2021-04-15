package ru.event.listener.extension.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetEventRequest {

    String username;

}
