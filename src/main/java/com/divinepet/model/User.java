package com.divinepet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Array;

@AllArgsConstructor
@Getter
public class User {
    private String id;
    private String username;
    private String firstname;
    private String lastname;
    private Array blocked;
}
