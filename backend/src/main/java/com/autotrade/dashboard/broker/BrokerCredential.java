package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "broker_credentials",
        uniqueConstraints = @UniqueConstraint(columnNames = {"broker", "environment"}))
public class BrokerCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "broker_credentials_seq")
    @SequenceGenerator(name = "broker_credentials_seq", sequenceName = "broker_credentials_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "broker", nullable = false, length = 20)
    private Broker broker;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 10)
    private TradingMode environment;

    @JsonIgnore
    @Column(name = "api_key_ciphertext", nullable = false, length = 4000)
    private String apiKeyCiphertext;

    @JsonIgnore
    @Column(name = "api_secret_ciphertext", nullable = false, length = 4000)
    private String apiSecretCiphertext;

    @Column(name = "encryption_key_version", nullable = false, length = 20)
    private String encryptionKeyVersion = "v1";

    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BrokerCredential() {
        // JPA
    }

    /**
     * Ciphertext must already be encrypted by {@link CredentialEncryptionService}
     * (via {@link BrokerCredentialService}) — this constructor does not encrypt.
     */
    public BrokerCredential(Broker broker, TradingMode environment,
                             String apiKeyCiphertext, String apiSecretCiphertext) {
        this.broker = broker;
        this.environment = environment;
        this.apiKeyCiphertext = apiKeyCiphertext;
        this.apiSecretCiphertext = apiSecretCiphertext;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public TradingMode getEnvironment() {
        return environment;
    }

    public void setEnvironment(TradingMode environment) {
        this.environment = environment;
    }

    public String getApiKeyCiphertext() {
        return apiKeyCiphertext;
    }

    public void setApiKeyCiphertext(String apiKeyCiphertext) {
        this.apiKeyCiphertext = apiKeyCiphertext;
    }

    public String getApiSecretCiphertext() {
        return apiSecretCiphertext;
    }

    public void setApiSecretCiphertext(String apiSecretCiphertext) {
        this.apiSecretCiphertext = apiSecretCiphertext;
    }

    public String getEncryptionKeyVersion() {
        return encryptionKeyVersion;
    }

    public void setEncryptionKeyVersion(String encryptionKeyVersion) {
        this.encryptionKeyVersion = encryptionKeyVersion;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BrokerCredential that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Deliberately excludes the ciphertext fields — never log broker credentials. */
    @Override
    public String toString() {
        return "BrokerCredential{id=" + id + ", broker=" + broker + ", environment=" + environment +
                ", encryptionKeyVersion='" + encryptionKeyVersion + "', isActive=" + isActive + "}";
    }
}
