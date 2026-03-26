package com.codewithmosh.store.services;

import com.codewithmosh.store.entities.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;


public class Jwt {
    private final SecretKey secretKey;
    private final Claims claims;

    public Jwt(SecretKey secretKey, Claims claims) {
        this.secretKey = secretKey;
        this.claims = claims;
    }


    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public Long getUserId(){
        return Long.valueOf(claims.getSubject());
    }

    public Role getRole(){
        return Role.valueOf(claims.get("role", String.class));
    }

    public String toString(){
       return Jwts.builder().claims(claims).signWith(secretKey).compact();
    }
}
