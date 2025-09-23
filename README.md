RoomLedger — Booking & Billing for Kos/Kost Domain: room bookings (kos/kost), deposit + monthly rent billing, admin verification, occupancy & finance basics.

Features:

    Tenants, Buildings, Rooms
    Room inquiry with filters (building, status, price) and availability by date range
    Booking lifecycle: DRAFT → ACTIVE → ENDED/CANCELLED
    Deposit first, then rent bills (monthly), with daily proration for partial months
    Discount tiers for multi-month stays (configurable)
    Payments (DEPOSIT/RENT) with statuses: PENDING → PAID → VERIFIED / CANCELLED
    Self-service payment confirm (user) and verify (admin)
    Renewal flow (continue/stop next period)
    Background job: auto-cancel DRAFT bookings if deposit not paid within TTL
    Dockerfile + docker-compose to run app + Postgre

