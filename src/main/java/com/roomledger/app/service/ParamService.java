package com.roomledger.app.service;


import com.roomledger.app.model.AppParam;
import com.roomledger.app.repository.AppParamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;

@Service
public class ParamService {
    private final AppParamRepository repo;
    public ParamService(AppParamRepository repo) { this.repo = repo; }

    private AppParam getRaw(String key) {
        return repo.findById(key).orElseThrow(() -> new NoSuchElementException("param not found: " + key));
    }

    public String getString(String key, String def) {
        try { return getRaw(key).getValue(); } catch (NoSuchElementException e) { return def; }
    }
    public int getInt(String key, int def) {
        try { return Integer.parseInt(getRaw(key).getValue()); } catch (Exception e) { return def; }
    }
    public BigDecimal getDecimal(String key, BigDecimal def) {
        try { return new BigDecimal(getRaw(key).getValue()); } catch (Exception e) { return def; }
    }
    public boolean getBool(String key, boolean def) {
        try { return Boolean.parseBoolean(getRaw(key).getValue()); } catch (Exception e) { return def; }
    }
    public LocalDate getDate(String key, LocalDate def) {
        try { return LocalDate.parse(getRaw(key).getValue()); } catch (Exception e) { return def; }
    }
    public ZoneId getZone(String key, String defZoneId) {
        String id = getString(key, defZoneId);
        return ZoneId.of(id);
    }

    @Transactional
    public void set(String key, String type, String value) {
        AppParam p = repo.findById(key).orElseGet(AppParam::new);
        p.setKey(key); p.setType(type); p.setValue(value);
        p.setUpdatedAt(java.time.OffsetDateTime.now());
        repo.save(p);
    }
}

