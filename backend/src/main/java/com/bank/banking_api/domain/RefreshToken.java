package com.bank.banking_api.domain;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken{
    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final String deviceInfo;
    private final Instant createdAt;
    private final Instant expiredAt;
    private final boolean revoked;
    private final Instant revokedAt;

    private RefreshToken(Builder builder){
        this.id = builder.id;
        this.userId = builder.userId;
        this.tokenHash = builder.tokenHash;
        this.deviceInfo = builder.deviceInfo;
        this.createdAt = builder.createdAt;
        this.expiredAt = builder.expiredAt;
        this.revoked = builder.revoked;
        this.revokedAt=builder.revokedAt;
    }

    public UUID getId(){return id;}
    public UUID getUserId(){return userId;}
    public String getTokenHash(){return tokenHash;}
    public String getDeviceInfo(){return deviceInfo;}
    public Instant getCreatedAt(){return createdAt;}
    public Instant getExpiredAt(){return expiredAt;}
    public boolean isRevoked(){return revoked;}
    public Instant getRevokedAt(){return revokedAt;}



    public RefreshToken revoke(Instant revokedAt){
        return new Builder(this)
                .revoked(true)
                .revokedAt(revokedAt)
                .build();
    }


    public static  Builder builder() {return new Builder();}

    public static class Builder{
        private UUID id;
        private UUID userId;
        private String tokenHash;
        private String deviceInfo;
        private Instant createdAt;
        private Instant expiredAt;
        private boolean revoked = false;
        private Instant revokedAt;


        public Builder id(UUID id){ this.id = id; return this;}
        public Builder userId(UUID userId){ this.userId = userId; return this;}
        public Builder tokenHash(String tokenHash){ this.tokenHash = tokenHash; return this;}
        public Builder deviceInfo(String deviceInfo){ this.deviceInfo = deviceInfo; return this;}
        public Builder createdAt(Instant createdAt){ this.createdAt = createdAt; return this;}
        public Builder expiredAt(Instant expiredAt){ this.expiredAt = expiredAt; return this;}
        public Builder revoked(boolean revoked){ this.revoked = revoked; return this;}
        public Builder revokedAt(Instant revokedAt){ this.revokedAt = revokedAt; return this;}

        public Builder(RefreshToken existing){
            this.id=existing.id;
            this.userId=existing.userId;
            this.tokenHash=existing.tokenHash;
            this.deviceInfo=existing.deviceInfo;
            this.createdAt=existing.createdAt;
            this.expiredAt=existing.expiredAt;
            this.revoked=existing.revoked;
            this.revokedAt=existing.revokedAt;
        }

        public Builder(){}

        public RefreshToken build(){
            if(id==null || userId==null){
                throw new  IllegalStateException("Cannot build RefreshToken with null values");
            }

            return new RefreshToken(this);
        }
    }
}