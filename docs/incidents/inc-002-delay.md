# INC-002 inventory delay

Set `FAULT_DELAY_MS` above zero on Inventory Service and restart it. Each reservation sleeps for that many milliseconds. Orders waits on that call and gives up after 8 seconds.

Watch the orders trace. The inventory client span should last at least the delay. Set `FAULT_DELAY_MS` back to `0`, restart Inventory Service, and place another order. That inventory span should be shorter.

`InventoryDelayTest` and `PurchaseTraceTest` are the evidence for that wait and the following faster call. `CICD/Local/k6/inc-002-delay.js` is the load profile. It was not run. Jaeger was not opened.
