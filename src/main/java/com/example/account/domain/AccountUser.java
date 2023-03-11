package com.example.account.domain;

import lombok.*;

import javax.persistence.Entity;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class AccountUser extends BaseEntity {
    private String name;
}
