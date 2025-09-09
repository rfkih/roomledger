package com.roomledger.app.controller;


import com.roomledger.app.service.ParamService;
import com.roomledger.app.service.ClockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/params")
public class ParamController {
    private final ParamService params; private final ClockService clock;
    public ParamController(ParamService params, ClockService clock) { this.params = params; this.clock = clock; }

    @GetMapping("/clock")
    public ResponseEntity<?> clock() {
        return ResponseEntity.ok(Map.of(
                "mode", params.getString("APP_DATE_MODE", "REALTIME"),
                "currentDate", params.getDate("APP_DATE", LocalDate.now(clock.zone())).toString(),
                "zone", clock.zone().toString(),
                "todayResolved", clock.today().toString()
        ));
    }

    @PostMapping("/clock/mode")
    public ResponseEntity<?> setMode(@RequestParam String mode) {
        params.set("APP_DATE_MODE","STRING", mode); return clock();
    }
    @PostMapping("/clock/date")
    public ResponseEntity<?> setDate(@RequestParam String date) {
        params.set("APP_DATE","DATE", date); return clock();
    }
    @PostMapping("/clock/zone")
    public ResponseEntity<?> setZone(@RequestParam String id) {
        params.set("APP_ZONE","STRING", id); return clock();
    }
    @PostMapping("/clock/tick")
    public ResponseEntity<?> tick(@RequestParam(defaultValue="1") int days) {
        java.time.LocalDate d = params.getDate("APP_DATE", LocalDate.now(clock.zone())).plusDays(days);
        params.set("APP_DATE","DATE", d.toString()); return clock();
    }
}

