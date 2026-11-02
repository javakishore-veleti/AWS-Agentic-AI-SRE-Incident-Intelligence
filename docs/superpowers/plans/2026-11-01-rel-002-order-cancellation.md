# REL-002 plan

1. Replace order deletion with a cancellation that keeps the row and writes one audit record.
2. Release the inventory reservation for that order number, and make a second release a no-op.
3. Leave the order unchanged when inventory cannot be reached.
4. Change the desk order action from Remove to Cancel.
5. Cover a repeated cancellation, a customer denial, an unreachable release, and a stock release that restores quantity.
