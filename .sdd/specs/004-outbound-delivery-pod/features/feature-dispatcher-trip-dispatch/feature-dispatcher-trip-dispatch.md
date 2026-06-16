# Feature: Dispatcher Lap Chuyen xe & Van chuyen Noi bo (US-WMS-08)

## 1. Context and Goal

Dispatcher gom cac Delivery Order da duoc Truong kho phe duyet xuat kho, gan xe va tai xe noi bo de lap chuyen xe. Khi tai xe xac nhan da nhan hang va xe roi kho, he thong chuyen hang tu outbound staging sang kho ao In-Transit, giai phong reserved quantity va tao delivery attempt cho tung Delivery Order.

## 2. Actors

* **Dispatcher**: Lap chuyen xe, gan xe va tai xe, sap xep stop order.
* **Tai xe**: Xac nhan nhan hang va xe roi kho.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL only allow trip planning for Delivery Orders in `WAREHOUSE_APPROVED` status.
  * The system SHALL release reserved inventory when a Delivery Order transitions to `IN_TRANSIT`.
  * The system SHALL create `TRIP_CREATE`, `TRIP_DEPART`, and `DELIVERY_ATTEMPT_CREATE` audit log entries for trip assignment, departure, and delivery attempt creation.
* **Event-driven:**
  * WHEN a Dispatcher creates a trip, the system SHALL:
    * Validate that all selected Delivery Orders are in `WAREHOUSE_APPROVED` status.
    * Validate total weight and volume of Delivery Order items against vehicle maximum weight/volume capacity.
    * Store the selected vehicle, driver, dispatcher, planned date, and stop order.
  * WHEN a driver confirms departure, the system SHALL:
    * Set `issued_qty = requested_qty` for Sprint 1 full-shipment Delivery Orders.
    * Decrease outbound staging inventory by `issued_qty`.
    * Decrease the related reserved quantity by `issued_qty`.
    * Increase virtual In-Transit inventory by `issued_qty`.
    * Create one new `deliveries` record per dispatched Delivery Order, using the next `attempt_number`.
    * Update Delivery Order status to `IN_TRANSIT` and Trip status to `IN_TRANSIT`.
  * WHEN all Delivery Orders assigned to a Trip reach terminal delivery outcomes, the system SHALL:
    * Treat `COMPLETED` and `RETURNED` as terminal outcomes for Sprint 1.
    * Update Trip status to `COMPLETED`.

## 4. API Endpoints

* `POST /api/v1/trips` - Tao chuyen xe moi tu cac Delivery Order da `WAREHOUSE_APPROVED`.
* `PUT /api/v1/trips/{id}/depart` - Tai xe xac nhan nhan hang va xe roi kho.

## 5. Acceptance Criteria

* **Scenario: Create trip from approved orders**
  * Given multiple Delivery Orders are in `WAREHOUSE_APPROVED`
  * When Dispatcher assigns a valid vehicle, driver, planned date, and stop order
  * Then the system SHALL create the trip and keep the Delivery Orders ready for departure.

* **Scenario: Move staged goods to In-Transit at departure**
  * Given a trip loaded with 50 units in outbound staging
  * When the driver confirms goods received and vehicle departure
  * Then the system SHALL create delivery attempts, move Delivery Orders to `IN_TRANSIT`, decrease outbound staging by 50, release reserved quantity by 50, and increase virtual In-Transit inventory by 50.

* **Scenario: Complete trip after all assigned orders finish**
  * Given a trip has multiple assigned Delivery Orders
  * When every assigned Delivery Order is either `COMPLETED` or `RETURNED`
  * Then the system SHALL mark the trip as `COMPLETED`.
